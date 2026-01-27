package org.xiyu.onekeyminer.neoforge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.api.OneKeyMinerAPI;
import org.xiyu.onekeyminer.chain.ChainActionContext;
import org.xiyu.onekeyminer.chain.ChainActionLogic;
import org.xiyu.onekeyminer.chain.ChainActionResult;
import org.xiyu.onekeyminer.chain.ChainActionType;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.MinerConfig;
import org.xiyu.onekeyminer.platform.PlatformServices;

/**
 * NeoForge 事件处理器
 * 
 * <p>监听并处理与链式操作相关的游戏事件：</p>
 * <ul>
 *   <li>{@link BlockEvent.BreakEvent} - 方块破坏事件（连锁挖掘）</li>
 *   <li>{@link PlayerInteractEvent.RightClickBlock} - 右键方块事件（连锁交互/种植）</li>
 * </ul>
 * 
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.21.9
 */
@EventBusSubscriber(modid = OneKeyMiner.MOD_ID)
public class NeoForgeEventHandler {
    
    /** 防止重入的标记 */
    private static final ThreadLocal<Boolean> IS_CHAIN_BREAKING = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> IS_CHAIN_INTERACTING = ThreadLocal.withInitial(() -> false);
    
    /**
     * 处理方块破坏事件 - 触发连锁挖掘
     * 
     * @param event 方块破坏事件
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // 防止重入（链式挖掘时不触发新的链式操作）
        if (IS_CHAIN_BREAKING.get()) {
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
        
        try {
            IS_CHAIN_BREAKING.set(true);
            
            // 执行连锁挖掘
            ChainActionResult result = ChainActionLogic.onBlockBreak(player, level, pos, state);
            
            if (result.isSuccess()) {
                // 发送操作完成消息
                if (config.showStats) {
                    PlatformServices.getInstance().sendChainActionMessage(
                            player,
                            "mining",
                            result.totalCount()
                    );
                }

                if (config.playSound) {
                    level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                            SoundSource.PLAYERS, 0.6f, 1.0f);
                }
                
                OneKeyMiner.LOGGER.debug("连锁挖掘完成: {}", result.getSummary());
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
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
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
        if (!config.enabled) {
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
        
        var heldItem = player.getItemInHand(hand);
        OneKeyMinerAPI.ToolActionRule customRule = OneKeyMinerAPI.findToolActionForBlock(heldItem, state).orElse(null);
        ChainActionContext.InteractionOverride interactionOverride = null;

        // 确定操作类型（交互、种植或自定义）
        ChainActionType actionType;
        if (customRule != null) {
            actionType = customRule.actionType();
            if (actionType == ChainActionType.INTERACTION) {
                interactionOverride = ChainActionLogic.mapInteractionOverride(customRule.interactionRule());
                if (interactionOverride == null) {
                    return;
                }
            }
        } else {
            actionType = determineActionType(player, hand, state);
        }
        if (actionType == null) {
            return;
        }
        
        // 检查对应功能是否启用
        if (actionType == ChainActionType.INTERACTION && !config.enableInteraction) {
            return;
        }
        if (actionType == ChainActionType.PLANTING && !config.enablePlanting) {
            return;
        }
        
        try {
            IS_CHAIN_INTERACTING.set(true);
            
            // 构建上下文
                ChainActionContext context = ChainActionContext.builder()
                    .player(player)
                    .level(level)
                    .originPos(pos)
                    .originState(state)
                    .actionType(actionType)
                    .heldItem(heldItem)
                    .hand(hand)
                    .interactionOverride(interactionOverride)
                    .build();
            
            // 执行链式操作
            ChainActionResult result = ChainActionLogic.execute(context);
            
            if (result.isSuccess()) {
                // 发送操作完成消息
                if (config.showStats) {
                    PlatformServices.getInstance().sendChainActionMessage(
                        player,
                        actionType.getId(),
                        result.totalCount()
                    );
                }

                if (config.playSound) {
                    level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS, 0.6f, 1.0f);
                }
                
                OneKeyMiner.LOGGER.debug("{} 完成: {}", 
                        actionType.getDisplayName(), 
                        result.getSummary());
            }
            
        } finally {
            IS_CHAIN_INTERACTING.set(false);
        }
    }

    /**
     * 处理右键实体事件 - 触发剪羊毛连锁
     *
     * @param event 右键实体事件
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        if (IS_CHAIN_INTERACTING.get()) {
            return;
        }

        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MinerConfig config = ConfigManager.getConfig();
        if (!config.enabled || !config.enableInteraction) {
            return;
        }

        if (!PlatformServices.getInstance().isChainModeActive(player)) {
            return;
        }

        Entity target = event.getTarget();
        InteractionHand hand = event.getHand();
        var heldItem = player.getItemInHand(hand);
        if (heldItem.isEmpty()) {
            return;
        }

        OneKeyMinerAPI.ToolActionRule customRule = OneKeyMinerAPI.findToolActionForEntity(heldItem, target).orElse(null);
        ChainActionContext.InteractionOverride interactionOverride = null;

        if (customRule != null) {
            if (customRule.actionType() != ChainActionType.INTERACTION) {
                return;
            }
            interactionOverride = ChainActionLogic.mapInteractionOverride(customRule.interactionRule());
            if (interactionOverride == null) {
                return;
            }
            if (interactionOverride != ChainActionContext.InteractionOverride.SHEARING) {
                return;
            }
        } else {
            if (!(target instanceof Shearable shearable) || !shearable.readyForShearing()) {
                return;
            }
            interactionOverride = ChainActionContext.InteractionOverride.SHEARING;
        }

        try {
            IS_CHAIN_INTERACTING.set(true);

            ChainActionContext context = ChainActionContext.builder()
                    .player(player)
                    .level(event.getLevel())
                    .originPos(target.blockPosition())
                    .originState(event.getLevel().getBlockState(target.blockPosition()))
                    .actionType(ChainActionType.INTERACTION)
                    .heldItem(heldItem)
                    .hand(hand)
                    .interactionOverride(interactionOverride)
                    .build();

            ChainActionResult result = ChainActionLogic.execute(context);

            if (result.isSuccess()) {
                if (config.showStats) {
                    PlatformServices.getInstance().sendChainActionMessage(
                            player,
                            ChainActionType.INTERACTION.getId(),
                            result.totalCount()
                    );
                }

                if (config.playSound) {
                    event.getLevel().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                            SoundSource.PLAYERS, 0.6f, 1.0f);
                }

                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        } finally {
            IS_CHAIN_INTERACTING.set(false);
        }
    }
    
    /**
     * 根据手持物品和目标方块确定操作类型
     * 
     * @param player 玩家
     * @param hand 交互手
     * @param targetState 目标方块状态
     * @return 操作类型，如果无法确定返回 null
     */
    private static ChainActionType determineActionType(
            ServerPlayer player,
            InteractionHand hand,
            BlockState targetState
    ) {
        var heldItem = player.getItemInHand(hand);
        
        if (heldItem.isEmpty()) {
            return null;
        }
        
        // 检查是否是种植操作 - 使用统一的可种植物品检查
        // 只有符合条件的物品才会触发连锁种植，否则正常放行放置事件
        if (ChainActionLogic.isPlantableItem(heldItem)) {
            return ChainActionType.PLANTING;
        }
        
        // 检查是否是交互操作
        if (ChainActionLogic.isValidInteractionTarget(heldItem, targetState)) {
            return ChainActionType.INTERACTION;
        }
        
        return null;
    }
}
