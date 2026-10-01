package org.xiyu.yee.onekeyminer.chain;

import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.WorldServer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumHand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.common.IShearable;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.*;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextComponentString;
import org.xiyu.yee.onekeyminer.Onekeyminer;
import org.xiyu.yee.onekeyminer.config.CommonConfig;
import org.xiyu.yee.onekeyminer.network.ChainActionPacket;
import org.xiyu.yee.onekeyminer.network.NetworkHandler;
import net.minecraft.util.text.ITextComponent;
import org.xiyu.yee.onekeyminer.config.ClientConfig;

import java.util.*;

/**
 * 处理连锁交互（例如右键方块）
 */
public class InteractionHandler {
    // 防止重复交互的方块位置集合
    private static final Set<BlockPos> PROCESSED_POSITIONS = new HashSet<>();
    // 防止重复交互的实体ID集合
    private static final Set<Integer> PROCESSED_ENTITY_IDS = new HashSet<>();

    /**
     * 检查物品是否是有效的交互工具
     */
    public static boolean isValidInteractionTool(ItemStack stack) {
        if (stack.isEmpty()) return false;

        Item item = stack.getItem();
    return item instanceof ItemHoe ||       // 锄头
        item instanceof ItemAxe ||       // 斧子
        item instanceof ItemSpade ||    // 铲子
        item instanceof ItemShears;    // 剪刀
    }

