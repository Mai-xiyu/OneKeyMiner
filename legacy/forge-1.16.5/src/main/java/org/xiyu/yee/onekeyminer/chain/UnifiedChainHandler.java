package org.xiyu.yee.onekeyminer.chain;

import net.minecraft.util.math.BlockPos;
import net.minecraft.server.MinecraftServer;

import net.minecraft.world.server.ServerWorld;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.block.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xiyu.yee.onekeyminer.Onekeyminer;
import org.xiyu.yee.onekeyminer.capability.ChainModeCapability;
import org.xiyu.yee.onekeyminer.config.ClientConfig;
import org.xiyu.yee.onekeyminer.config.CommonConfig;
import org.xiyu.yee.onekeyminer.api.ChainAPI;
import org.xiyu.yee.onekeyminer.api.ChainPositionStore;

/**
 * 统一连锁处理器 - 整合连锁挖掘、交互和种植功能
 */
@Mod.EventBusSubscriber(modid = Onekeyminer.MODID)
public class UnifiedChainHandler {

    /**
     * 处理玩家右键点击方块事件
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // 忽略非玩家实体
        if (!(event.getEntity() instanceof ServerPlayerEntity)) {
            return;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();

        try {
            // 检查连锁模式是否激活
            if (!ChainModeCapability.isChainModeActive(player)) {
                return;
            }
            
            // 检查是否需要潜行
            if (CommonConfig.INSTANCE.requireSneaking.get() && !player.isShiftKeyDown()) {
                return;
            }
            
            // 检查创造模式设置
            if (player.isCreative() && !CommonConfig.INSTANCE.enableInCreative.get()) {
                return;
            }
            if(!isToolItem(player.getItemInHand(event.getHand()))) {
                return;
            }
            // 获取玩家手中的物品
            ItemStack heldItem = player.getItemInHand(event.getHand());
            BlockState targetState = event.getWorld().getBlockState(event.getPos());

            // 在触发实际连锁处理前，清空旧的位置数据，实际处理方法会在执行时填入准确的位置
            try {
                java.util.UUID uuid = player.getUUID();
                String key = uuid.toString();

                // 清空相关位置存储，让实际处理方法填入准确的处理位置
                if (InteractionHandler.isValidInteractionTool(heldItem)) {
                    ChainPositionStore.setInteractionPositions(key, new java.util.ArrayList<>());
                } else if (isPlantableItem(heldItem)) {
                    ChainPositionStore.setPlantingPositions(key, new java.util.ArrayList<>());
                } else {
                    ChainPositionStore.setMiningPositions(key, new java.util.ArrayList<>());
                }
            } catch (Exception e) {
                Onekeyminer.LOGGER.debug("清理连锁位置存储时出错: {}", e.getMessage());
            }

            // 判断物品类型并调用相应处理方法
            if (InteractionHandler.isValidInteractionTool(heldItem)) {
                // 工具类 - 使用连锁交互
                if(ClientConfig.INSTANCE.Debug.get()) {
                    Onekeyminer.LOGGER.debug("触发工具连锁交互: {}", heldItem.getItem().getClass().getSimpleName());
                }
                // 使用新的交互方法
                InteractionHandler.tryChainInteraction(
                    player, 
                    (ServerWorld)event.getWorld(),
                    event.getPos(), 
                    event.getFace(), 
                    // 从BlockHitResult获取点击位置的Vec3
                    event.getHitVec().getLocation(), 
                    event.getHand()
                );
            } else if (isPlantableItem(heldItem)) {
                // 种植物类 - 使用连锁种植
                PlantingHandler.handleChainPlanting(player, event);
            }
        } catch (Exception e) {
            Onekeyminer.LOGGER.error("处理玩家交互事件时发生错误: {}", e.getMessage());
            Onekeyminer.LOGGER.debug("详细错误栈", e);
        }
    }

    /**
     * 处理玩家右键点击实体事件
     * 
     * 当玩家使用剪刀右键点击羊时，会检查以下配置：
     * - 连锁模式是否激活 (通过ChainModeCapability)
     * - 是否需要潜行 (requireSneaking)
     * - 创造模式下是否启用 (enableInCreative)
     * - 使用maxBlocksInChain或maxBlocksInChainCreative作为最大羊的数量
     * - 使用maxChainDepth*0.5作为基础搜索范围
     * - 启用对角连锁时 (enableDiagonalChaining) 搜索范围增加50%
     * - 如果启用teleportDropsToPlayer，羊毛会掉落在玩家位置
     */
    @SubscribeEvent
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        // 忽略非玩家实体
        if (!(event.getEntity() instanceof ServerPlayerEntity)) {
            return;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();

        try {
            // 检查连锁模式是否激活
            if (!ChainModeCapability.isChainModeActive(player)) {
                return;
            }
            
            // 检查是否需要潜行
            if (CommonConfig.INSTANCE.requireSneaking.get() && !player.isShiftKeyDown()) {
                return;
            }
            
            // 检查创造模式设置
            if (player.isCreative() && !CommonConfig.INSTANCE.enableInCreative.get()) {
                return;
            }

            // 获取玩家手中的物品
            ItemStack heldItem = player.getItemInHand(event.getHand());
            
            // 目标实体
            Entity targetEntity = event.getTarget();
            
            // 处理剪羊毛
            if (heldItem.getItem() instanceof ShearsItem && targetEntity instanceof SheepEntity) {
                SheepEntity sheep = (SheepEntity) targetEntity;
                if (sheep.readyForShearing()) {
                    if(ClientConfig.INSTANCE.Debug.get()) {
                        Onekeyminer.LOGGER.debug("检测到剪羊毛操作");
                    }
                    MinecraftServer server = player.level.getServer();
                    // 在下一个游戏刻执行，确保原始操作已完成
                        server.execute(() -> {
                        // 执行连锁剪羊毛
                        InteractionHandler.tryChainShearing(player, sheep, event.getHand());
                    });
                }
            }
        } catch (Exception e) {
            Onekeyminer.LOGGER.error("处理实体交互事件时发生错误: {}", e.getMessage());
            Onekeyminer.LOGGER.debug("详细错误栈", e);
        }
    }

