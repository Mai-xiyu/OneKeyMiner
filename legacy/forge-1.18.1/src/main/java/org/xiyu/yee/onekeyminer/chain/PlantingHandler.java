package org.xiyu.yee.onekeyminer.chain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.xiyu.yee.onekeyminer.Onekeyminer;
import org.xiyu.yee.onekeyminer.config.CommonConfig;
import org.xiyu.yee.onekeyminer.network.ChainActionPacket;
import org.xiyu.yee.onekeyminer.network.NetworkHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.concurrent.RejectedExecutionException;

public class PlantingHandler {

    // 防止递归和重复处理
    private static final Set<BlockPos> CURRENTLY_PLANTING = new HashSet<>();

    /**
     * 监听玩家右键方块事件
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = (Player) event.getEntity();

        // 只处理服务器端玩家
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // 获取手中物品
        ItemStack heldItem = player.getItemInHand(event.getHand());

        // 改进的物品类型检查 - 使用继承关系而不是硬编码
        if (!isPlantableItem(heldItem)) {
            return;
        }

        // 检查是否在黑名单中
        if (isInBlacklist(heldItem)) {
            return;
        }

        // 获取目标方块位置
        BlockPos pos = event.getPos();

        // 避免重复处理
        if (CURRENTLY_PLANTING.contains(pos)) {
            return;
        }

        // 先让原始交互事件完成

        // 添加到正在处理的集合
        CURRENTLY_PLANTING.add(pos);

        try {
            // 在下一个游戏刻执行，确保原始操作已完成
                serverPlayer.level.getServer().execute(() -> {
                    Level level = serverPlayer.level;
                BlockState targetState = level.getBlockState(pos);

                // 执行连锁种植
                performChainPlanting(serverPlayer, event.getHand(), pos, targetState, heldItem);
            });
        } finally {
            CURRENTLY_PLANTING.remove(pos);
        }
    }

    /**
     * 执行连锁种植操作 - 修复死锁和无限循环问题
     */
    private static void performChainPlanting(ServerPlayer player, InteractionHand hand,
                                             BlockPos startPos, BlockState targetState, ItemStack seedItem) {
        Level level = player.level;
        if (!(level instanceof ServerLevel)) {
            return;
        }

        // 获取配置的最大种植数量
        int maxPlants = CommonConfig.INSTANCE.maxBlocksInChain.get();
        if (player.isCreative()) {
            maxPlants = CommonConfig.INSTANCE.maxBlocksInChainCreative.get();
        }

        // 时间限制 - 防止过长运行时间导致服务器卡顿
        final long startTime = System.currentTimeMillis();
        final long timeLimit = 2000; // 最多执行2秒

        // 迭代限制 - 防止过多循环
        final int maxIterations = Math.min(10000, maxPlants * 10);
        int iterations = 0;

        // 预先计算总共可用的种子数量
        int availableSeeds = countAvailableItems(player, seedItem.getItem());
        if (player.isCreative()) {
            availableSeeds = Integer.MAX_VALUE;
        }

        // 使用BFS搜索可种植位置
        Queue<BlockPos> positionsToCheck = new LinkedList<>();
        positionsToCheck.add(startPos);

        Set<BlockPos> checkedPositions = new HashSet<>();
        checkedPositions.add(startPos);

        Set<BlockPos> plantedPositions = new HashSet<>();

        boolean allowDiagonal = CommonConfig.INSTANCE.enableDiagonalChaining.get();

        int plantsCount = 0;

        try {
            while (!positionsToCheck.isEmpty() && plantsCount < maxPlants && iterations < maxIterations) {
                // 检查是否超时
                if (System.currentTimeMillis() - startTime > timeLimit) {
                    break;
                }

                iterations++;
                BlockPos currentPos = positionsToCheck.poll();

                // 如果当前位置已经被处理过，跳过
                if (plantedPositions.contains(currentPos)) {
                    continue;
                }

                // 检查种子数量
                if (plantsCount >= availableSeeds && !player.isCreative()) {
                    break;
                }

                // 检查是否可以种植
                if (canPlantAt(level, currentPos, level.getBlockState(currentPos), seedItem)) {
                    // 尝试种植，使用try-catch防止种植过程中的异常
                    try {
                        if (tryPlant(player, hand, currentPos, seedItem)) {
                            plantedPositions.add(currentPos);
                            plantsCount++;

                            // 如果不是创造模式，减少种子数量
                            if (!player.isCreative()) {
                                availableSeeds--;
                            }
                        }
                    } catch (Exception e) {
                        Onekeyminer.LOGGER.error("在位置 {} 种植时发生错误: {}", currentPos, e.getMessage());
                        // 继续处理其他位置，不中断整个过程
                    }
                }

                // 安全地添加相邻位置
                for (Direction direction : Direction.values()) {
                    try {
                        BlockPos adjacentPos = currentPos.relative(direction);
                        if (!checkedPositions.contains(adjacentPos) &&
                                adjacentPos.distSqr(startPos) <= 100) { // 限制搜索范围
                            positionsToCheck.add(adjacentPos);
                            checkedPositions.add(adjacentPos);
                        }
                    } catch (Exception e) {
                        // 忽略添加位置时的异常
                    }
                }

                // 添加对角线位置，但要限制数量
                if (allowDiagonal && iterations % 2 == 0) { // 只在偶数迭代中添加对角线位置，减少总量
                    for (int x = -1; x <= 1; x += 2) { // 只使用-1和1，跳过0
                        for (int y = -1; y <= 1; y += 2) {
                            for (int z = -1; z <= 1; z += 2) {
                                try {
                                    BlockPos diagonalPos = currentPos.offset(x, y, z);
                                    if (!checkedPositions.contains(diagonalPos) &&
                                            diagonalPos.distSqr(startPos) <= 100) {
                                        positionsToCheck.add(diagonalPos);
                                        checkedPositions.add(diagonalPos);
                                    }
                                } catch (Exception e) {
                                    // 忽略添加位置时的异常
                                }
                            }
                        }
                    }
                }

                // 防止队列过大消耗过多内存
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

        // 种植完成后，添加日志和发送消息
        if (plantsCount > 0) {
            // 发送连锁种植消息到客户端
            try {

                // 发送网络消息
                NetworkHandler.sendToPlayer(new ChainActionPacket("planting", plantsCount), player);

            } catch (Exception e) {
                Onekeyminer.LOGGER.error("发送连锁种植消息时出错: {}", e.getMessage(), e);
            }
        } else {
        }
    }

    /**
     * 尝试在指定位置种植
     * 返回是否成功种植
     */
    private static boolean tryPlant(ServerPlayer player, InteractionHand hand, BlockPos pos, ItemStack seedItem) {
    Level level = player.level;

        // 创建点击上下文
        BlockHitResult hitResult = new BlockHitResult(
                Vec3.atCenterOf(pos),
                Direction.UP,
                pos,
                false
        );

        // 创建交互上下文
        UseOnContext context = new UseOnContext(player, hand, hitResult);

        // 尝试使用物品 - 这会触发种植行为
        InteractionResult result = seedItem.useOn(context);

        // 返回是否成功种植
        return result.consumesAction();
    }

    /**
     * 检查是否可以在指定位置种植
     */
    private static boolean canPlantAt(Level level, BlockPos pos, BlockState state, ItemStack seedItem) {
        // 检查方块是否是空气
        if (!level.isEmptyBlock(pos)) {
            return false;
        }

        // 获取下方方块
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);

        // 检查是否可以在下方方块上种植
        return canPlantOnSurface(seedItem.getItem(), belowState);
    }

    /**
     * 计算玩家可用的特定物品总数
     */
    private static int countAvailableItems(Player player, Item item) {
        int count = 0;

        // 计算主手和副手的物品
        if (player.getMainHandItem().getItem() == item) {
            count += player.getMainHandItem().getCount();
        }
        if (player.getOffhandItem().getItem() == item) {
            count += player.getOffhandItem().getCount();
        }

        // 计算物品栏中的物品
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }

        return count;
    }