    /**
     * 计算连锁交互的所有位置，但不实际执行交互 - 提供给API调用
     */
    public static List<BlockPos> computeInteractionPositions(EntityPlayerMP player, WorldServer level,
                                                             BlockPos startPos, IBlockState targetState, EnumHand hand) {
        List<BlockPos> result = new ArrayList<>();
        
        // 基础验证
        if (player == null || level == null || startPos == null || targetState == null) {
            return result;
        }

        ItemStack heldItem = player.getHeldItem(hand);
        if (heldItem.isEmpty() || !isValidInteractionTool(heldItem)) {
            return result;
        }

        if (level.isAirBlock(startPos)) {
            return result;
        }

        // 配置参数
        int maxBlocks = player.isCreative()
                ? CommonConfig.INSTANCE.maxBlocksInChainCreative.get()
                : CommonConfig.INSTANCE.maxBlocksInChain.get();
        int maxDepth = CommonConfig.INSTANCE.maxChainDepth.get();
        boolean includeDiagonal = CommonConfig.INSTANCE.enableDiagonalChaining.get();

        // 搜索队列和访问记录
        Queue<BlockSearchNode> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(new BlockSearchNode(startPos, 0));

        // 时间和迭代限制
        final long startTime = System.currentTimeMillis();
        final long timeLimit = 300; // 计算时间限制为300毫秒
        final int maxIterations = Math.min(5000, maxBlocks * 8);
        int iterations = 0;

        try {
            while (!queue.isEmpty() && result.size() < maxBlocks && iterations < maxIterations) {
                // 检查是否超时
                if (System.currentTimeMillis() - startTime > timeLimit) {
                    break;
                }

                iterations++;
                BlockSearchNode node = queue.poll();
                BlockPos pos = node.pos;

                // 深度限制
                if (node.depth > maxDepth) {
                    continue;
                }

                // 如果已访问，跳过
                if (visited.contains(pos)) {
                    continue;
                }
                visited.add(pos);

                // 检查方块状态是否匹配
                IBlockState state = level.getBlockState(pos);
                if (!isMatchingBlock(state, targetState)) {
                    continue;
                }

                // 添加到结果中
                result.add(pos);

                // 将相邻方块添加到队列
                for (BlockPos neighbor : getNeighborPositions(pos, includeDiagonal)) {
                    if (!visited.contains(neighbor)) {
                        queue.offer(new BlockSearchNode(neighbor, node.depth + 1));
                    }
                }
            }
        } catch (Exception e) {
            Onekeyminer.LOGGER.error("计算连锁交互位置时发生错误: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * 处理玩家交互事件
     * 用于兼容旧的调用方式
     * @deprecated 使用 {@link #tryChainInteraction} 代替
     */
    @Deprecated
    public static void handleChainInteraction(EntityPlayerMP player, PlayerInteractEvent.RightClickBlock event) {
        // 调用新的实现方法
    tryChainInteraction(
        player,
        (WorldServer)event.getWorld(),
        event.getPos(),
        event.getFace(),
        // 从BlockHitResult获取点击位置的Vec3
        event.getHitVec(),
        event.getHand()
    );
    }

    /**
     * 尝试连锁交互
     * @param player 玩家
     * @param level 世界
     * @param pos 初始方块位置
     * @param side 交互面
     * @param hitVec 点击坐标
     * @param hand 使用的手
     * @return 是否成功处理了连锁交互
     */
    public static boolean tryChainInteraction(EntityPlayerMP player, WorldServer level, BlockPos pos,
                                              EnumFacing side, Vec3d hitVec, EnumHand hand) {
        // 清空已处理集合
        PROCESSED_POSITIONS.clear();

        // 获取玩家手中物品
        ItemStack heldItem = player.getHeldItem(hand);
        if (heldItem.isEmpty() || !isValidInteractionTool(heldItem)) {
            return false;
        }

        // 获取初始方块状态
        IBlockState initialState = level.getBlockState(pos);
        if (level.isAirBlock(pos)) {
            return false;
        }
    if(ClientConfig.INSTANCE.Debug.get()) Onekeyminer.LOGGER.debug("尝试连锁交互: {}", initialState.getBlock().getUnlocalizedName());

        // 创建初始方块点击结果 (1.12.2构造函数)
        RayTraceResult initialHit = new RayTraceResult(hitVec, side, pos);

        // 获取配置的最大交互数量
        int maxBlocks = player.isCreative()
                ? CommonConfig.INSTANCE.maxBlocksInChainCreative.get()
                : CommonConfig.INSTANCE.maxBlocksInChain.get();

        // 是否启用对角线检查
        boolean includeDiagonal = CommonConfig.INSTANCE.enableDiagonalChaining.get();

        // 使用洪水填充算法执行连锁交互
        int processedCount = floodFillInteraction(player, level, initialState, initialHit, hand,
                maxBlocks, CommonConfig.INSTANCE.maxChainDepth.get(), includeDiagonal);

        // 发送交互结果
        if (processedCount > 0) {
            // 向客户端发送反馈
            NetworkHandler.sendToPlayer(new ChainActionPacket("interaction", processedCount), player);
            return true;
        }

        return false;
    }

    /**
     * 使用洪水填充算法执行连锁交互
     */
    private static int floodFillInteraction(EntityPlayerMP player, WorldServer level, IBlockState targetState,
                                            RayTraceResult initialHit, EnumHand hand,
                                            int maxBlocks, int maxDepth, boolean includeDiagonal) {
        // 使用队列进行广度优先搜索
        Queue<BlockSearchNode> queue = new LinkedList<>();

        // 添加初始节点
        queue.add(new BlockSearchNode(initialHit.getBlockPos(), 0));

        // 已处理的方块数量
        int processedCount = 0;

        // 获取玩家手中物品
        ItemStack heldItem = player.getHeldItem(hand);

        while (!queue.isEmpty() && processedCount < maxBlocks) {
            // 获取下一个待处理的节点
            BlockSearchNode node = queue.poll();
            BlockPos pos = node.pos;

            // 如果深度超过最大值，跳过
            if (node.depth > maxDepth) {
                continue;
            }

            // 如果方块已处理，跳过
            if (PROCESSED_POSITIONS.contains(pos)) {
                continue;
            }

            // 检查方块状态是否匹配目标
            IBlockState state = level.getBlockState(pos);
            if (!isMatchingBlock(state, targetState)) {
                continue;
            }

            // 检查工具是否已损坏
            if (heldItem.isEmpty() || heldItem.getItemDamage() >= heldItem.getMaxDamage() - 1) {
                break;
            }

            // 标记为已处理
            PROCESSED_POSITIONS.add(pos);

            // 在1.12.2中直接调用onItemUse
            EnumActionResult result = heldItem.onItemUse(player, level, pos, hand, 
                initialHit.sideHit, 
                (float)initialHit.hitVec.x, 
                (float)initialHit.hitVec.y, 
                (float)initialHit.hitVec.z);

            if (result == EnumActionResult.SUCCESS) {
                processedCount++;

                if(ClientConfig.INSTANCE.Debug.get()) Onekeyminer.LOGGER.debug("连锁交互成功: {}", pos);
            }

            // 将相邻方块添加到队列
            for (BlockPos neighbor : getNeighborPositions(pos, includeDiagonal)) {
                queue.offer(new BlockSearchNode(neighbor, node.depth + 1));
            }
        }

        Onekeyminer.LOGGER.debug("连锁交互完成，共处理 {} 个方块", processedCount);
        return processedCount;
    }

    /**
     * 检查方块状态是否匹配目标
     */
    private static boolean isMatchingBlock(IBlockState state, IBlockState target) {
        if (state.getBlock() == net.minecraft.init.Blocks.AIR || target.getBlock() == net.minecraft.init.Blocks.AIR) {
            return false;
        }

        if ((CommonConfig.INSTANCE.matchIBlockState.get() && !(target.getBlock() instanceof BlockCrops)) || (CommonConfig.INSTANCE.matchseedIBlockState.get() && target.getBlock() instanceof BlockCrops)) {
            // 完全匹配方块状态
            return state.equals(target);
        } else {
            // 只匹配方块类型
            return state.getBlock() == target.getBlock();
        }
    }

    /**
     * 获取相邻方块位置
     */
    private static List<BlockPos> getNeighborPositions(BlockPos pos, boolean includeDiagonal) {
        List<BlockPos> neighbors = new ArrayList<>();

        // 直接相邻的6个方向
        neighbors.add(pos.up());
        neighbors.add(pos.down());
        neighbors.add(pos.north());
        neighbors.add(pos.south());
        neighbors.add(pos.east());
        neighbors.add(pos.west());

        // 如果允许对角线，添加12个对角线位置
        if (includeDiagonal) {
            // 水平对角线
            neighbors.add(pos.north().east());
            neighbors.add(pos.north().west());
            neighbors.add(pos.south().east());
            neighbors.add(pos.south().west());

            // 垂直对角线
            neighbors.add(pos.up().north());
            neighbors.add(pos.up().south());
            neighbors.add(pos.up().east());
            neighbors.add(pos.up().west());
            neighbors.add(pos.down().north());
            neighbors.add(pos.down().south());
            neighbors.add(pos.down().east());
            neighbors.add(pos.down().west());

            // 完全对角线（八个角）
            neighbors.add(pos.up().north().east());
            neighbors.add(pos.up().north().west());
            neighbors.add(pos.up().south().east());
            neighbors.add(pos.up().south().west());
            neighbors.add(pos.down().north().east());
            neighbors.add(pos.down().north().west());
            neighbors.add(pos.down().south().east());
            neighbors.add(pos.down().south().west());
        }

        return neighbors;
    }

    /**
     * 方块搜索节点，用于跟踪深度
     */
    private static class BlockSearchNode {
        final BlockPos pos;
        final int depth;

        BlockSearchNode(BlockPos pos, int depth) {
            this.pos = pos;
            this.depth = depth;
        }
    }

    /**
     * 处理剪羊毛的连锁操作 - 不依赖InteractionResult
     * @param player 玩家
     * @param targetSheep 被交互的目标羊
     * @param hand 使用的手
     * @return 是否成功处理了连锁剪羊毛
     */
    public static boolean tryChainShearing(EntityPlayerMP player, EntitySheep targetSheep, EnumHand hand) {
        // 清空已处理集合
        PROCESSED_ENTITY_IDS.clear();

        // 获取玩家手中物品
        ItemStack heldItem = player.getHeldItem(hand);
        if (heldItem.isEmpty() || !(heldItem.getItem() instanceof ItemShears)) {
            return false;
        }

        if(ClientConfig.INSTANCE.Debug.get()) {
            Onekeyminer.LOGGER.debug("尝试连锁剪羊毛: {}", targetSheep.getEntityId());
        }

        // 获取配置的最大交互数量
        int maxEntities = player.isCreative()
                ? CommonConfig.INSTANCE.maxBlocksInChainCreative.get()
                : CommonConfig.INSTANCE.maxBlocksInChain.get();

        // 获取搜索范围 - 使用maxChainDepth作为基础半径，对角连锁时增加范围
        double searchRadius = CommonConfig.INSTANCE.maxChainDepth.get() * 0.5;
        if (CommonConfig.INSTANCE.enableDiagonalChaining.get()) {
            // 对角连锁时增加50%范围
            searchRadius *= 1.5;
        }

    // 将初始羊添加到已处理集合
    PROCESSED_ENTITY_IDS.add(targetSheep.getEntityId());

    // 检查是否启用传送掉落物
    boolean teleportDrops = CommonConfig.INSTANCE.teleportDropsToPlayer.get();
    BlockPos playerPos = player.getPosition();
    WorldServer level = (WorldServer) player.world;

    // 一次性获取所有区域内的羊 (1.12: getEntitiesWithinAABB)
    AxisAlignedBB searchBox = new AxisAlignedBB(
        targetSheep.posX - searchRadius, targetSheep.posY - searchRadius, targetSheep.posZ - searchRadius,
        targetSheep.posX + searchRadius, targetSheep.posY + searchRadius, targetSheep.posZ + searchRadius);

    List<EntitySheep> allSheepInArea = level.getEntitiesWithinAABB(EntitySheep.class, searchBox);

        if(ClientConfig.INSTANCE.Debug.get()) {
            Onekeyminer.LOGGER.debug("找到 {} 只可剪羊", allSheepInArea.size() + 1); // +1 包括目标羊
        }

        // 计数器
        int shearedCount = 1; // 包括初始的羊

        // 将要剪毛的羊 UUID 列表（包含目标羊与检测到的可剪羊）用于 API 暴露给附属模组
        List<UUID> shearingUuids = new ArrayList<>();
        shearingUuids.add(targetSheep.getUniqueID());
        for (EntitySheep s : allSheepInArea) {
            shearingUuids.add(s.getUniqueID());
        }
        // 将列表写入 ChainPositionStore，key 使用玩家 UUID 的字符串形式
        try {
            String key = player.getUniqueID().toString();
            org.xiyu.yee.onekeyminer.api.ChainPositionStore.setShearingEntityUuids(key, shearingUuids);
        } catch (Exception e) {
            Onekeyminer.LOGGER.debug("写入剪羊 UUID 列表到 ChainPositionStore 时出错: {}", e.getMessage());
        }

        // 批量处理所有羊
        for (EntitySheep sheep : allSheepInArea) {
            // 如果已经达到最大数量，停止处理
            if (shearedCount >= maxEntities) {
                break;
            }

            try {
                // 检查羊是否可以被剪毛和是否有毛可剪 (保守检查：仅检查存活与未处理)
                if (sheep.isEntityAlive() && !PROCESSED_ENTITY_IDS.contains(sheep.getEntityId())) {
                    // 保存剪毛前的状态（尝试兼容不同版本接口）
                    boolean wasShearable = true;
                    int woolColorBefore = 0;

                    // 使用版本无关的方法检查剪毛成功与否
                    boolean shearingSuccessful = false;

                    if (teleportDrops) {
                        // 保存羊原来的位置
                        double originalX = sheep.posX;
                        double originalY = sheep.posY;
                        double originalZ = sheep.posZ;

                        try {
                            // 临时将羊移动到玩家位置附近
                            sheep.setPosition(playerPos.getX(), playerPos.getY(), playerPos.getZ());

                            // 尝试使用1.12兼容的IShearable接口来剪毛
                            List<ItemStack> items = new ArrayList<>();
                            try {
                                if (sheep instanceof IShearable) {
                                    items = ((IShearable) sheep).onSheared(heldItem, level, sheep.getPosition(), 1);
                                }
                            } catch (Exception ex) {
                                // 忽略并继续
                            }

                            // 检查是否有掉落物表示剪毛成功
                            shearingSuccessful = !items.isEmpty();

                            // 模拟伤害工具（1.12替代接口）
                            if (shearingSuccessful && !player.capabilities.isCreativeMode) {
                                try {
                                    heldItem.damageItem(1, player);
                                } catch (Exception ex) {
                                    // ignore
                                }
                            }

                            // 触发掉落物生成
                            for (ItemStack itemStack : items) {
                                EntityItem itemEntity = new EntityItem(level, sheep.posX, sheep.posY + 1.0D, sheep.posZ, itemStack);
                                try {
                                    level.spawnEntity(itemEntity);
                                } catch (Exception ex) {
                                    // ignore
                                }
                            }

                            if(ClientConfig.INSTANCE.Debug.get()) {
                                Onekeyminer.LOGGER.debug("剪羊状态: 使用IShearable.onSheared, 掉落物数量={}", items.size());
                            }

                            // 恢复羊的原始位置
                            sheep.setPosition(originalX, originalY, originalZ);
                        } catch (Exception e) {
                            Onekeyminer.LOGGER.error("传送羊毛掉落物时出错: {}", e.getMessage());
                            sheep.setPosition(originalX, originalY, originalZ);
                        }
                    } else {
                        // 对于不需要传送掉落物的情况，使用IShearable接口
                        List<ItemStack> items = new ArrayList<>();
                        try {
                            if (sheep instanceof IShearable) {
                                items = ((IShearable) sheep).onSheared(heldItem, level, sheep.getPosition(), 1);
                            }
                        } catch (Exception ex) {
                            // ignore
                        }

                        shearingSuccessful = !items.isEmpty();

                        if (shearingSuccessful && !player.capabilities.isCreativeMode) {
                            try { heldItem.damageItem(1, player); } catch (Exception ex) {}
                        }

                        for (ItemStack itemStack : items) {
                            EntityItem itemEntity = new EntityItem(level, sheep.posX, sheep.posY + 1.0D, sheep.posZ, itemStack);
                            try { level.spawnEntity(itemEntity); } catch (Exception ex) {}
                        }

                        if(ClientConfig.INSTANCE.Debug.get()) {
                            Onekeyminer.LOGGER.debug("剪羊状态: 使用IShearable.onSheared, 掉落物数量={}", items.size());
                        }
                    }

                    // 如果剪毛成功
                        if (shearingSuccessful) {
                        // 标记为已处理并增加计数
                        PROCESSED_ENTITY_IDS.add(sheep.getEntityId());
                        shearedCount++;

                        if(ClientConfig.INSTANCE.Debug.get()) {
                            Onekeyminer.LOGGER.debug("成功剪了羊 {}", sheep.getEntityId());
                        }

                        // 如果工具损坏了，停止处理
                        if (heldItem.isEmpty()) {
                            break;
                        }
                    } else if(ClientConfig.INSTANCE.Debug.get()) {
                        Onekeyminer.LOGGER.debug("剪羊 {} 失败，尽管它应该是可剪的", sheep.getEntityId());
                    }
                }
            } catch (Exception e) {
                // 捕获并记录处理单个羊时的错误，但继续处理其他羊
                Onekeyminer.LOGGER.error("处理羊 {} 时出错: {}", sheep.getEntityId(), e.getMessage());
            }
        }

        if(ClientConfig.INSTANCE.Debug.get()) {
            Onekeyminer.LOGGER.debug("成功剪了 {} 只羊", shearedCount);
        }

        // 根据客户端配置发送反馈消息
        if (shearedCount > 1 && ClientConfig.INSTANCE.showBlockCount.get()) {
            // 获取消息风格
            String messageStyle = ClientConfig.INSTANCE.messageStyle.get();

            // 发送反馈
            if (messageStyle.equals("chat") || messageStyle.equals("both")) {
                // 1.12: 使用 TextComponentTranslation 并调用单参数 sendMessage
                player.sendMessage(new TextComponentTranslation("message.onekeyminer.chain_shearing", shearedCount));
            }

            if (messageStyle.equals("actionbar") || messageStyle.equals("both")) {
                NetworkHandler.sendToPlayer(new ChainActionPacket("shearing", shearedCount), player);
            }

            return true;
        }

        return shearedCount > 1;
    }
} 