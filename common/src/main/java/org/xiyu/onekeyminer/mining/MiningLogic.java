package org.xiyu.onekeyminer.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.api.OneKeyMinerAPI;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.MinerConfig;
import org.xiyu.onekeyminer.platform.PlatformServices;

import java.util.*;

/**
 * 核心挖矿逻辑处理器
 * 
 * <p>实现基于 BFS（广度优先搜索）的连锁挖矿算法。
 * 所有挖矿操作都通过模拟玩家破坏方块来执行，确保与保护插件和原版机制的兼容性。</p>
 * 
 * <h2>算法特点</h2>
 * <ul>
 *   <li>使用队列实现 BFS，避免递归导致的 StackOverflow</li>
 *   <li>严格限制单次操作的最大方块数和最大距离</li>
 *   <li>支持 6 向（正交）和 26 向（含对角线）搜索模式</li>
 *   <li>实时检查玩家权限、工具耐久度和饥饿值</li>
 * </ul>
 * 
 * <h2>兼容性保证</h2>
 * <ul>
 *   <li>触发 BlockBreakEvent 确保保护插件正常工作</li>
 *   <li>正确应用战利品表和附魔效果</li>
 *   <li>正确处理工具耐久度消耗</li>
 * </ul>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.0
 * @since Minecraft 1.21.9
 */
public class MiningLogic {
    
    /** 6 向搜索偏移量（正交方向） */
    private static final BlockPos[] ORTHOGONAL_OFFSETS = {
            new BlockPos(1, 0, 0),
            new BlockPos(-1, 0, 0),
            new BlockPos(0, 1, 0),
            new BlockPos(0, -1, 0),
            new BlockPos(0, 0, 1),
            new BlockPos(0, 0, -1)
    };
    
    /** 26 向搜索偏移量（含对角线） */
    private static final BlockPos[] DIAGONAL_OFFSETS;
    
    static {
        // 生成 26 向偏移量（排除原点）
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        offsets.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        DIAGONAL_OFFSETS = offsets.toArray(new BlockPos[0]);
    }
    
    /** 防止重入的标记（使用 ThreadLocal 支持多线程） */
    private static final ThreadLocal<Boolean> IS_MINING = ThreadLocal.withInitial(() -> false);
    
    /**
     * 处理方块破坏事件，触发连锁挖矿
     * 
     * <p>此方法应在方块破坏事件处理器中调用。
     * 它会检查所有前置条件并执行连锁挖矿。</p>
     * 
     * @param player 破坏方块的服务端玩家
     * @param level 世界实例
     * @param originPos 起始方块位置
     * @param originState 起始方块状态
     */
    public static void onBlockBreak(ServerPlayer player, Level level, BlockPos originPos, BlockState originState) {
        // 防止重入
        if (IS_MINING.get()) {
            return;
        }
        
        MinerConfig config = ConfigManager.getConfig();
        
        // 检查模组是否启用
        if (!config.enabled) {
            return;
        }
        
        // 检查激活条件
        if (!checkActivation(player, config)) {
            return;
        }
        
        // 检查方块是否在白名单中
        if (!OneKeyMinerAPI.isBlockAllowed(originState.getBlock())) {
            return;
        }
        
        // 检查工具是否允许
        ItemStack tool = player.getMainHandItem();
        if (!OneKeyMinerAPI.isToolAllowed(tool)) {
            return;
        }
        
        // 执行连锁挖矿
        try {
            IS_MINING.set(true);
            performVeinMining(player, level, originPos, originState, tool, config);
        } finally {
            IS_MINING.set(false);
        }
    }
    
    /**
     * 执行连锁挖矿算法
     * 
     * @param player 玩家
     * @param level 世界
     * @param originPos 起始位置
     * @param originState 起始方块状态
     * @param tool 使用的工具
     * @param config 当前配置
     */
    private static void performVeinMining(
            ServerPlayer player, 
            Level level, 
            BlockPos originPos, 
            BlockState originState, 
            ItemStack tool, 
            MinerConfig config
    ) {
        // 收集要挖掘的方块
        List<BlockPos> blocksToMine = collectBlocks(level, originPos, originState, config);
        
        // 如果只有起始方块（已经被挖掘），直接返回
        if (blocksToMine.isEmpty()) {
            return;
        }
        
        // 执行挖矿
        MiningResult result = mineBlocks(player, level, blocksToMine, tool, config);
        
        // 显示统计信息
        if (config.showStats && result.totalMined() > 0) {
            sendMiningStats(player, result);
        }
        
        OneKeyMiner.LOGGER.debug("连锁挖矿完成: 挖掘 {} 个方块, 停止原因: {}", 
                result.totalMined(), result.stopReason());
    }
    
