package org.xiyu.yee.onekeyminer.chain;

import net.minecraft.init.Blocks;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockMushroom;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockReed; // 甘蔗
import net.minecraft.block.BlockNetherWart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.WorldServer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumActionResult;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.xiyu.yee.onekeyminer.Onekeyminer;
import org.xiyu.yee.onekeyminer.config.CommonConfig;
import org.xiyu.yee.onekeyminer.network.ChainActionPacket;
import org.xiyu.yee.onekeyminer.network.NetworkHandler;

import java.util.*;
import java.util.concurrent.RejectedExecutionException;

public class PlantingHandler {

    // 防止递归和重复处理
    private static final Set<BlockPos> CURRENTLY_PLANTING = new HashSet<>();

    /**
     * 监听玩家右键方块事件（1.12事件注解）
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        ItemStack heldItem = player.getHeldItem(event.getHand());
        if (!isPlantableItem(heldItem)) {
            return;
        }
        if (isInBlacklist(heldItem)) {
            return;
        }
        BlockPos pos = event.getPos();
        synchronized (CURRENTLY_PLANTING) {
            if (CURRENTLY_PLANTING.contains(pos)) {
                return;
            }
            CURRENTLY_PLANTING.add(pos); // 保留直到任务真正结束
        }
        try {
            serverPlayer.getServer().addScheduledTask(() -> {
                try {
                    World level = serverPlayer.world;
                    IBlockState targetState = level.getBlockState(pos);
                    performChainPlanting(serverPlayer, event.getHand(), pos, targetState, heldItem);
                } finally {
                    synchronized (CURRENTLY_PLANTING) {
                        CURRENTLY_PLANTING.remove(pos);
                    }
                }
            });
        } catch (Exception e) {
            Onekeyminer.LOGGER.error("调度连锁种植任务失败: {}", e.getMessage());
            synchronized (CURRENTLY_PLANTING) {
                CURRENTLY_PLANTING.remove(pos);
            }
        }
    }

    /**
     * 执行连锁种植（核心逻辑适配）
     */
    private static void performChainPlanting(EntityPlayerMP player, EnumHand hand,
                                             BlockPos startPos, IBlockState targetState, ItemStack seedItem) {
        World level = player.world;
        if (!(level instanceof WorldServer)) {
            return;
        }

        // 配置读取（保留原逻辑）
        int maxPlants = CommonConfig.INSTANCE.maxBlocksInChain.get();
        if (player.isCreative()) {
            maxPlants = CommonConfig.INSTANCE.maxBlocksInChainCreative.get();
        }

        // 时间与迭代限制（保留原逻辑）
        final long startTime = System.currentTimeMillis();
        final long timeLimit = 2000;
        final int maxIterations = Math.min(10000, maxPlants * 10);
        int iterations = 0;

        // 种子数量计算
        int availableSeeds = countAvailableItems(player, seedItem.getItem());
        if (player.isCreative()) {
            availableSeeds = Integer.MAX_VALUE;
        }

        // BFS初始化：起始位置改为“耕地上方空气块”（解决耕地种植核心问题）
        BlockPos plantStartPos = startPos.up();
        Queue<BlockPos> positionsToCheck = new LinkedList<>();
        positionsToCheck.add(plantStartPos);

        Set<BlockPos> checkedPositions = new HashSet<>();
        checkedPositions.add(plantStartPos);

        Set<BlockPos> plantedPositions = new HashSet<>();
        boolean allowDiagonal = CommonConfig.INSTANCE.enableDiagonalChaining.get();
        int plantsCount = 0;

        try {
            while (!positionsToCheck.isEmpty() && plantsCount < maxPlants && iterations < maxIterations) {
                if (System.currentTimeMillis() - startTime > timeLimit) {
                    break;
                }

                iterations++;
                BlockPos currentPos = positionsToCheck.poll();

                if (plantedPositions.contains(currentPos)) {
                    continue;
                }
                if (plantsCount >= availableSeeds && !player.isCreative()) {
                    break;
                }

                // 检查当前位置是否可种植
                if (canPlantAt(level, currentPos, level.getBlockState(currentPos), seedItem)) {
                    try {
                        if (tryPlant(player, hand, currentPos, seedItem)) {
                            plantedPositions.add(currentPos);
                            plantsCount++;
                            if (!player.isCreative()) {
                                availableSeeds--;
                            }
                        }
                    } catch (Exception e) {
                        Onekeyminer.LOGGER.error("在位置 {} 种植时发生错误: {}", currentPos, e.getMessage());
                    }
                }

                // 添加相邻位置（1.12用offset替代relative）
                for (EnumFacing direction : EnumFacing.values()) {
                    try {
                        BlockPos adjacentPos = currentPos.offset(direction);
                        if (!checkedPositions.contains(adjacentPos) &&
                                adjacentPos.distanceSq(plantStartPos) <= 100) {
                            positionsToCheck.add(adjacentPos);
                            checkedPositions.add(adjacentPos);
                        }
                    } catch (Exception e) {
                    }
                }

                // 对角线位置（保留原逻辑）
                if (allowDiagonal && iterations % 2 == 0) {
                    for (int x = -1; x <= 1; x += 2) {
                        for (int y = -1; y <= 1; y += 2) {
                            for (int z = -1; z <= 1; z += 2) {
                                try {
                                    BlockPos diagonalPos = currentPos.add(x, y, z);
                                    if (!checkedPositions.contains(diagonalPos) &&
                                            diagonalPos.distanceSq(plantStartPos) <= 100) {
                                        positionsToCheck.add(diagonalPos);
                                        checkedPositions.add(diagonalPos);
                                    }
                                } catch (Exception e) {
                                }
                            }
                        }
                    }
                }

                // 队列大小限制（保留原逻辑）
                if (positionsToCheck.size() > 1000) {
                    Queue<BlockPos> limitedQueue = new LinkedList<>();
                    for (int i = 0; i < 500 && !positionsToCheck.isEmpty(); i++) {
                        limitedQueue.add(positionsToCheck.poll());
                    }
                    positionsToCheck = limitedQueue;
                }
            }
        } catch (Exception e) {
            Onekeyminer.LOGGER.error("连锁种植过程中发生未预期错误: {}", e.getMessage(), e);
        }

        // 发送网络消息（保留原逻辑）
        if (plantsCount > 0) {
            try {
                NetworkHandler.sendToPlayer(new ChainActionPacket("planting", plantsCount), player);
            } catch (Exception e) {
                Onekeyminer.LOGGER.error("发送连锁种植消息时出错: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 尝试种植（1.12核心适配：用onItemUse替代ItemUseContext）
     */
    private static boolean tryPlant(EntityPlayerMP player, EnumHand hand, BlockPos pos, ItemStack seedItem) {
        World level = player.world;

        // 关键修复1：检查种子状态（确保数量≥1，未损坏）
        if (seedItem.isEmpty() || seedItem.getCount() <= 0) {
            Onekeyminer.LOGGER.info("种植失败：种子数量不足，物品={}", seedItem.getUnlocalizedName());
            return false;
        }

        // 关键修复2：获取玩家当前手的实际物品（防止hand参数传递错误）
        ItemStack handStack = player.getHeldItem(hand);
        if (handStack.getItem() != seedItem.getItem()) {
            hand = EnumHand.MAIN_HAND;
            handStack = player.getHeldItem(hand);
            if (handStack.getItem() != seedItem.getItem()) {
                return false;
            }
        }

        // 目标点击位置修正：如果当前位置是空气且下方是耕地，则点击耕地而不是空气
        BlockPos clickPos = pos;
        IBlockState stateAtPos = level.getBlockState(pos);
        BlockPos belowPos = pos.down();
        IBlockState belowState = level.getBlockState(belowPos);
        if (level.isAirBlock(pos) && (belowState.getBlock() instanceof BlockFarmland || belowState.getBlock() == Blocks.FARMLAND)) {
            clickPos = belowPos; // 对种子使用耕地方块
        }
        EnumActionResult result = handStack.onItemUse(
                player,
                level,
                clickPos,
                hand,
                EnumFacing.UP,
                0.5F, 0.5F, 0.5F
        );
        boolean success = result == EnumActionResult.SUCCESS;

        // 二次确认：若是种子且空气->应当产生作物方块
        if (success && clickPos == belowPos) {
            IBlockState plantedAbove = level.getBlockState(belowPos.up());
            if (plantedAbove.getBlock() instanceof BlockCrops || plantedAbove.getBlock() instanceof BlockBush) {
                // 正常
            } else {
                // 有些物品（如甘蔗）直接占用空气方块
            }
        }

        Onekeyminer.LOGGER.info("种植调试：手={}, 目标点击块={}, 原始空气位置={}, 下方耕地={}, 种子数量={}, 结果={}",
                hand, clickPos, pos, belowPos, handStack.getCount(), result);

        return success;
    }

    /**
     * 检查是否可种植（适配1.12方块类）
     */
    private static boolean canPlantAt(World level, BlockPos pos, IBlockState state, ItemStack seedItem) {
        Item item = seedItem.getItem();
        BlockPos belowPos = pos.down();
        IBlockState belowState = level.getBlockState(belowPos);
        Block belowBlock = belowState.getBlock();
        // 必须保证当前位置为空气（防止在已有作物方块上重复种植）
        if (!level.isAirBlock(pos)) {
            return false;
        }
        // 普通耕地作物种子：空气方块上方且下方是耕地
        if (item == Items.WHEAT_SEEDS || item == Items.BEETROOT_SEEDS || item == Items.POTATO || item == Items.CARROT || item == Items.PUMPKIN_SEEDS || item == Items.MELON_SEEDS) {
            return belowBlock instanceof BlockFarmland || belowBlock == Blocks.FARMLAND;
        }
        // 甘蔗特殊：下方需是允许的方块且相邻有水
        if (item == Items.REEDS) {
            if (!(belowBlock == Blocks.DIRT || belowBlock == Blocks.SAND || belowBlock == Blocks.GRASS || belowBlock == Blocks.MYCELIUM)) {
                return false;
            }
            boolean waterNearby = false;
            for (EnumFacing f : new EnumFacing[]{EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST}) {
                BlockPos adj = belowPos.offset(f);
                Block adjBlock = level.getBlockState(adj).getBlock();
                if (adjBlock == Blocks.WATER || adjBlock == Blocks.FLOWING_WATER) {
                    waterNearby = true;
                    break;
                }
            }
            return waterNearby;
        }
        // 树苗/花类：下方是泥土/草/菌丝，当前位置空气
        if (item instanceof ItemBlock) {
            ItemBlock ib = (ItemBlock) item;
            Block block = ib.getBlock();
            if (block instanceof BlockSapling || block instanceof BlockFlower || block instanceof BlockBush || block instanceof BlockMushroom) {
                return belowBlock == Blocks.DIRT || belowBlock == Blocks.GRASS || belowBlock == Blocks.MYCELIUM;
            }
            if (block instanceof BlockCactus) { // 仙人掌：下方沙子且四周空气
                if (belowBlock != Blocks.SAND) return false;
                for (EnumFacing f : new EnumFacing[]{EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST}) {
                    if (!level.isAirBlock(pos.offset(f))) return false;
                }
                return true;
            }
            if (block instanceof BlockNetherWart) {
                return belowBlock == Blocks.SOUL_SAND;
            }
            if (block instanceof BlockReed) { // 兼容某些模组甘蔗方块物品
                boolean waterNearby = false;
                for (EnumFacing f : new EnumFacing[]{EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST}) {
                    BlockPos adj = belowPos.offset(f);
                    Block adjBlock = level.getBlockState(adj).getBlock();
                    if (adjBlock == Blocks.WATER || adjBlock == Blocks.FLOWING_WATER) {
                        waterNearby = true; break;
                    }
                }
                return waterNearby && (belowBlock == Blocks.DIRT || belowBlock == Blocks.SAND || belowBlock == Blocks.GRASS || belowBlock == Blocks.MYCELIUM);
            }
        }
        // 其他默认：允许的表面
        return canPlantOnSurface(item, belowState);
    }

    /**
     * 计算可用种子数量（适配1.12 Inventory）
     */
    private static int countAvailableItems(EntityPlayer player, Item item) {
        int count = 0;

        // 1.12获取主副手
        ItemStack main = player.getHeldItem(EnumHand.MAIN_HAND);
        if (!main.isEmpty() && main.getItem() == item) count += main.getCount();

        ItemStack off = player.getHeldItem(EnumHand.OFF_HAND);
        if (!off.isEmpty() && off.getItem() == item) count += off.getCount();

        // 1.12遍历物品栏（mainInventory）
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack != null && !stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }

        return count;
    }

    /**
     * 检查种植表面（适配1.12方块类和常量）
     */
    private static boolean canPlantOnSurface(Item item, IBlockState surfaceState) {
        Block surfaceBlock = surfaceState.getBlock();

        // 修复点1：普通种子（非ItemBlock）强制要求下方是耕地
        // 1.12中小麦、胡萝卜等种子不是ItemBlock，需单独判断
        if (item == Items.WHEAT_SEEDS ||
                item == Items.BEETROOT_SEEDS ||
                item == Items.POTATO ||
                item == Items.CARROT) {
            // 必须种在耕地上（BlockFarmland或Blocks.FARMLAND）
            return surfaceBlock instanceof BlockFarmland || surfaceBlock == Blocks.FARMLAND;
        }

        // 原有ItemBlock类型作物的判断（树苗、花等）
        if (item instanceof ItemBlock) {
            ItemBlock blockItem = (ItemBlock) item;
            Block block = blockItem.getBlock();

            // 作物（如方块形态的作物，若有）必须种在耕地上
            if (block instanceof BlockCrops) {
                return surfaceBlock instanceof BlockFarmland || surfaceBlock == Blocks.FARMLAND;
            }

            // 树苗/花种在泥土/草方块上（1.12草方块是Blocks.GRASS）
            if (block instanceof BlockSapling || block instanceof BlockFlower) {
                return surfaceBlock == Blocks.DIRT ||
                        surfaceBlock == Blocks.GRASS ||
                        surfaceBlock == Blocks.MYCELIUM;
            }

            // 蘑菇种在菌丝上
            if (block instanceof BlockMushroom) {
                return surfaceBlock == Blocks.MYCELIUM;
            }
        }

        // 通用表面规则（仅用于非种子/非作物类植物，如甘蔗）
        return surfaceBlock == Blocks.DIRT ||
                surfaceBlock == Blocks.GRASS ||
                surfaceBlock == Blocks.SAND ||
                surfaceBlock == Blocks.SOUL_SAND ||
                surfaceBlock == Blocks.MYCELIUM;
    }

    /**
     * 判断可种植物品（适配1.12类和物品）
     */
    static boolean isPlantableItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();

        // 黑名单检查
        if (isInBlacklist(stack)) {
            return false;
        }

        // 1.12方块物品判断（ItemBlock）
        if (item instanceof ItemBlock) {
            ItemBlock blockItem = (ItemBlock) item;
            Block block = blockItem.getBlock();

            // 1.12可种植方块类：BlockCrops→作物，BlockSapling→树苗等
            return block instanceof BlockCrops ||
                    block instanceof BlockSapling ||
                    block instanceof BlockBush ||
                    block instanceof BlockFlower ||
                    block instanceof BlockMushroom ||
                    block instanceof BlockCactus ||
                    block instanceof BlockReed || // 甘蔗
                    block instanceof BlockNetherWart;
        }

        // 物品名称关键词（保留原逻辑）
        String itemName = item.toString().toLowerCase();
        if (itemName.contains("seed") ||
                itemName.contains("sapling") ||
                itemName.contains("seedling") ||
                itemName.contains("plant")) {
            return true;
        }

        // 1.12存在的种植物品（移除1.16新增物品）
        return item == Items.WHEAT_SEEDS ||
                item == Items.BEETROOT_SEEDS ||
                item == Items.PUMPKIN_SEEDS ||
                item == Items.MELON_SEEDS ||
                item == Items.POTATO ||
                item == Items.CARROT ||
                item == Items.REEDS;// 甘蔗
    }

    /**
     * 黑名单检查（保留原逻辑）
     */
    private static boolean isInBlacklist(ItemStack stack) {
        return CommonConfig.INSTANCE.isSeedBlacklisted(stack.getItem());
    }

    /**
     * 计算种植位置（适配1.12类型）
     */
    public static List<BlockPos> computePlantingPositions(EntityPlayerMP player, WorldServer level,
                                                          BlockPos startPos, IBlockState targetState, ItemStack seedItem) {
        List<BlockPos> result = new ArrayList<>();
        if (!isPlantableItem(seedItem)) {
            return result;
        }

        // 起始位置改为耕地上方空气块
        BlockPos plantStartPos = startPos.up();
        int maxPlants = CommonConfig.INSTANCE.maxBlocksInChain.get();
        if (player.isCreative()) {
            maxPlants = CommonConfig.INSTANCE.maxBlocksInChainCreative.get();
        }

        final long startTime = System.currentTimeMillis();
        final long timeLimit = 500;
        final int maxIterations = Math.min(5000, maxPlants * 8);
        int iterations = 0;

        int availableSeeds = countAvailableItems(player, seedItem.getItem());
        if (player.isCreative()) {
            availableSeeds = Integer.MAX_VALUE;
        }

        Queue<BlockPos> positionsToCheck = new LinkedList<>();
        positionsToCheck.add(plantStartPos);
        Set<BlockPos> checkedPositions = new HashSet<>();
        checkedPositions.add(plantStartPos);

        boolean allowDiagonal = CommonConfig.INSTANCE.enableDiagonalChaining.get();
        int plantsCount = 0;

        try {
            while (!positionsToCheck.isEmpty() && plantsCount < maxPlants &&
                    iterations < maxIterations && plantsCount < availableSeeds) {
                if (System.currentTimeMillis() - startTime > timeLimit) {
                    break;
                }

                iterations++;
                BlockPos currentPos = positionsToCheck.poll();
                if (canPlantAt(level, currentPos, level.getBlockState(currentPos), seedItem)) {
                    result.add(currentPos);
                    plantsCount++;
                }

                // 相邻位置（1.12用offset）
                for (EnumFacing direction : EnumFacing.values()) {
                    BlockPos adjacentPos = currentPos.offset(direction);
                    if (!checkedPositions.contains(adjacentPos) &&
                            adjacentPos.distanceSq(plantStartPos) <= 100) {
                        positionsToCheck.add(adjacentPos);
                        checkedPositions.add(adjacentPos);
                    }
                }

                // 对角线位置（保留原逻辑）
                if (allowDiagonal && iterations % 2 == 0) {
                    for (int x = -1; x <= 1; x += 2) {
                        for (int y = -1; y <= 1; y += 2) {
                            for (int z = -1; z <= 1; z += 2) {
                                BlockPos diagonalPos = currentPos.add(x, y, z);
                                if (!checkedPositions.contains(diagonalPos) &&
                                        diagonalPos.distanceSq(plantStartPos) <= 100) {
                                    positionsToCheck.add(diagonalPos);
                                    checkedPositions.add(diagonalPos);
                                }
                            }
                        }
                    }
                }

                if (positionsToCheck.size() > 500) {
                    Queue<BlockPos> limitedQueue = new LinkedList<>();
                    for (int i = 0; i < 250 && !positionsToCheck.isEmpty(); i++) {
                        limitedQueue.add(positionsToCheck.poll());
                    }
                    positionsToCheck = limitedQueue;
                }
            }
        } catch (Exception e) {
            Onekeyminer.LOGGER.error("计算连锁种植位置时发生错误: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * 统一处理器调用方法（适配1.12类型）
     */
    public static void handleChainPlanting(EntityPlayerMP player, PlayerInteractEvent.RightClickBlock event) {
        BlockPos pos = event.getPos();
        EnumHand hand = event.getHand();
        ItemStack heldItem = player.getHeldItem(hand);

        boolean canProcess = false;
        synchronized (CURRENTLY_PLANTING) {
            if (!CURRENTLY_PLANTING.contains(pos)) {
                CURRENTLY_PLANTING.add(pos);
                canProcess = true;
            }
        }

        if (!canProcess) {
            return;
        }

        try {
            // 1.12服务器调度任务
            player.getServer().addScheduledTask(() -> {
                try {
                    World level = player.world;
                    IBlockState targetState = level.getBlockState(pos);
                    // 这里传入耕地本身位置，内部会使用 startPos.up() -> 空气块
                    performChainPlanting(player, hand, pos, targetState, heldItem);
                } catch (Throwable e) {
                    Onekeyminer.LOGGER.error("连锁种植执行期间发生严重错误: {}", e.getMessage(), e);
                } finally {
                    synchronized (CURRENTLY_PLANTING) {
                        CURRENTLY_PLANTING.remove(pos);
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            Onekeyminer.LOGGER.error("无法提交任务: {}", e.getMessage(), e);
        }
    }
}
