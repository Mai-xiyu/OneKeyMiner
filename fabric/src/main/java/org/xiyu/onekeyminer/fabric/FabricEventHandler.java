package org.xiyu.onekeyminer.fabric;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.MinerConfig;
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
 * @version 2.0.0
 * @since Minecraft 1.21.9
 */
public class FabricEventHandler {
    
    /** 防止重入的标记 */
    private static final ThreadLocal<Boolean> IS_CHAIN_BREAKING = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> IS_CHAIN_INTERACTING = ThreadLocal.withInitial(() -> false);
    
    /**
     * 注册所有事件监听器
     */
    public static void register() {
        // 注册方块破坏事件（连锁挖掘）
        PlayerBlockBreakEvents.AFTER.register(FabricEventHandler::onBlockBreak);
        
        // 注册右键方块事件（连锁交互/种植）
        UseBlockCallback.EVENT.register(FabricEventHandler::onUseBlock);
        
        // 注册玩家断开连接事件（清理状态）
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            FabricPlatformServices.cleanupPlayer(handler.getPlayer().getUUID());
        });
        
        // 注册服务器停止事件（清理所有状态）
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            // 清理所有玩家状态
            server.getPlayerList().getPlayers().forEach(player -> 
                FabricPlatformServices.cleanupPlayer(player.getUUID())
            );
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
    private static void onBlockBreak(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity
    ) {
        // 防止重入（链式挖掘时不触发新的链式操作）
        if (IS_CHAIN_BREAKING.get()) {
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
            ChainActionResult result = ChainActionLogic.onBlockBreak(serverPlayer, level, pos, state);
            
            if (result.isSuccess()) {
                // 发送操作完成消息
                PlatformServices.getInstance().sendChainActionMessage(
                        serverPlayer,
                        "mining",
                        result.totalCount()
                );
                
                OneKeyMiner.LOGGER.debug("连锁挖掘完成: {}", result.getSummary());
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
        // 防止重入
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
        if (!config.enabled) {
            return InteractionResult.PASS;
        }
        
        // 检查链式模式是否激活
        if (!PlatformServices.getInstance().isChainModeActive(serverPlayer)) {
            return InteractionResult.PASS;
        }
        
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);
        
        // 确定操作类型（交互还是种植）
        ChainActionType actionType = determineActionType(serverPlayer, hand, state);
        if (actionType == null) {
            return InteractionResult.PASS;
        }
        
        // 检查对应功能是否启用
        if (actionType == ChainActionType.INTERACTION && !config.enableInteraction) {
            return InteractionResult.PASS;
        }
        if (actionType == ChainActionType.PLANTING && !config.enablePlanting) {
            return InteractionResult.PASS;
        }
        
        try {
            IS_CHAIN_INTERACTING.set(true);
            
            // 构建上下文
            ChainActionContext context = ChainActionContext.builder()
                    .player(serverPlayer)
                    .level(level)
                    .originPos(pos)
                    .originState(state)
                    .actionType(actionType)
                    .heldItem(serverPlayer.getItemInHand(hand))
                    .hand(hand)
                    .build();
            
            // 执行链式操作
            ChainActionResult result = ChainActionLogic.execute(context);
            
            if (result.isSuccess()) {
                // 发送操作完成消息
                PlatformServices.getInstance().sendChainActionMessage(
                        serverPlayer,
                        actionType.getId(),
                        result.totalCount()
                );
                
                OneKeyMiner.LOGGER.debug("{} 完成: {}", 
                        actionType.getDisplayName(), 
                        result.getSummary());
                
                // 返回 SUCCESS 以消费事件
                return InteractionResult.SUCCESS;
            }
            
        } finally {
            IS_CHAIN_INTERACTING.set(false);
        }
        
        return InteractionResult.PASS;
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
        
        var item = heldItem.getItem();
        
        // 检查是否是种植操作 - 使用统一的可种植物品检查
        // 只有符合条件的物品才会触发连锁种植，否则正常放行放置事件
        if (ChainActionLogic.isPlantableItem(heldItem)) {
            return ChainActionType.PLANTING;
        }
        
        // 检查是否是交互操作
        // 工具类物品（锄头、斧头、铲子、剪刀等）
        if (item instanceof net.minecraft.world.item.HoeItem ||
            item instanceof net.minecraft.world.item.AxeItem ||
            item instanceof net.minecraft.world.item.ShovelItem ||
            item instanceof net.minecraft.world.item.ShearsItem ||
            item instanceof net.minecraft.world.item.BrushItem) {
            return ChainActionType.INTERACTION;
        }
        
        return null;
    }
}