    /**
     * 处理方块破坏事件
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) {
            return;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();

        try {
            // 检查连锁模式是否激活
            if (!ChainModeCapability.isChainModeActive(player)) {
                return;
            }

            // 获取玩家手中的工具
            ItemStack tool = player.getMainHandItem();
            BlockState state = event.getState();
            World level = player.level;
            ServerWorld serverLevel = (ServerWorld) level;
            TileEntity blockEntity = level.getBlockEntity(event.getPos());

            // The handler performs the whole chain, so prevent the original
            // break path from processing the origin a second time.
            if (ChainHandler.tryChainMine(player, event.getPos(), state, tool)) {
                event.setCanceled(true);
            }
        } catch (Exception e) {
            Onekeyminer.LOGGER.error("处理方块破坏事件时发生错误: {}", e.getMessage());
            Onekeyminer.LOGGER.debug("详细错误栈", e);
        }
    }

    /**
     * 判断是否是工具类物品
     */
    public static boolean isToolItem(ItemStack stack) {
        if (stack.isEmpty()) return false;

        Item item = stack.getItem();
        return   // 包括镐、铲、斧等挖掘工具
               item instanceof ShearsItem ||    // 剪刀
               item instanceof HoeItem ||       // 锄头
               item instanceof AxeItem ||       // 斧头（特别列出以确保兼容性）
               item instanceof ShovelItem
                || isPlantableItem(stack);      // 铲子（特别列出以确保兼容性）
    }

    /**
     * 判断是否是可种植物品
     */
    public static boolean isPlantableItem(ItemStack stack) {
        return PlantingHandler.isPlantableItem(stack);
    }
} 
