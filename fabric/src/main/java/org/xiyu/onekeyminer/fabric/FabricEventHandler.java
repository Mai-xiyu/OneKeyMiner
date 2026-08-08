package org.xiyu.onekeyminer.fabric;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.chain.ChainActionContext;
import org.xiyu.onekeyminer.chain.ChainActionLogic;
import org.xiyu.onekeyminer.chain.ChainActionResult;
import org.xiyu.onekeyminer.chain.ChainActionType;
import org.xiyu.onekeyminer.chain.OriginalToolGuard;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.MinerConfig;
import org.xiyu.onekeyminer.mining.MiningStateManager;
import org.xiyu.onekeyminer.platform.PlatformServices;

/**
 * Fabric 事件处理器
 * 
 * <p>注册并处理与链式操作相关的 Fabric 事件：</p>
 * <ul>
 *   <li>{@link PlayerBlockBreakEvents#AFTER} - 方块破坏后事件（连锁挖掘）</li>
 *   <li>{@link UseBlockCallback} - 右键方块事件（连锁交互/种植）</li>
 *   <li>{@link ServerPlayConnectionEvents#DISCONNECT} - 玩家断开连接（清理状态）</li>
 * </ul>
 * 
 * @author OneKeyMiner Team
 * @version 1.6.8
 * @since Minecraft 1.20.1
 */
public class FabricEventHandler {
    
    /** 防止重入的标记 */
    private static final ThreadLocal<Boolean> IS_CHAIN_BREAKING = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> IS_CHAIN_INTERACTING = ThreadLocal.withInitial(() -> false);
    private static final FabricBreakToolSnapshots BREAK_TOOL_SNAPSHOTS =
            new FabricBreakToolSnapshots();
    