    /**
     * 检查物品是否可以种植在指定表面
     */
    private static boolean canPlantOnSurface(Item item, BlockState surfaceState) {
        Block surfaceBlock = surfaceState.getBlock();

        // 常见种植规则
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();

            // 作物一般种在耕地上
            if (block instanceof CropBlock) {
                return surfaceBlock == Blocks.FARMLAND;
            }

            // 树苗和花可以种在泥土、草方块等上
            if (block instanceof SaplingBlock || block instanceof FlowerBlock) {
                return surfaceBlock == Blocks.DIRT ||
                        surfaceBlock == Blocks.GRASS_BLOCK ||
                        surfaceBlock == Blocks.PODZOL ||
                        surfaceBlock == Blocks.MYCELIUM;
            }

            // 蘑菇可以种在菌丝上
            if (block instanceof MushroomBlock) {
                return surfaceBlock == Blocks.MYCELIUM;
            }

            // 特殊作物
            if (item == Items.POTATO || item == Items.CARROT ||
                    item == Items.WHEAT_SEEDS || item == Items.BEETROOT_SEEDS) {
                return surfaceBlock == Blocks.FARMLAND;
            }
        }

        // 一般种植规则
        return surfaceBlock == Blocks.DIRT || // 泥土
                surfaceBlock == Blocks.GRASS_BLOCK || // 草方块
                surfaceBlock == Blocks.PODZOL || // 灰化土
                surfaceBlock == Blocks.SAND|| //沙子
                surfaceBlock == Blocks.SOUL_SAND ||//灵魂沙
                surfaceBlock == Blocks.MYCELIUM// 菌丝
                ||surfaceBlock instanceof FarmBlock|| surfaceBlock == Blocks.FARMLAND||CommonConfig.INSTANCE.isFarmlandAllowedForChainPlanting(surfaceBlock.defaultBlockState());
    }

    /**
     * 判断物品是否可以种植（基于类继承关系）
     */
    static boolean isPlantableItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();

        // 1. 检查是否是黑名单中的物品
        if (isInBlacklist(stack)) {
            return false;
        }

        // 2. 检查物品是否是BlockItem，以及对应的方块是否是可种植的
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);

            // 检查是否为目标方块（需要知道该方块的注册名，例如"modid:organic_compost"）

            // 检查方块是否继承自Crops, Sapling等可种植方块
                return block instanceof CropBlock ||
                        block instanceof SaplingBlock ||
                        block instanceof BushBlock ||
                        block instanceof FlowerBlock ||
                        block instanceof TallFlowerBlock ||
                        block instanceof CactusBlock ||
                        block instanceof TallGrassBlock ||
                        block instanceof FungusBlock ||
                        block instanceof RootsBlock ||
                        block instanceof NetherSproutsBlock ||
                        block instanceof CocoaBlock ||
                        block instanceof SugarCaneBlock ||
                        block instanceof NetherWartBlock ||
                        block instanceof AzaleaBlock;
        }

        // 3. 检查物品名称是否包含种子相关关键词（作为后备）
        String itemName = item.toString().toLowerCase();
        if (itemName.contains("seed") ||
                itemName.contains("sapling") ||
                itemName.contains("seedling") ||
                itemName.contains("plant")) {
            return true;
        }

        // 4. 列出特定的原版种植物品作为兜底
        return item == Items.WHEAT_SEEDS ||
                item == Items.BEETROOT_SEEDS ||
                item == Items.PUMPKIN_SEEDS ||
                item == Items.MELON_SEEDS ||
                item == Items.POTATO ||
                item == Items.CARROT ||
                item == Items.SWEET_BERRIES ||
                item == Items.GLOW_BERRIES ||
                item == Items.CRIMSON_FUNGUS ||
                item == Items.WARPED_FUNGUS ||
                item == Items.COCOA_BEANS ||
                item == Items.KELP ||
                item == Items.BAMBOO ||
                item == Items.SUGAR_CANE ||
                item == Items.CACTUS ||
                item == Items.SEA_PICKLE;
    }

    /**
     * 更直接地检查物品是否在黑名单中
     */
    private static boolean isInBlacklist(ItemStack stack) {
        return CommonConfig.INSTANCE.isSeedBlacklisted(stack.getItem());
    }

    /**
     * 判断方块是否是可种植的方块（不是作物但可以直接放置的植物，如树苗）
     */
    private static boolean isPlantableBlock(Block block) {
        return block == Blocks.OAK_SAPLING ||
            block == Blocks.SPRUCE_SAPLING ||
            block == Blocks.BIRCH_SAPLING ||
            block == Blocks.JUNGLE_SAPLING ||
            block == Blocks.ACACIA_SAPLING ||
            block == Blocks.DARK_OAK_SAPLING ||
            block == Blocks.AZALEA ||
            block == Blocks.FLOWERING_AZALEA;
    }

    /**
     * 计算连锁种植的所有位置，但不实际种植 - 提供给API调用
     */
    public static List<BlockPos> computePlantingPositions(ServerPlayer player, ServerLevel level,
                                                          BlockPos startPos, BlockState targetState, ItemStack seedItem) {
        List<BlockPos> result = new ArrayList<>();

        if (!isPlantableItem(seedItem)) {
            return result;
        }

        // 获取配置的最大种植数量
        int maxPlants = CommonConfig.INSTANCE.maxBlocksInChain.get();
        if (player.isCreative()) {
            maxPlants = CommonConfig.INSTANCE.maxBlocksInChainCreative.get();
        }

        // 时间和迭代限制防止过长运行
        final long startTime = System.currentTimeMillis();
        final long timeLimit = 500; // 计算时间限制为500毫秒
        final int maxIterations = Math.min(5000, maxPlants * 8); // 降低计算时的迭代限制
        int iterations = 0;

        // 计算可用种子数量
        int availableSeeds = countAvailableItems(player, seedItem.getItem());
        if (player.isCreative()) {
            availableSeeds = Integer.MAX_VALUE;
        }

        // BFS搜索逻辑 - 复用performChainPlanting的逻辑但只计算位置
        Queue<BlockPos> positionsToCheck = new LinkedList<>();
        positionsToCheck.add(startPos);

        Set<BlockPos> checkedPositions = new HashSet<>();
        checkedPositions.add(startPos);

        boolean allowDiagonal = CommonConfig.INSTANCE.enableDiagonalChaining.get();
        int plantsCount = 0;

        try {
            while (!positionsToCheck.isEmpty() && plantsCount < maxPlants &&
                    iterations < maxIterations && plantsCount < availableSeeds) {

                // 检查是否超时
                if (System.currentTimeMillis() - startTime > timeLimit) {
                    break;
                }

                iterations++;
                BlockPos currentPos = positionsToCheck.poll();

                // 检查是否可以种植
                if (canPlantAt(level, currentPos, level.getBlockState(currentPos), seedItem)) {
                    result.add(currentPos);
                    plantsCount++;
                }

                // 添加相邻位置
                for (Direction direction : Direction.values()) {
                    BlockPos adjacentPos = currentPos.relative(direction);
                    if (!checkedPositions.contains(adjacentPos) &&
                            adjacentPos.distSqr(startPos) <= 100) { // 限制搜索范围
                        positionsToCheck.add(adjacentPos);
                        checkedPositions.add(adjacentPos);
                    }
                }

                // 添加对角线位置（如果启用）
                if (allowDiagonal && iterations % 2 == 0) {
                    for (int x = -1; x <= 1; x += 2) {
                        for (int y = -1; y <= 1; y += 2) {
                            for (int z = -1; z <= 1; z += 2) {
                                BlockPos diagonalPos = currentPos.offset(x, y, z);
                                if (!checkedPositions.contains(diagonalPos) &&
                                        diagonalPos.distSqr(startPos) <= 100) {
                                    positionsToCheck.add(diagonalPos);
                                    checkedPositions.add(diagonalPos);
                                }
                            }
                        }
                    }
                }

                // 防止队列过大
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

    // 移除原有的事件监听器，改为提供公共方法给统一处理器调用
    public static void handleChainPlanting(ServerPlayer player, PlayerInteractEvent.RightClickBlock event) {
        BlockPos pos = event.getPos();
        InteractionHand hand = event.getHand();
        ItemStack heldItem = player.getItemInHand(hand);

        // 简化同步逻辑
        boolean canProcess = false;

        synchronized(CURRENTLY_PLANTING) {
            if (!CURRENTLY_PLANTING.contains(pos)) {
                CURRENTLY_PLANTING.add(pos);
                canProcess = true;
            }
        }

        if (!canProcess) {
            return;
        }

        try {
                player.level.getServer().submit(() -> {
                try {
                    // 避免耗时操作在同步块内执行
                        Level level = player.level;
                    BlockState targetState = level.getBlockState(pos);

                    performChainPlanting(player, hand, pos, targetState, heldItem);
                } catch (Throwable e) {
                    // 捕获所有可能的错误，包括Error
                    Onekeyminer.LOGGER.error("连锁种植执行期间发生严重错误: {}", e.getMessage(), e);
                } finally {
                    // 无论如何确保移除位置标记
                    synchronized(CURRENTLY_PLANTING) {
                        CURRENTLY_PLANTING.remove(pos);
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            Onekeyminer.LOGGER.error("无法提交任务: {}", e.getMessage(), e);
        }
    }
} 