    /**
     * 使用 BFS 算法收集相连的同类方块
     * 
     * <p>算法流程：</p>
     * <ol>
     *   <li>将起始位置加入队列</li>
     *   <li>从队列取出位置，检查相邻方块</li>
     *   <li>如果相邻方块符合条件，加入队列和结果集</li>
     *   <li>重复直到队列为空或达到限制</li>
     * </ol>
     * 
     * @param level 世界实例
     * @param originPos 起始位置
     * @param originState 起始方块状态
     * @param config 配置
     * @return 收集到的方块位置列表（不包含起始位置）
     */
    private static List<BlockPos> collectBlocks(
            Level level, 
            BlockPos originPos, 
            BlockState originState, 
            MinerConfig config
    ) {
        // 根据配置选择搜索模式
        return switch (config.shapeMode) {
            case CONNECTED -> collectConnectedBlocks(level, originPos, originState, config);
            case CUBE -> collectCubeBlocks(level, originPos, originState, config);
            case COLUMN -> collectColumnBlocks(level, originPos, originState, config);
        };
    }
    
    /**
     * BFS 搜索相连的同类方块
     */
    private static List<BlockPos> collectConnectedBlocks(
            Level level, 
            BlockPos originPos, 
            BlockState originState, 
            MinerConfig config
    ) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        
        // 选择搜索偏移量
        BlockPos[] offsets = config.allowDiagonal ? DIAGONAL_OFFSETS : ORTHOGONAL_OFFSETS;
        
        // 起始位置已被破坏，从相邻位置开始搜索
        visited.add(originPos);
        
        // 将起始位置的相邻方块加入队列
        for (BlockPos offset : offsets) {
            BlockPos neighbor = originPos.offset(offset);
            if (!visited.contains(neighbor)) {
                BlockState neighborState = level.getBlockState(neighbor);
                if (isMatchingBlock(originState, neighborState, config)) {
                    queue.add(neighbor);
                    visited.add(neighbor);
                }
            }
        }
        
        // BFS 搜索
        while (!queue.isEmpty() && result.size() < config.maxBlocks) {
            BlockPos current = queue.poll();
            
            // 检查距离限制
            if (getDistance(originPos, current) > config.maxDistance) {
                continue;
            }
            
            // 添加到结果
            result.add(current);
            
            // 如果已达到最大数量，停止搜索
            if (result.size() >= config.maxBlocks) {
                break;
            }
            
            // 搜索相邻方块
            for (BlockPos offset : offsets) {
                BlockPos neighbor = current.offset(offset);
                
                if (visited.contains(neighbor)) {
                    continue;
                }
                
                // 距离预检查（优化性能）
                if (getDistance(originPos, neighbor) > config.maxDistance) {
                    visited.add(neighbor);
                    continue;
                }
                
                BlockState neighborState = level.getBlockState(neighbor);
                if (isMatchingBlock(originState, neighborState, config)) {
                    queue.add(neighbor);
                }
                visited.add(neighbor);
            }
        }
        