    /**
     * 注册所有事件监听器
     */
    public static void register() {
        // 注册方块破坏事件（连锁挖掘）
        PlayerBlockBreakEvents.BEFORE.register(FabricEventHandler::beforeBlockBreak);
        PlayerBlockBreakEvents.CANCELED.register(FabricEventHandler::onBlockBreakCanceled);
        PlayerBlockBreakEvents.AFTER.register(FabricEventHandler::onBlockBreak);
        
        // 注册右键方块事件（连锁交互/种植）
        UseBlockCallback.EVENT.register(FabricEventHandler::onUseBlock);

        // 注册玩家断开连接事件（清理状态）
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            var playerId = handler.getPlayer().getUUID();
            BREAK_TOOL_SNAPSHOTS.clearPlayer(playerId);
            FabricPlatformServices.cleanupPlayer(playerId);
        });
        
        // 注册服务器停止事件（清理所有状态）
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            BREAK_TOOL_SNAPSHOTS.clearAll();
            MiningStateManager.clearAll();
        });
        
        OneKeyMiner.LOGGER.info("Fabric 事件处理器已注册");
    }
    
    /**
     * 处理方块破坏后事件 - 触发连锁挖掘
     * 
     * @param level 世界
     * @param player 玩家
     * @param pos 方块位置
     * @param state 方块状态（破坏前）
     * @param blockEntity 方块实体（如果有）
     */
    private static boolean beforeBlockBreak(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity
    ) {
        if (IS_CHAIN_BREAKING.get() || IS_CHAIN_INTERACTING.get()) {
            return true;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }
        MinerConfig config = ConfigManager.getConfig();
        if (!config.enabled
                || !PlatformServices.getInstance().isChainModeActive(serverPlayer)) {
            return true;
        }

        BREAK_TOOL_SNAPSHOTS.capture(
                serverPlayer.getUUID(),
                level,
                pos,
                serverPlayer.getInventory().selected,
                serverPlayer.getMainHandItem()
        );
        return true;
    }

    private static void onBlockBreakCanceled(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity
    ) {
        if (player instanceof ServerPlayer serverPlayer) {
            BREAK_TOOL_SNAPSHOTS.discard(serverPlayer.getUUID(), level, pos);
        }
    }

    private static void onBlockBreak(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity
    ) {
        // 防止重入（链式挖掘时不触发新的链式操作）
        if (IS_CHAIN_BREAKING.get() || ChainActionLogic.isProcessing()) {
            return;
        }
        
        // 只处理服务端事件
        if (level.isClientSide()) {
            return;
        }
        
        // 检查玩家
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        var toolSnapshot = BREAK_TOOL_SNAPSHOTS.consume(
                serverPlayer.getUUID(),
                level,
                pos
        );
        if (toolSnapshot == null
                || serverPlayer.getInventory().selected
                        != toolSnapshot.selectedSlot()
                || !OriginalToolGuard.matchesAfterBreak(
                        toolSnapshot.tool(),
                        serverPlayer.getMainHandItem()
                )) {
            return;
        }
        
        // 检查配置
        MinerConfig config = ConfigManager.getConfig();
        if (!config.enabled) {
            return;
        }
        
        // 检查链式模式是否激活
        if (!PlatformServices.getInstance().isChainModeActive(serverPlayer)) {
            return;
        }
        
        try {
            IS_CHAIN_BREAKING.set(true);
            
            // 执行连锁挖掘
            ChainActionResult result = ChainActionLogic.onVerifiedBlockBreak(
                    serverPlayer,
                    level,
                    pos,
                    state,
                    toolSnapshot.tool()
            );
            
            if (result.isSuccess() && result.totalCount() > 0) {
                // 发送操作完成消息
                if (config.showStats) {
                    PlatformServices.getInstance().sendChainActionMessage(
                            serverPlayer,
                            "mining",
                            result.totalCount()
                    );
                }

                if (config.playSound) {
                    level.playSound(null, serverPlayer.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                            SoundSource.PLAYERS, 0.6f, 1.0f);
                }
                
            }
            
        } finally {
            IS_CHAIN_BREAKING.set(false);
        }
    }
    
    /**
     * 处理右键方块事件 - 触发连锁交互或种植
     * 
     * @param player 玩家
     * @param level 世界
     * @param hand 交互手
     * @param hitResult 点击结果
     * @return 交互结果
     */
    private static InteractionResult onUseBlock(
            Player player,
            Level level,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        // Other block interactions are observed after their authoritative
        // return value by the common ServerPlayerGameMode mixin. This callback
        // only owns harvesting, which has no consuming vanilla empty-hand use.
        if (IS_CHAIN_INTERACTING.get()) {
            return InteractionResult.PASS;
        }
        
        // 只处理服务端事件
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }
        
        // 检查玩家
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        
        // 检查配置
        MinerConfig config = ConfigManager.getConfig();
        if (!config.enabled || !config.enableHarvesting) {
            return InteractionResult.PASS;
        }
        
        // 检查链式模式是否激活
        if (!PlatformServices.getInstance().isChainModeActive(serverPlayer)) {
            return InteractionResult.PASS;
        }
        
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!serverPlayer.getItemInHand(hand).isEmpty()
                || !ChainActionLogic.isMatureCrop(state)) {
            return InteractionResult.PASS;
        }
        
        try {
            IS_CHAIN_INTERACTING.set(true);

            ChainActionContext context = ChainActionContext.builder()
                    .player(serverPlayer)
                    .level(level)
                    .originPos(pos)
                    .originState(state)
                    .actionType(ChainActionType.HARVESTING)
                    .heldItem(serverPlayer.getItemInHand(hand))
                    .hand(hand)
                    .activationVerified(true)
                    .build();
            
            // 执行链式操作
            ChainActionResult result = ChainActionLogic.execute(context);
            
            if (result.isSuccess() && result.totalCount() > 0) {
                // 发送操作完成消息
                if (config.showStats) {
                    PlatformServices.getInstance().sendChainActionMessage(
                        serverPlayer,
                        ChainActionType.HARVESTING.getId(),
                        result.totalCount()
                    );
                }

                if (config.playSound) {
                    level.playSound(null, serverPlayer.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS, 0.6f, 1.0f);
                }
                
                OneKeyMiner.LOGGER.debug("{} 完成: {}",
                        ChainActionType.HARVESTING.getDisplayName(),
                        result.getSummary());
                
                // 返回 SUCCESS 以消费事件
                return InteractionResult.SUCCESS;
            }
            
        } finally {
            IS_CHAIN_INTERACTING.set(false);
        }
        
        return InteractionResult.PASS;
    }

}
