package org.xiyu.onekeyminer.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.chain.ChainActionContext;
import org.xiyu.onekeyminer.chain.ChainActionLogic;
import org.xiyu.onekeyminer.chain.ChainActionResult;
import org.xiyu.onekeyminer.chain.ChainActionType;
import org.xiyu.onekeyminer.chain.OriginalToolGuard;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.MinerConfig;
import org.xiyu.onekeyminer.platform.PlatformServices;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Forge 事件处理器
 * 
 * <p>监听并处理与链式操作相关的游戏事件：</p>
 * <ul>
 *   <li>{@link BlockEvent.BreakEvent} - 方块破坏事件（连锁挖掘）</li>
 *   <li>{@link PlayerInteractEvent.RightClickBlock} - 右键方块事件（连锁交互/种植）</li>
 *   <li>{@link PlayerEvent.PlayerLoggedOutEvent} - 玩家退出事件（清理状态）</li>
 * </ul>
 * 
 * @author OneKeyMiner Team
 * @version 1.6.8
 * @since Minecraft 1.20.4
 */
public class ForgeEventHandler {
    
    /** 防止重入的标记 */
    private static final ThreadLocal<Boolean> IS_CHAIN_BREAKING = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> IS_CHAIN_INTERACTING = ThreadLocal.withInitial(() -> false);
    private static final Queue<PendingBreak> PENDING_BREAKS = new ArrayDeque<>();
    private static final int MAX_PENDING_BREAKS = 4_096;
    
    /**
     * 处理方块破坏事件 - 触发连锁挖掘
     * 
     * @param event 方块破坏事件
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        // 防止重入（链式挖掘时不触发新的链式操作）
        if (IS_CHAIN_BREAKING.get() || ChainActionLogic.isProcessing()) {
            return;
        }
        
        // 只处理服务端事件
        if (event.getLevel().isClientSide()) {
            return;
        }
        
        // 检查玩家
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        
        // 检查配置
        MinerConfig config = ConfigManager.getConfig();
        if (!config.enabled) {
            return;
        }
        
        // 检查链式模式是否激活
        if (!PlatformServices.getInstance().isChainModeActive(player)) {
            return;
        }
        
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        Level level = (Level) event.getLevel();
        
        // BreakEvent runs before vanilla removes the block. MinecraftServer#execute
        // may run inline on the server thread, so a tick-end queue is required.
        if (!event.isCanceled() && PENDING_BREAKS.size() < MAX_PENDING_BREAKS) {
            PENDING_BREAKS.add(new PendingBreak(
                    player,
                    level,
                    pos.immutable(),
                    state,
                    player.getInventory().selected,
                    player.getMainHandItem().copy()
            ));
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_BREAKS.isEmpty()) {
            return;
        }
        PendingBreak pending;
        while ((pending = PENDING_BREAKS.poll()) != null) {
            processVerifiedBreak(pending);
        }
    }

    private static void processVerifiedBreak(PendingBreak pending) {
        ServerPlayer player = pending.player();
        Level level = pending.level();
        BlockPos pos = pending.pos();
        if (player.isRemoved()
                || player.level() != level
                || !level.hasChunkAt(pos)
                || player.getInventory().selected != pending.selectedSlot()
                || !OriginalToolGuard.matchesAfterBreak(
                        pending.originalTool(),
                        player.getMainHandItem()
                )
                || level.getBlockState(pos).getBlock() == pending.state().getBlock()) {
            return;
        }
        try {
            IS_CHAIN_BREAKING.set(true);
            ChainActionResult result = ChainActionLogic.onVerifiedBlockBreak(
                    player,
                    level,
                    pos,
                    pending.state(),
                    pending.originalTool()
            );
            if (!result.isSuccess() || result.totalCount() <= 0) {
                return;
            }
            MinerConfig config = ConfigManager.getConfig();
            if (config.showStats) {
                PlatformServices.getInstance().sendChainActionMessage(
                        player,
                        ChainActionType.MINING.getId(),
                        result.totalCount()
                );
            }
            if (config.playSound) {
                level.playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS,
                        0.6f,
                        1.0f
                );
            }
        } finally {
            IS_CHAIN_BREAKING.set(false);
        }
    }
    
    /**
     * 处理右键方块事件 - 触发连锁交互或种植
     * 
     * @param event 右键方块事件
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // 防止重入
        if (IS_CHAIN_INTERACTING.get()) {
            return;
        }
        
        // 只处理服务端事件
        if (event.getLevel().isClientSide()) {
            return;
        }
        
        // 检查玩家
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        // 检查配置
        MinerConfig config = ConfigManager.getConfig();
        if (!config.enabled || !config.enableHarvesting) {
            return;
        }
        
        // 检查链式模式是否激活
        if (!PlatformServices.getInstance().isChainModeActive(player)) {
            return;
        }
        
        BlockPos pos = event.getPos();
        Level level = event.getLevel();
        InteractionHand hand = event.getHand();
        BlockState state = level.getBlockState(pos);
        
        if (!player.getItemInHand(hand).isEmpty()
                || !ChainActionLogic.isMatureCrop(state)) {
            return;
        }
        
        try {
            IS_CHAIN_INTERACTING.set(true);

            ChainActionContext context = ChainActionContext.builder()
                    .player(player)
                    .level(level)
                    .originPos(pos)
                    .originState(state)
                    .actionType(ChainActionType.HARVESTING)
                    .heldItem(player.getItemInHand(hand))
                    .hand(hand)
                    .activationVerified(true)
                    .build();
            
            // 执行链式操作
            ChainActionResult result = ChainActionLogic.execute(context);
            
            if (result.isSuccess() && result.totalCount() > 0) {
                // 发送操作完成消息
                if (config.showStats) {
                    PlatformServices.getInstance().sendChainActionMessage(
                        player,
                        ChainActionType.HARVESTING.getId(),
                        result.totalCount()
                    );
                }

                if (config.playSound) {
                    level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS, 0.6f, 1.0f);
                }
                
                OneKeyMiner.LOGGER.debug("{} 完成: {}",
                        ChainActionType.HARVESTING.getDisplayName(),
                        result.getSummary());
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            
        } finally {
            IS_CHAIN_INTERACTING.set(false);
        }
    }

    /**
     * 处理玩家退出事件 - 清理状态
     * 
     * @param event 玩家退出事件
     */
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ForgePlatformServices.cleanupPlayer(player.getUUID());
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        PENDING_BREAKS.clear();
        org.xiyu.onekeyminer.mining.MiningStateManager.clearAll();
    }

    private record PendingBreak(
            ServerPlayer player,
            Level level,
            BlockPos pos,
            BlockState state,
            int selectedSlot,
            ItemStack originalTool
    ) {
    }
}