        return result;
    }
    
    /**
     * 在立方体范围内搜索同类方块
     */
    private static List<BlockPos> collectCubeBlocks(
            Level level, 
            BlockPos originPos, 
            BlockState originState, 
            MinerConfig config
    ) {
        List<BlockPos> result = new ArrayList<>();
        int radius = config.maxDistance;
        
        for (int x = -radius; x <= radius && result.size() < config.maxBlocks; x++) {
            for (int y = -radius; y <= radius && result.size() < config.maxBlocks; y++) {
                for (int z = -radius; z <= radius && result.size() < config.maxBlocks; z++) {
                    if (x == 0 && y == 0 && z == 0) continue; // 跳过起始位置
                    
                    BlockPos pos = originPos.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    
                    if (isMatchingBlock(originState, state, config)) {
                        result.add(pos);
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * 在垂直柱状范围内搜索同类方块
     */
    private static List<BlockPos> collectColumnBlocks(
            Level level, 
            BlockPos originPos, 
            BlockState originState, 
            MinerConfig config
    ) {
        List<BlockPos> result = new ArrayList<>();
        
        // 向上搜索
        for (int y = 1; y <= config.maxDistance && result.size() < config.maxBlocks; y++) {
            BlockPos pos = originPos.above(y);
            BlockState state = level.getBlockState(pos);
            
            if (isMatchingBlock(originState, state, config)) {
                result.add(pos);
            } else {
                break; // 遇到不同方块停止
            }
        }
        
        // 向下搜索
        for (int y = 1; y <= config.maxDistance && result.size() < config.maxBlocks; y++) {
            BlockPos pos = originPos.below(y);
            BlockState state = level.getBlockState(pos);
            
            if (isMatchingBlock(originState, state, config)) {
                result.add(pos);
            } else {
                break; // 遇到不同方块停止
            }
        }
        
        return result;
    }
    
    /**
     * 执行实际的方块挖掘操作
     * 
     * @param player 玩家
     * @param level 世界
     * @param blocks 要挖掘的方块列表
     * @param tool 使用的工具
     * @param config 配置
     * @return 挖矿结果
     */
    private static MiningResult mineBlocks(
            ServerPlayer player, 
            Level level, 
            List<BlockPos> blocks, 
            ItemStack tool, 
            MinerConfig config
    ) {
        List<BlockPos> minedBlocks = new ArrayList<>();
        StopReason stopReason = StopReason.COMPLETED;
        int totalExpCollected = 0;
        
        // 如果需要传送掉落物/经验，预先记录区域内的实体
        Set<Integer> existingEntityIds = new HashSet<>();
        if (config.teleportDrops || config.teleportExp) {
            AABB searchArea = calculateSearchArea(blocks);
            if (level instanceof ServerLevel serverLevel) {
                for (ItemEntity entity : serverLevel.getEntitiesOfClass(ItemEntity.class, searchArea)) {
                    existingEntityIds.add(entity.getId());
                }
                for (ExperienceOrb entity : serverLevel.getEntitiesOfClass(ExperienceOrb.class, searchArea)) {
                    existingEntityIds.add(entity.getId());
                }
            }
        }
        
        for (BlockPos pos : blocks) {
            // 检查工具耐久度
            if (config.consumeDurability && config.stopOnLowDurability) {
                if (tool.isDamageableItem()) {
                    int remainingDurability = tool.getMaxDamage() - tool.getDamageValue();
                    if (remainingDurability <= config.preserveDurability) {
                        stopReason = StopReason.LOW_DURABILITY;
                        break;
                    }
                }
            }
            
            // 检查工具是否已损坏
            if (tool.isEmpty()) {
                stopReason = StopReason.TOOL_BROKEN;
                break;
            }
            
            // 检查饥饿值
            if (config.consumeHunger) {
                int foodLevel = player.getFoodData().getFoodLevel();
                if (foodLevel <= config.minHungerLevel) {
                    stopReason = StopReason.LOW_HUNGER;
                    break;
                }
            }
            
            // 检查权限
            BlockState state = level.getBlockState(pos);
            if (!PlatformServices.getInstance().canPlayerBreakBlock(player, level, pos, state)) {
                // 跳过此方块，继续尝试其他方块
                continue;
            }
            
            // 模拟玩家破坏方块
            boolean success = PlatformServices.getInstance().simulateBlockBreak(player, level, pos);
            
            if (success) {
                minedBlocks.add(pos);
                
                // 消耗额外饥饿值
                if (config.consumeHunger && config.hungerMultiplier > 0) {
                    player.getFoodData().addExhaustion(0.005f * config.hungerMultiplier);
                }
            }
        }
        
        // 处理掉落物和经验传送
        if (level instanceof ServerLevel serverLevel && !minedBlocks.isEmpty()) {
            AABB searchArea = calculateSearchArea(minedBlocks);
            
            // 传送掉落物
            if (config.teleportDrops) {
                collectAndTeleportDrops(serverLevel, player, searchArea, existingEntityIds);
            }
            
            // 传送经验
            if (config.teleportExp) {
                totalExpCollected = collectAndTeleportExp(serverLevel, player, searchArea, existingEntityIds);
            }
        }
        
        return new MiningResult(minedBlocks, minedBlocks.size(), stopReason, totalExpCollected);
    }
    
    /**
     * 计算方块列表的搜索区域
     */
    private static AABB calculateSearchArea(List<BlockPos> blocks) {
        if (blocks.isEmpty()) {
            return new AABB(0, 0, 0, 0, 0, 0);
        }
        
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        
        for (BlockPos pos : blocks) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        
        // 扩展搜索区域以确保捕获所有掉落物
        return new AABB(minX - 2, minY - 2, minZ - 2, maxX + 3, maxY + 3, maxZ + 3);
    }
    
    /**
     * 收集并传送掉落物到玩家背包
     */
    private static List<ItemStack> collectAndTeleportDrops(
            ServerLevel level, 
            ServerPlayer player, 
            AABB area, 
            Set<Integer> existingEntityIds
    ) {
        List<ItemStack> collectedDrops = new ArrayList<>();
        List<ItemEntity> newItems = level.getEntitiesOfClass(ItemEntity.class, area, 
                entity -> !existingEntityIds.contains(entity.getId()) && entity.isAlive());
        
        for (ItemEntity itemEntity : newItems) {
            ItemStack stack = itemEntity.getItem().copy();
            collectedDrops.add(stack.copy());
            
            // 尝试添加到玩家背包
            if (player.getInventory().add(stack)) {
                // 成功添加，移除实体
                itemEntity.discard();
            } else {
                // 背包满了，将物品传送到玩家位置
                itemEntity.teleportTo(player.getX(), player.getY(), player.getZ());
            }
        }
        return collectedDrops;
    }
    
    /**
     * 收集并传送经验到玩家
     * 
     * @return 收集的总经验值
     */
    private static int collectAndTeleportExp(
            ServerLevel level, 
            ServerPlayer player, 
            AABB area, 
            Set<Integer> existingEntityIds
    ) {
        List<ExperienceOrb> newOrbs = level.getEntitiesOfClass(ExperienceOrb.class, area,
                entity -> !existingEntityIds.contains(entity.getId()) && entity.isAlive());
        
        int totalExp = 0;
        for (ExperienceOrb orb : newOrbs) {
            totalExp += orb.getValue();
            orb.discard();
        }
        
        // 直接给予玩家经验
        if (totalExp > 0) {
            player.giveExperiencePoints(totalExp);
        }
        
        return totalExp;
    }
    
    /**
     * 检查激活条件
     * <p>始终使用按住按键激活模式</p>
     */
    private static boolean checkActivation(ServerPlayer player, MinerConfig config) {
        // 只检查按键按住状态
        return MiningStateManager.isHoldingKey(player);
    }
    
    /**
     * 检查方块是否匹配
     */
    private static boolean isMatchingBlock(BlockState origin, BlockState target, MinerConfig config) {
        if (target.isAir()) {
            return false;
        }
        
        Block originBlock = origin.getBlock();
        Block targetBlock = target.getBlock();
        
        // 检查是否在黑名单
        if (OneKeyMinerAPI.isBlockBlacklisted(targetBlock)) {
            return false;
        }
        
        // 检查是否在白名单
        if (!OneKeyMinerAPI.isBlockAllowed(targetBlock)) {
            return false;
        }
        
        if (config.requireExactMatch) {
            // 严格匹配：必须是完全相同的方块
            return originBlock == targetBlock;
        } else {
            // 宽松匹配：同一类型或同标签即可
            return originBlock == targetBlock || OneKeyMinerAPI.areBlocksInSameGroup(originBlock, targetBlock);
        }
    }
    
    /**
     * 计算两个位置之间的切比雪夫距离
     */
    private static int getDistance(BlockPos a, BlockPos b) {
        return Math.max(
                Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY())),
                Math.abs(a.getZ() - b.getZ())
        );
    }
    
    /**
     * 发送挖矿统计信息给玩家
     */
    private static void sendMiningStats(ServerPlayer player, MiningResult result) {
        // 使用 Action Bar 显示统计
        StringBuilder message = new StringBuilder();
        message.append("§a连锁挖矿: §e").append(result.totalMined()).append(" §a个方块");
        
        if (result.expCollected() > 0) {
            message.append(" §7| §b+").append(result.expCollected()).append(" XP");
        }
        
        if (result.stopReason() != StopReason.COMPLETED) {
            message.append(" §7(").append(result.stopReason().getMessage()).append(")");
        }
        
        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(message.toString()), 
                true // Action Bar
        );
    }
    
    /**
     * 挖矿结果记录
     * 
     * @param minedBlocks 成功挖掘的方块位置列表
     * @param totalMined 总共挖掘的方块数量
     * @param stopReason 停止挖矿的原因
     * @param expCollected 收集的经验值（如果启用了经验传送）
     */
    public record MiningResult(List<BlockPos> minedBlocks, int totalMined, StopReason stopReason, int expCollected) {
        /** 兼容旧代码的构造函数 */
        public MiningResult(List<BlockPos> minedBlocks, int totalMined, StopReason stopReason) {
            this(minedBlocks, totalMined, stopReason, 0);
        }
    }
    
    /**
     * 停止挖矿的原因枚举
     */
    public enum StopReason {
        /** 正常完成 */
        COMPLETED("完成"),
        /** 达到最大方块数 */
        MAX_BLOCKS("已达上限"),
        /** 工具耐久度不足 */
        LOW_DURABILITY("耐久不足"),
        /** 工具已损坏 */
        TOOL_BROKEN("工具损坏"),
        /** 饥饿值不足 */
        LOW_HUNGER("饥饿不足"),
        /** 被事件取消 */
        CANCELLED("被取消");
        
        private final String message;
        
        StopReason(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
