package org.xiyu.onekeyminer.chain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.api.OneKeyMinerAPI;
import org.xiyu.onekeyminer.api.event.ChainEvents;
import org.xiyu.onekeyminer.api.event.PostActionEvent;
import org.xiyu.onekeyminer.api.event.PreActionEvent;
import org.xiyu.onekeyminer.chain.ChainActionResult.StopReason;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.MinerConfig;
import org.xiyu.onekeyminer.mining.MiningStateManager;
import org.xiyu.onekeyminer.platform.PlatformServices;
import org.xiyu.onekeyminer.registry.TagResolver;

import java.util.*;


/**
 * 通用链式操作逻辑处理器
 * 
 * <p>实现基于 BFS（广度优先搜索）的链式操作算法，支持三种操作类型：</p>
 * <ul>
 *   <li><strong>连锁挖掘</strong> - 破坏相连的同类型方块</li>
 *   <li><strong>连锁交互</strong> - 对相邻目标执行右键交互（剪羊毛、耕地、剥皮等）</li>
 *   <li><strong>连锁种植</strong> - 批量种植作物/树苗</li>
 * </ul>
 * 
 * <h2>设计原则</h2>
 * <ul>
 *   <li>使用队列实现 BFS，避免递归导致的 StackOverflow</li>
 *   <li>严格限制单次操作的最大数量和最大距离</li>
 *   <li>通过 {@link PlatformServices} 模拟原版行为，确保兼容性</li>
 *   <li>使用通用的物品交互事件，自动支持模组工具</li>
 * </ul>
 * 
 * <h2>关键约束</h2>
 * <p><strong>禁止</strong>使用 {@code world.setBlock(pos, Blocks.AIR.defaultBlockState())}，
 * 必须通过 {@link PlatformServices#simulateBlockBreak} 模拟玩家破坏。</p>
 * 
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.21.9
 */
public final class ChainActionLogic {
    
    // ==================== 搜索偏移量常量 ====================
    
    /** 6 向搜索偏移量（正交方向：上下左右前后） */
    private static final BlockPos[] ORTHOGONAL_OFFSETS = {
            new BlockPos(1, 0, 0),   // 东
            new BlockPos(-1, 0, 0),  // 西
            new BlockPos(0, 1, 0),   // 上
            new BlockPos(0, -1, 0),  // 下
            new BlockPos(0, 0, 1),   // 南
            new BlockPos(0, 0, -1)   // 北
    };
    
    /** 26 向搜索偏移量（含对角线） */
    private static final BlockPos[] DIAGONAL_OFFSETS;
    
    static {
        // 生成 26 向偏移量（排除原点 0,0,0）
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
    
    /** 防止重入的线程本地标记 */
    private static final ThreadLocal<Boolean> IS_PROCESSING = ThreadLocal.withInitial(() -> false);
    
    /** 操作超时时间（毫秒） */
    private static final long OPERATION_TIMEOUT_MS = 2000;
    
    /** 最大迭代次数（防止无限循环） */
    private static final int MAX_ITERATIONS = 10000;
    
    private ChainActionLogic() {
        // 工具类，禁止实例化
    }
    
    // ==================== 公共入口方法 ====================
    
    /**
     * 执行链式操作
     * 
     * <p>根据上下文中的操作类型自动选择对应的处理逻辑。</p>
     * 
     * @param context 操作上下文
     * @return 操作结果
     */
    public static ChainActionResult execute(ChainActionContext context) {
        // 防止重入
        if (IS_PROCESSING.get()) {
            return ChainActionResult.cancelled(context.getActionType(), StopReason.ERROR);
        }
        
        try {
            IS_PROCESSING.set(true);
            
            // 根据操作类型分发处理
            return switch (context.getActionType()) {
                case MINING -> executeMining(context);
                case INTERACTION -> executeInteraction(context);
                case PLANTING -> executePlanting(context);
                case HARVESTING -> executeHarvesting(context);
            };
            
        } catch (Exception e) {
            OneKeyMiner.LOGGER.error("链式操作执行失败: {}", e.getMessage(), e);
            return ChainActionResult.cancelled(context.getActionType(), StopReason.ERROR);
        } finally {
            IS_PROCESSING.set(false);
        }
    }
    
    /**
     * 处理方块破坏事件，触发连锁挖掘
     * 
     * <p>便捷方法，用于在事件处理器中快速调用。</p>
     * 
     * @param player 玩家
     * @param level 世界
     * @param pos 位置
     * @param state 方块状态
     * @return 操作结果
     */
    public static ChainActionResult onBlockBreak(
            ServerPlayer player,
            Level level,
            BlockPos pos,
            BlockState state
    ) {
        ChainActionContext context = ChainActionContext.forMining(player, level, pos, state);
        return execute(context);
    }
    
    /**
     * 处理右键交互事件，触发连锁交互
     * 
     * @param player 玩家
     * @param level 世界
     * @param pos 位置
     * @param hand 交互手
     * @return 操作结果
     */
    public static ChainActionResult onBlockInteraction(
            ServerPlayer player,
            Level level,
            BlockPos pos,
            InteractionHand hand
    ) {
        BlockState state = level.getBlockState(pos);
        ChainActionContext context = ChainActionContext.forInteraction(player, level, pos, state, hand);
        return execute(context);
    }

    /**
     * 处理实体交互事件，触发连锁交互（主要用于剪羊毛）
     *
     * @param player 玩家
     * @param level 世界
     * @param entity 目标实体
     * @param hand 交互手
     * @return 操作结果
     */
    public static ChainActionResult onEntityInteraction(
            ServerPlayer player,
            Level level,
            Entity entity,
            InteractionHand hand
    ) {
        if (entity == null) {
            return ChainActionResult.cancelled(ChainActionType.INTERACTION, StopReason.ERROR);
        }

        BlockPos originPos = entity.blockPosition();
        ChainActionContext context = ChainActionContext.builder()
                .player(player)
                .level(level)
                .originPos(originPos)
                .originState(level.getBlockState(originPos))
                .actionType(ChainActionType.INTERACTION)
                .heldItem(player.getItemInHand(hand))
                .hand(hand)
                .interactionOverride(ChainActionContext.InteractionOverride.SHEARING)
                .build();
        return execute(context);
    }
    
    // ==================== 连锁挖掘逻辑 ====================
    
    /**
     * 执行连锁挖掘
     */
    private static ChainActionResult executeMining(ChainActionContext context) {
        MinerConfig config = ConfigManager.getConfig();
        
        // 检查模组是否启用
        if (!config.enabled) {
            return ChainActionResult.cancelled(ChainActionType.MINING, StopReason.EVENT_CANCELLED);
        }
        
        // 检查激活条件
        if (!checkActivationConditions(context, config)) {
            return ChainActionResult.cancelled(ChainActionType.MINING, StopReason.EVENT_CANCELLED);
        }
        
        // 检查方块是否在白名单中（允许自定义工具规则绕过）
        boolean allowedByRule = OneKeyMinerAPI.findToolActionForBlock(
                context.getHeldItem(),
                context.getOriginState()
        ).map(rule -> rule.actionType() == ChainActionType.MINING).orElse(false);
        if (!OneKeyMinerAPI.isBlockAllowed(context.getOriginState().getBlock()) && !allowedByRule) {
            return ChainActionResult.cancelled(ChainActionType.MINING, StopReason.EVENT_CANCELLED);
        }
        
        // 检查工具是否允许（允许自定义工具规则绕过）
        if (!OneKeyMinerAPI.isToolAllowed(context.getHeldItem()) &&
                !OneKeyMinerAPI.hasToolActionRule(context.getHeldItem(), ChainActionType.MINING)) {
            return ChainActionResult.cancelled(ChainActionType.MINING, StopReason.EVENT_CANCELLED);
        }
        
        // 检查工具挖掘等级是否足够（方块需要正确工具才掉落物品时检查）
        // 例如：石镐无法让钻石矿掉落，此时不应触发连锁挖掘
        // 注意：如果允许空手且玩家空手，则跳过此检查（允许空手挖掘不需要特殊工具的方块）
        if (!context.getHeldItem().isEmpty() && !canToolHarvestBlock(context.getHeldItem(), context.getOriginState())) {
            // 工具挖掘等级不足，不触发连锁，但放行原事件
            return ChainActionResult.cancelled(ChainActionType.MINING, StopReason.EVENT_CANCELLED);
        }
        
        // 收集要挖掘的方块
        List<BlockPos> blocksToMine = collectMiningBlocks(context, config);
        
        if (blocksToMine.isEmpty()) {
            return ChainActionResult.cancelled(ChainActionType.MINING, StopReason.COMPLETED);
        }
        
        // 触发 PreActionEvent
        PreActionEvent preEvent = new PreActionEvent(
                context.getPlayer(),
                context.getLevel(),
                context.getOriginPos(),
                blocksToMine,
                context.getHeldItem(),
                ChainActionType.MINING
        );
        ChainEvents.firePreActionEvent(preEvent);
        
        if (preEvent.isCancelled()) {
            OneKeyMiner.LOGGER.debug("连锁挖掘被 PreActionEvent 取消");
            return ChainActionResult.cancelled(ChainActionType.MINING, StopReason.EVENT_CANCELLED);
        }
        
        // 获取可能被修改的方块列表
        List<BlockPos> finalBlocks = preEvent.getTargetPositions();
        
        // 执行挖掘
        return performMining(context, finalBlocks, config);
    }
    
    /**
     * 使用 BFS 收集相连的同类方块
     */
    private static List<BlockPos> collectMiningBlocks(ChainActionContext context, MinerConfig config) {
        int maxBlocks = context.getMaxCount() > 0 ? context.getMaxCount()
                : (context.isCreativeMode() ? config.maxBlocksCreative : config.maxBlocks);
        int maxDistance = context.getMaxDistance() > 0 ? context.getMaxDistance() : config.maxDistance;
        boolean allowDiagonal = context.isAllowDiagonal() && config.allowDiagonal;

        return switch (config.shapeMode) {
            case CONNECTED -> collectConnectedMiningBlocks(context, config, maxBlocks, maxDistance, allowDiagonal);
            case CUBE -> collectCubeMiningBlocks(context, config, maxBlocks, maxDistance);
            case COLUMN -> collectColumnMiningBlocks(context, config, maxBlocks, maxDistance);
        };
    }

    /**
     * BFS 收集相连方块（连通模式）
     */
    private static List<BlockPos> collectConnectedMiningBlocks(
            ChainActionContext context,
            MinerConfig config,
            int maxBlocks,
            int maxDistance,
            boolean allowDiagonal
    ) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        BlockPos originPos = context.getOriginPos();
        BlockState originState = context.getOriginState();
        Level level = context.getLevel();

        BlockPos[] offsets = allowDiagonal ? DIAGONAL_OFFSETS : ORTHOGONAL_OFFSETS;

        // 起始位置已被破坏，从相邻位置开始搜索
        visited.add(originPos);

        for (BlockPos offset : offsets) {
            BlockPos neighbor = originPos.offset(offset);
            if (!visited.contains(neighbor)) {
                BlockState neighborState = level.getBlockState(neighbor);
                if (isMatchingMiningBlock(originState, neighborState, config)) {
                    queue.add(neighbor);
                    visited.add(neighbor);
                }
            }
        }

        long startTime = System.currentTimeMillis();
        int iterations = 0;

        while (!queue.isEmpty() && result.size() < maxBlocks && iterations < MAX_ITERATIONS) {
            if (System.currentTimeMillis() - startTime > OPERATION_TIMEOUT_MS) {
                OneKeyMiner.LOGGER.warn("连锁挖掘收集超时，已收集 {} 个方块", result.size());
                break;
            }

            iterations++;
            BlockPos current = queue.poll();

            if (current.distManhattan(originPos) > maxDistance) {
                continue;
            }

            result.add(current);

            for (BlockPos offset : offsets) {
                BlockPos neighbor = current.offset(offset);
                if (!visited.contains(neighbor) && neighbor.distManhattan(originPos) <= maxDistance) {
                    BlockState neighborState = level.getBlockState(neighbor);
                    if (isMatchingMiningBlock(originState, neighborState, config)) {
                        queue.add(neighbor);
                        visited.add(neighbor);
                    }
                }
            }
        }

        return result;
    }

    /**
     * 立方体范围收集（CUBE 模式）
     */
    private static List<BlockPos> collectCubeMiningBlocks(
            ChainActionContext context,
            MinerConfig config,
            int maxBlocks,
            int maxDistance
    ) {
        List<BlockPos> result = new ArrayList<>();
        BlockPos originPos = context.getOriginPos();
        BlockState originState = context.getOriginState();
        Level level = context.getLevel();

        int radius = maxDistance;
        for (int x = -radius; x <= radius && result.size() < maxBlocks; x++) {
            for (int y = -radius; y <= radius && result.size() < maxBlocks; y++) {
                for (int z = -radius; z <= radius && result.size() < maxBlocks; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }

                    BlockPos pos = originPos.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (isMatchingMiningBlock(originState, state, config)) {
                        result.add(pos);
                    }
                }
            }
        }

        return result;
    }

    /**
     * 垂直柱状范围收集（COLUMN 模式）
     */
    private static List<BlockPos> collectColumnMiningBlocks(
            ChainActionContext context,
            MinerConfig config,
            int maxBlocks,
            int maxDistance
    ) {
        List<BlockPos> result = new ArrayList<>();
        BlockPos originPos = context.getOriginPos();
        BlockState originState = context.getOriginState();
        Level level = context.getLevel();

        for (int y = 1; y <= maxDistance && result.size() < maxBlocks; y++) {
            BlockPos pos = originPos.above(y);
            BlockState state = level.getBlockState(pos);
            if (isMatchingMiningBlock(originState, state, config)) {
                result.add(pos);
            } else {
                break;
            }
        }

        for (int y = 1; y <= maxDistance && result.size() < maxBlocks; y++) {
            BlockPos pos = originPos.below(y);
            BlockState state = level.getBlockState(pos);
            if (isMatchingMiningBlock(originState, state, config)) {
                result.add(pos);
            } else {
                break;
            }
        }

        return result;
    }
    
    /**
     * 执行实际的挖掘操作
     */
    private static ChainActionResult performMining(
            ChainActionContext context,
            List<BlockPos> blocks,
            MinerConfig config
    ) {
        ServerPlayer player = context.getPlayer();
        Level level = context.getLevel();
        ItemStack tool = context.getHeldItem();
        boolean hasTool = !tool.isEmpty();
        float hungerPerBlock = config.hungerPerBlock * Math.max(0f, config.hungerMultiplier);

        ServerLevel serverLevel = level instanceof ServerLevel sl ? sl : null;
        Set<Integer> existingEntityIds = new HashSet<>();
        if (serverLevel != null && (config.teleportDrops || config.teleportExp)) {
            AABB searchArea = calculateSearchArea(blocks);
            for (ItemEntity entity : serverLevel.getEntitiesOfClass(ItemEntity.class, searchArea)) {
                existingEntityIds.add(entity.getId());
            }
            for (ExperienceOrb entity : serverLevel.getEntitiesOfClass(ExperienceOrb.class, searchArea)) {
                existingEntityIds.add(entity.getId());
            }
        }
        
        List<BlockPos> minedBlocks = new ArrayList<>();
        int durabilityUsed = 0;
        float hungerUsed = 0f;
        StopReason stopReason = StopReason.COMPLETED;
        
        for (BlockPos pos : blocks) {
            // 工具耐久检查
            if (config.consumeDurability && config.stopOnLowDurability && !context.isCreativeMode()) {
                if (tool.isDamageableItem()) {
                    int remaining = tool.getMaxDamage() - tool.getDamageValue();
                    if (remaining <= config.preserveDurability) {
                        stopReason = StopReason.TOOL_DURABILITY_LOW;
                        break;
                    }
                }
            }
            
            // 饥饿值检查
            if (config.consumeHunger && !context.isCreativeMode()) {
                if (player.getFoodData().getFoodLevel() <= config.minHungerLevel) {
                    stopReason = StopReason.HUNGER_LOW;
                    break;
                }
            }
            
            // 权限检查
            if (!context.isSkipPermissionCheck()) {
                if (!PlatformServices.getInstance().canPlayerBreakBlock(player, level, pos, level.getBlockState(pos))) {
                    continue; // 跳过没有权限的方块
                }
            }
            
            // 模拟玩家破坏方块 - 关键！不使用 setBlock
            boolean success = PlatformServices.getInstance().simulateBlockBreak(player, level, pos);
            
            if (success) {
                minedBlocks.add(pos);
                
                if (!context.isCreativeMode()) {
                    durabilityUsed++;
                    if (config.consumeHunger && hungerPerBlock > 0f) {
                        hungerUsed += hungerPerBlock;
                        player.getFoodData().addExhaustion(hungerPerBlock);
                    }
                }
            }
            
            // 检查工具是否损坏
            if (hasTool && tool.isEmpty()) {
                stopReason = StopReason.TOOL_BROKEN;
                break;
            }
        }

        if (serverLevel != null && !minedBlocks.isEmpty()) {
            AABB searchArea = calculateSearchArea(minedBlocks);
            if (config.teleportDrops) {
                collectAndTeleportDrops(serverLevel, player, searchArea, existingEntityIds);
            }
            if (config.teleportExp) {
                collectAndTeleportExp(serverLevel, player, searchArea, existingEntityIds);
            }
        }
        
        // 触发 PostActionEvent
        ChainActionResult result = ChainActionResult.success(
                ChainActionType.MINING,
                minedBlocks,
                durabilityUsed,
                hungerUsed,
                stopReason
        );
        
        PostActionEvent postEvent = new PostActionEvent(
                player,
                level,
                context.getOriginPos(),
                result
        );
        ChainEvents.firePostActionEvent(postEvent);
        
        return result;
    }
    
    /**
     * 检查两个方块状态是否匹配（用于挖掘）
     */
    private static boolean isMatchingMiningBlock(BlockState origin, BlockState target, MinerConfig config) {
        if (target.isAir()) {
            return false;
        }
        
        Block originBlock = origin.getBlock();
        Block targetBlock = target.getBlock();
        
        // 检查是否在黑名单中
        if (OneKeyMinerAPI.isBlockBlacklisted(targetBlock)) {
            return false;
        }
        
        boolean strictMatch = config.strictBlockMatching || config.requireExactMatch;
        if (strictMatch) {
            return originBlock == targetBlock;
        }
        
        // 宽松匹配：同类型或同标签
        if (originBlock == targetBlock) {
            return true;
        }
        
        // 检查是否共享同一个标签
        return OneKeyMinerAPI.blocksShareTag(originBlock, targetBlock);
    }
    
    // ==================== 连锁交互逻辑 ====================
    
    /**
     * 执行连锁交互
     */
    private static ChainActionResult executeInteraction(ChainActionContext context) {
        MinerConfig config = ConfigManager.getConfig();
        
        if (!config.enabled || !config.enableInteraction) {
            return ChainActionResult.cancelled(ChainActionType.INTERACTION, StopReason.EVENT_CANCELLED);
        }
        
        // 检查手持物品是否支持交互
        ItemStack heldItem = context.getHeldItem();
        if (!isInteractionTool(heldItem)) {
            return ChainActionResult.cancelled(ChainActionType.INTERACTION, StopReason.EVENT_CANCELLED);
        }
        
        // 检查激活条件
        if (!checkActivationConditions(context, config)) {
            return ChainActionResult.cancelled(ChainActionType.INTERACTION, StopReason.EVENT_CANCELLED);
        }
        
        // 根据工具类型选择交互目标
        InteractionType interactionType = determineInteractionType(context);
        
        // 收集交互目标
        List<BlockPos> targets = collectInteractionTargets(context, config, interactionType);
        
        if (targets.isEmpty()) {
            return ChainActionResult.cancelled(ChainActionType.INTERACTION, StopReason.COMPLETED);
        }
        
        // 触发 PreActionEvent
        PreActionEvent preEvent = new PreActionEvent(
                context.getPlayer(),
                context.getLevel(),
                context.getOriginPos(),
                targets,
                heldItem,
                ChainActionType.INTERACTION
        );
        ChainEvents.firePreActionEvent(preEvent);
        
        if (preEvent.isCancelled()) {
            return ChainActionResult.cancelled(ChainActionType.INTERACTION, StopReason.EVENT_CANCELLED);
        }
        
        // 执行交互
        return performInteraction(context, preEvent.getTargetPositions(), config, interactionType);
    }
    
    /**
     * 判断物品是否为交互工具
     * 
     * <p>使用通用类型检查，自动支持模组工具。</p>
     */
    private static boolean isInteractionTool(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        
        Item item = stack.getItem();
        
        // 使用物品类型继承检查，而非硬编码
        // 这样可以自动支持所有继承自这些基类的模组工具
        return item instanceof HoeItem ||        // 锄头类（耕地）
               item instanceof AxeItem ||        // 斧头类（剥皮）
               item instanceof ShovelItem ||     // 铲子类（土径）
               item instanceof ShearsItem ||     // 剪刀类（剪羊毛）
               item instanceof BrushItem ||      // 刷子类（刷除）
             OneKeyMinerAPI.isInteractionToolAllowed(stack) || // API 注册的工具
             OneKeyMinerAPI.isInteractiveItemAllowed(stack) || // API 注册的交互物品
             OneKeyMinerAPI.hasToolActionRule(stack, ChainActionType.INTERACTION); // 自定义动作规则
    }
    
    /**
     * 交互类型枚举
     */
    private enum InteractionType {
        SHEARING,     // 剪羊毛
        TILLING,      // 耕地
        STRIPPING,    // 剥皮
        PATH_MAKING,  // 制作土径
        BRUSHING,     // 刷除
        ITEM_USE,     // 物品使用交互
        GENERIC       // 通用右键交互
    }
    
    /**
     * 根据工具类型确定交互类型
     */
    private static InteractionType determineInteractionType(ItemStack stack) {
        Item item = stack.getItem();
        
        if (item instanceof ShearsItem) {
            return InteractionType.SHEARING;
        } else if (item instanceof HoeItem) {
            return InteractionType.TILLING;
        } else if (item instanceof AxeItem) {
            return InteractionType.STRIPPING;
        } else if (item instanceof ShovelItem) {
            return InteractionType.PATH_MAKING;
        } else if (item instanceof BrushItem) {
            return InteractionType.BRUSHING;
        } else if (OneKeyMinerAPI.isInteractiveItemAllowed(stack)) {
            return InteractionType.ITEM_USE;
        }
        
        return InteractionType.GENERIC;
    }

    private static InteractionType determineInteractionType(ChainActionContext context) {
        ChainActionContext.InteractionOverride override = context.getInteractionOverride();
        if (override != null) {
            return switch (override) {
                case SHEARING -> InteractionType.SHEARING;
                case TILLING -> InteractionType.TILLING;
                case STRIPPING -> InteractionType.STRIPPING;
                case PATH_MAKING -> InteractionType.PATH_MAKING;
                case BRUSHING -> InteractionType.BRUSHING;
                case ITEM_USE -> InteractionType.ITEM_USE;
                case GENERIC -> InteractionType.GENERIC;
            };
        }
        return determineInteractionType(context.getHeldItem());
    }

    public static ChainActionContext.InteractionOverride mapInteractionOverride(OneKeyMinerAPI.InteractionRule rule) {
        if (rule == null) {
            return null;
        }
        return switch (rule) {
            case SHEARING -> ChainActionContext.InteractionOverride.SHEARING;
            case TILLING -> ChainActionContext.InteractionOverride.TILLING;
            case STRIPPING -> ChainActionContext.InteractionOverride.STRIPPING;
            case PATH_MAKING -> ChainActionContext.InteractionOverride.PATH_MAKING;
            case BRUSHING -> ChainActionContext.InteractionOverride.BRUSHING;
            case GENERIC -> ChainActionContext.InteractionOverride.GENERIC;
        };
    }

    /**
     * 检查工具是否可以与目标方块触发连锁交互
     */
    public static boolean isValidInteractionTarget(ItemStack stack, BlockState targetState) {
        if (stack.isEmpty()) {
            return false;
        }

        if (!isInteractionTool(stack)) {
            return false;
        }

        OneKeyMinerAPI.ToolActionRule customRule = OneKeyMinerAPI.findToolActionForBlock(stack, targetState).orElse(null);
        if (customRule != null) {
            return customRule.actionType() == ChainActionType.INTERACTION;
        }

        InteractionType type = determineInteractionType(stack);
        return switch (type) {
            case SHEARING -> false; // 剪羊毛为实体交互，不在方块交互中触发
            case TILLING -> canTill(targetState);
            case STRIPPING -> canStrip(targetState);
            case PATH_MAKING -> canMakePath(targetState);
            case BRUSHING -> canBrush(targetState);
            case ITEM_USE -> canItemUseOnBlock(null, targetState);
            case GENERIC -> true;
        };
    }
    
    /**
     * 收集交互目标
     */
    private static List<BlockPos> collectInteractionTargets(
            ChainActionContext context,
            MinerConfig config,
            InteractionType interactionType
    ) {
        // 剪羊毛需要特殊处理（搜索实体而非方块）
        if (interactionType == InteractionType.SHEARING) {
            return collectShearingTargets(context, config);
        }
        
        // 其他交互类型：搜索相邻的可交互方块
        return collectBlockInteractionTargets(context, config, interactionType);
    }
    
    /**
     * 收集剪羊毛目标（方块位置，实际表示可剪实体的位置）
     */
    private static List<BlockPos> collectShearingTargets(ChainActionContext context, MinerConfig config) {
        List<BlockPos> result = new ArrayList<>();
        ServerPlayer player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos originPos = context.getOriginPos();
        
        int maxBlocks = context.isCreativeMode() ? config.maxBlocksCreative : config.maxBlocks;
        int searchRadius = context.getMaxDistance() > 0 ? context.getMaxDistance() : config.maxDistance;
        
        // 搜索范围内的所有可剪切实体
        AABB searchBox = new AABB(originPos).inflate(searchRadius);
        List<Entity> shearables = level.getEntities((Entity) null, searchBox, 
                entity -> entity instanceof Shearable shearable && 
                          shearable.readyForShearing() && 
                          entity.isAlive());
        
        // 按距离排序，优先处理近的
        shearables.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        
        // 限制数量
        int count = Math.min(shearables.size(), maxBlocks);
        for (int i = 0; i < count; i++) {
            result.add(shearables.get(i).blockPosition());
        }
        
        return result;
    }
    
    /**
     * 收集方块交互目标
     */
    private static List<BlockPos> collectBlockInteractionTargets(
            ChainActionContext context,
            MinerConfig config,
            InteractionType interactionType
    ) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        
        BlockPos originPos = context.getOriginPos();
        BlockState originState = context.getOriginState();
        Level level = context.getLevel();
        
        int maxBlocks = context.isCreativeMode() ? config.maxBlocksCreative : config.maxBlocks;
        int maxDistance = config.maxDistance;
        boolean allowDiagonal = context.isAllowDiagonal() && config.allowDiagonal;
        
        BlockPos[] offsets = allowDiagonal ? DIAGONAL_OFFSETS : ORTHOGONAL_OFFSETS;
        
        // 从起始位置开始（包括起始位置）
        queue.add(originPos);
        visited.add(originPos);
        
        long startTime = System.currentTimeMillis();
        int iterations = 0;
        
        while (!queue.isEmpty() && result.size() < maxBlocks && iterations < MAX_ITERATIONS) {
            if (System.currentTimeMillis() - startTime > OPERATION_TIMEOUT_MS) {
                break;
            }
            
            iterations++;
            BlockPos current = queue.poll();
            
            if (current.distManhattan(originPos) > maxDistance) {
                continue;
            }
            
            BlockState currentState = level.getBlockState(current);
            
            // 检查该位置是否可交互
            if (canInteractAt(level, current, currentState, interactionType, originState)) {
                result.add(current);
            }
            
            // 添加相邻方块到队列
            for (BlockPos offset : offsets) {
                BlockPos neighbor = current.offset(offset);
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 检查指定位置是否可以进行交互
     */
    private static boolean canInteractAt(
            Level level,
            BlockPos pos,
            BlockState state,
            InteractionType interactionType,
            BlockState originState
    ) {
        if (state.isAir()) {
            return false;
        }
        
        // 根据交互类型检查方块是否匹配
        return switch (interactionType) {
            case TILLING -> canTill(state);
            case STRIPPING -> canStrip(state);
            case PATH_MAKING -> canMakePath(state);
            case BRUSHING -> canBrush(state);
            case ITEM_USE -> canItemUseOnBlock(null, state);
            case GENERIC -> state.getBlock() == originState.getBlock();
            default -> false;
        };
    }
    
    /**
     * 检查方块是否可以耕地
     */
    private static boolean canTill(BlockState state) {
        // 检查是否在可耕地标签中
        return TagResolver.matchesBlock(state.getBlock(), "#minecraft:dirt") ||
               TagResolver.matchesBlock(state.getBlock(), "#c:tillable");
    }
    
    /**
     * 检查方块是否可以剥皮
     */
    private static boolean canStrip(BlockState state) {
        return TagResolver.matchesBlock(state.getBlock(), "#minecraft:logs");
    }
    
    /**
     * 检查方块是否可以制作土径
     */
    private static boolean canMakePath(BlockState state) {
        return TagResolver.matchesBlock(state.getBlock(), "#minecraft:dirt");
    }
    
    /**
     * 检查方块是否可以刷除
     */
    private static boolean canBrush(BlockState state) {
        return TagResolver.matchesBlock(state.getBlock(), "#minecraft:brushable") ||
               TagResolver.matchesBlock(state.getBlock(), "#minecraft:suspicious_blocks");
    }
    
    /**
     * 检查物品是否可以对方块进行使用交互
     *
     * @param stack 物品栈（可为 null，此时只检查方块是否为非空气）
     * @param state 目标方块状态
     * @return 如果可以交互返回 true
     */
    public static boolean canItemUseOnBlock(ItemStack stack, BlockState state) {
        if (state.isAir()) {
            return false;
        }
        // 如果有 API 注册的验证器，优先使用
        if (stack != null && !OneKeyMinerAPI.validateInteraction(stack, state)) {
            return false;
        }
        // 默认对非空气方块允许交互
        return true;
    }
    
    /**
     * 执行交互操作
     */
    private static ChainActionResult performInteraction(
            ChainActionContext context,
            List<BlockPos> targets,
            MinerConfig config,
            InteractionType interactionType
    ) {
        // 剪羊毛需要特殊处理
        if (interactionType == InteractionType.SHEARING) {
            return performShearing(context, targets, config);
        }
        
        return performBlockInteraction(context, targets, config);
    }
    
    /**
     * 执行剪羊毛操作
     */
    private static ChainActionResult performShearing(
            ChainActionContext context,
            List<BlockPos> entityPositions,
            MinerConfig config
    ) {
        ServerPlayer player = context.getPlayer();
        Level level = context.getLevel();
        ItemStack shears = context.getHeldItem();
        
        List<BlockPos> shearedPositions = new ArrayList<>();
        int durabilityUsed = 0;
        StopReason stopReason = StopReason.COMPLETED;
        
        for (BlockPos pos : entityPositions) {
            // 查找该位置的可剪切实体
            AABB searchBox = new AABB(pos).inflate(1.5);
            List<Entity> nearbyShearables = level.getEntities((Entity) null, searchBox,
                    entity -> entity instanceof Shearable shearable && 
                              shearable.readyForShearing() && 
                              entity.isAlive());
            
            if (nearbyShearables.isEmpty()) {
                continue;
            }
            
            Entity target = nearbyShearables.get(0);
            Shearable shearable = (Shearable) target;
            
            // 工具耐久检查
            if (config.consumeDurability && config.stopOnLowDurability && !context.isCreativeMode()) {
                if (shears.isDamageableItem()) {
                    int remaining = shears.getMaxDamage() - shears.getDamageValue();
                    if (remaining <= config.preserveDurability) {
                        stopReason = StopReason.TOOL_DURABILITY_LOW;
                        break;
                    }
                }
            }
            
            // 使用 Shearable 接口的 shear 方法
            // 在 1.21.9 中签名为: shear(ServerLevel, SoundSource, ItemStack)
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                shearable.shear(serverLevel, net.minecraft.sounds.SoundSource.PLAYERS, shears);
            }
            shearedPositions.add(pos);
            
            if (!context.isCreativeMode()) {
                durabilityUsed++;
            }
            
            if (shears.isEmpty()) {
                stopReason = StopReason.TOOL_BROKEN;
                break;
            }
        }
        
        ChainActionResult result = ChainActionResult.success(
                ChainActionType.INTERACTION,
                shearedPositions,
                durabilityUsed,
                0f,
                stopReason
        );
        
        // 触发 PostActionEvent
        PostActionEvent postEvent = new PostActionEvent(
                player,
                level,
                context.getOriginPos(),
                result
        );
        ChainEvents.firePostActionEvent(postEvent);
        
        return result;
    }
    
    /**
     * 执行方块交互操作
     */
    private static ChainActionResult performBlockInteraction(
            ChainActionContext context,
            List<BlockPos> targets,
            MinerConfig config
    ) {
        ServerPlayer player = context.getPlayer();
        Level level = context.getLevel();
        ItemStack tool = context.getHeldItem();
        InteractionHand hand = context.getHand();
        
        List<BlockPos> interactedPositions = new ArrayList<>();
        int durabilityUsed = 0;
        StopReason stopReason = StopReason.COMPLETED;
        
        for (BlockPos pos : targets) {
            // 工具耐久检查
            if (config.consumeDurability && config.stopOnLowDurability && !context.isCreativeMode() && tool.isDamageableItem()) {
                int remaining = tool.getMaxDamage() - tool.getDamageValue();
                if (remaining <= config.preserveDurability) {
                    stopReason = StopReason.TOOL_DURABILITY_LOW;
                    break;
                }
            }
            
            // 权限检查
            if (!context.isSkipPermissionCheck()) {
                if (!PlatformServices.getInstance().canPlayerInteract(player, level, pos, level.getBlockState(pos))) {
                    continue;
                }
            }
            
            // 模拟物品使用 - 通用方法，支持模组物品
            boolean success = simulateItemUse(player, level, pos, tool, hand);
            
            if (success) {
                interactedPositions.add(pos);
                if (!context.isCreativeMode()) {
                    durabilityUsed++;
                }
            }
            
            if (tool.isEmpty()) {
                stopReason = StopReason.TOOL_BROKEN;
                break;
            }
        }
        
        ChainActionResult result = ChainActionResult.success(
                ChainActionType.INTERACTION,
                interactedPositions,
                durabilityUsed,
                0f,
                stopReason
        );
        
        PostActionEvent postEvent = new PostActionEvent(
                player,
                level,
                context.getOriginPos(),
                result
        );
        ChainEvents.firePostActionEvent(postEvent);
        
        return result;
    }
    
    /**
     * 模拟物品使用
     * 
     * <p>使用原版的物品交互系统，确保模组兼容性。</p>
     */
    private static boolean simulateItemUse(
            ServerPlayer player,
            Level level,
            BlockPos pos,
            ItemStack item,
            InteractionHand hand
    ) {
        return PlatformServices.getInstance().simulateItemUseOnBlock(player, level, pos, hand, item);
    }

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

        return new AABB(minX - 2, minY - 2, minZ - 2, maxX + 3, maxY + 3, maxZ + 3);
    }

    private static void collectAndTeleportDrops(
            ServerLevel level,
            ServerPlayer player,
            AABB area,
            Set<Integer> existingEntityIds
    ) {
        List<ItemEntity> newItems = level.getEntitiesOfClass(ItemEntity.class, area,
                entity -> !existingEntityIds.contains(entity.getId()) && entity.isAlive());

        for (ItemEntity itemEntity : newItems) {
            ItemStack stack = itemEntity.getItem().copy();
            if (player.getInventory().add(stack)) {
                itemEntity.discard();
            } else {
                itemEntity.teleportTo(player.getX(), player.getY(), player.getZ());
            }
        }
    }

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

        if (totalExp > 0) {
            player.giveExperiencePoints(totalExp);
        }

        return totalExp;
    }
    
    // ==================== 连锁种植逻辑 ====================
    
    /**
     * 执行连锁种植
     */
    private static ChainActionResult executePlanting(ChainActionContext context) {
        MinerConfig config = ConfigManager.getConfig();
        
        if (!config.enabled || !config.enablePlanting) {
            return ChainActionResult.cancelled(ChainActionType.PLANTING, StopReason.EVENT_CANCELLED);
        }
        
        // 检查手持物品是否为种子/树苗
        ItemStack heldItem = context.getHeldItem();
        if (!isPlantableItem(heldItem)) {
            return ChainActionResult.cancelled(ChainActionType.PLANTING, StopReason.EVENT_CANCELLED);
        }
        
        // 检查种子黑名单
        if (OneKeyMinerAPI.isSeedBlacklisted(heldItem.getItem())) {
            return ChainActionResult.cancelled(ChainActionType.PLANTING, StopReason.EVENT_CANCELLED);
        }
        
        // 检查激活条件
        if (!checkActivationConditions(context, config)) {
            return ChainActionResult.cancelled(ChainActionType.PLANTING, StopReason.EVENT_CANCELLED);
        }
        
        // 收集可种植位置
        List<BlockPos> plantablePositions = collectPlantablePositions(context, config);
        
        if (plantablePositions.isEmpty()) {
            return ChainActionResult.cancelled(ChainActionType.PLANTING, StopReason.COMPLETED);
        }
        
        // 触发 PreActionEvent
        PreActionEvent preEvent = new PreActionEvent(
                context.getPlayer(),
                context.getLevel(),
                context.getOriginPos(),
                plantablePositions,
                heldItem,
                ChainActionType.PLANTING
        );
        ChainEvents.firePreActionEvent(preEvent);
        
        if (preEvent.isCancelled()) {
            return ChainActionResult.cancelled(ChainActionType.PLANTING, StopReason.EVENT_CANCELLED);
        }
        
        // 执行种植
        return performPlanting(context, preEvent.getTargetPositions(), config);
    }
    
    /**
     * 检查工具是否能有效挖掘方块（让方块掉落物品）
     * 
     * <p>例如：石镐无法让钻石矿掉落物品，此时不应触发连锁挖掘。</p>
     * <p>如果方块不需要特定工具，或者玩家的工具足够挖掘该方块，则返回 true。</p>
     * 
     * @param tool 玩家手持的工具
     * @param blockState 目标方块状态
     * @return 如果工具能有效挖掘（让方块掉落物品）返回 true
     */
    public static boolean canToolHarvestBlock(ItemStack tool, BlockState blockState) {
        // 如果方块不需要正确工具就能掉落物品，直接返回 true
        if (!blockState.requiresCorrectToolForDrops()) {
            return true;
        }
        
        // 空手挖掘需要正确工具的方块时不会掉落物品
        if (tool.isEmpty()) {
            return false;
        }
        
        // 检查工具是否能正确挖掘此方块
        return tool.isCorrectToolForDrops(blockState);
    }
    
    /**
     * 检查物品是否可种植
     * 
     * <p>使用方块类型继承检查和标签检查，支持原版和模组种子。</p>
     * <p>优先检查方块类型继承关系，支持：</p>
     * <ul>
     *   <li>CropBlock - 作物</li>
     *   <li>SaplingBlock - 树苗</li>
     *   <li>BushBlock - 灌木</li>
     *   <li>FlowerBlock - 花卉</li>
     *   <li>TallFlowerBlock - 高花</li>
     *   <li>CactusBlock - 仙人掌</li>
     *   <li>TallGrassBlock - 高草</li>
     *   <li>FungusBlock - 真菌</li>
     *   <li>RootsBlock - 根茎</li>
     *   <li>NetherSproutsBlock - 下界芽</li>
     *   <li>CocoaBlock - 可可豆</li>
     *   <li>MangroveRootsBlock - 红树根</li>
     *   <li>SugarCaneBlock - 甘蔗</li>
     *   <li>NetherWartBlock - 下界疣</li>
     *   <li>BambooStalkBlock - 竹子</li>
     *   <li>BambooSaplingBlock - 竹笋</li>
     *   <li>AzaleaBlock - 杜鹃花</li>
     * </ul>
     * 
     * @param stack 物品栈
     * @return 如果是可种植物品返回 true
     */
    public static boolean isPlantableItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        
        Item item = stack.getItem();
        
        // 检查是否为方块物品，并检查对应方块的类型
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            
            // 检查方块是否继承自可种植的方块类型
            if (block instanceof CropBlock ||
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
                block instanceof MangroveRootsBlock ||
                block instanceof SugarCaneBlock ||
                block instanceof NetherWartBlock ||
                block instanceof BambooStalkBlock ||
                block instanceof BambooSaplingBlock ||
                block instanceof AzaleaBlock) {
                return true;
            }
        }
        
        // 检查物品标签（作为后备）
        return TagResolver.matchesItem(item, "#c:seeds") ||
               TagResolver.matchesItem(item, "#minecraft:saplings") ||
               TagResolver.matchesItem(item, "#neoforge:seeds") ||
               OneKeyMinerAPI.isPlantableItemAllowed(stack);
    }
    
    /**
     * 收集可种植位置
     */
    private static List<BlockPos> collectPlantablePositions(ChainActionContext context, MinerConfig config) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        
        ServerPlayer player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos originPos = context.getOriginPos();
        ItemStack seedItem = context.getHeldItem();
        
        int maxBlocks = context.isCreativeMode() ? config.maxBlocksCreative : config.maxBlocks;
        int maxDistance = config.maxDistance;
        
        // 计算可用种子数量
        int availableSeeds = context.isCreativeMode() ? Integer.MAX_VALUE 
                : countItemsInInventory(player, seedItem.getItem());
        
        queue.add(originPos);
        visited.add(originPos);
        
        long startTime = System.currentTimeMillis();
        int iterations = 0;
        
        while (!queue.isEmpty() && result.size() < maxBlocks && 
               result.size() < availableSeeds && iterations < MAX_ITERATIONS) {
            
            if (System.currentTimeMillis() - startTime > OPERATION_TIMEOUT_MS) {
                break;
            }
            
            iterations++;
            BlockPos current = queue.poll();
            
            if (current.distManhattan(originPos) > maxDistance) {
                continue;
            }
            
            // 检查该位置是否可以种植
            if (canPlantAt(level, current, seedItem, config)) {
                result.add(current);
            }
            
            // 只在水平方向搜索相邻位置（种植通常是平面的）
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = current.relative(dir);
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 检查是否可以在指定位置种植
     */
    private static boolean canPlantAt(Level level, BlockPos pos, ItemStack seedItem, MinerConfig config) {
        // 位置必须是空气
        if (!level.isEmptyBlock(pos)) {
            return false;
        }
        
        // 检查下方方块是否适合种植
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        
        // 使用原版的种植检查逻辑
        if (seedItem.getItem() instanceof BlockItem blockItem) {
            Block plantBlock = blockItem.getBlock();
            BlockState plantState = plantBlock.defaultBlockState();
            
            // 检查方块是否可以放置在下方方块上
            if (!plantState.canSurvive(level, pos)) {
                return false;
            }

            if (!config.farmlandWhitelist.isEmpty()) {
                return matchesBlockList(belowState, config.farmlandWhitelist);
            }

            return true;
        }
        
        if (!config.farmlandWhitelist.isEmpty()) {
            return matchesBlockList(belowState, config.farmlandWhitelist);
        }

        // 通用检查：耕地或草方块
        return TagResolver.matchesBlock(belowState.getBlock(), "#minecraft:dirt") ||
               TagResolver.matchesBlock(belowState.getBlock(), "#c:farmland");
    }

    private static boolean matchesBlockList(BlockState state, List<String> entries) {
        Block block = state.getBlock();
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            if (entry.startsWith("#")) {
                if (TagResolver.matchesBlock(block, entry)) {
                    return true;
                }
                continue;
            }
            var loc = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            if (loc != null && loc.toString().equals(entry)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 执行种植操作
     */
    private static ChainActionResult performPlanting(
            ChainActionContext context,
            List<BlockPos> positions,
            MinerConfig config
    ) {
        ServerPlayer player = context.getPlayer();
        Level level = context.getLevel();
        ItemStack seedItem = context.getHeldItem();
        InteractionHand hand = context.getHand();
        
        List<BlockPos> plantedPositions = new ArrayList<>();
        StopReason stopReason = StopReason.COMPLETED;
        
        for (BlockPos pos : positions) {
            // 检查是否还有种子
            if (!context.isCreativeMode() && !hasItem(player, seedItem.getItem())) {
                stopReason = StopReason.ITEMS_EXHAUSTED;
                break;
            }
            
            // 权限检查
            if (!context.isSkipPermissionCheck()) {
                if (!PlatformServices.getInstance().canPlayerBreakBlock(player, level, pos, level.getBlockState(pos))) {
                    continue;
                }
            }
            
            // 模拟使用种子
            boolean success = simulateItemUse(player, level, pos.below(), seedItem, hand);
            
            if (success) {
                plantedPositions.add(pos);
            }
        }
        
        ChainActionResult result = ChainActionResult.success(
                ChainActionType.PLANTING,
                plantedPositions,
                0,
                0f,
                stopReason
        );
        
        PostActionEvent postEvent = new PostActionEvent(
                player,
                level,
                context.getOriginPos(),
                result
        );
        ChainEvents.firePostActionEvent(postEvent);
        
        return result;
    }
    
    // ==================== 连锁收割逻辑 ====================
    
    /**
     * 检查方块是否为成熟作物
     * 
     * <p>支持以下作物类型：</p>
     * <ul>
     *   <li>CropBlock - 普通农作物（小麦、胡萝卜、土豆、甜菜等）</li>
     *   <li>NetherWartBlock - 下界疣（age &gt;= 3）</li>
     *   <li>CocoaBlock - 可可豆（age &gt;= 2）</li>
     *   <li>SweetBerryBushBlock - 甜浆果丛（age &gt;= 2）</li>
     * </ul>
     * 
     * @param state 方块状态
     * @return 如果是成熟作物返回 true
     */
    public static boolean isMatureCrop(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock cropBlock) {
            return cropBlock.isMaxAge(state);
        }
        if (block instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= 3;
        }
        if (block instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) >= 2;
        }
        if (block instanceof SweetBerryBushBlock) {
            return state.getValue(SweetBerryBushBlock.AGE) >= 2;
        }
        return false;
    }
    
    /**
     * 执行连锁收割
     */
    private static ChainActionResult executeHarvesting(ChainActionContext context) {
        MinerConfig config = ConfigManager.getConfig();
        
        if (!config.enabled || !config.enableHarvesting) {
            return ChainActionResult.cancelled(ChainActionType.HARVESTING, StopReason.EVENT_CANCELLED);
        }
        
        // 检查起始方块是否为成熟作物
        if (!isMatureCrop(context.getOriginState())) {
            return ChainActionResult.cancelled(ChainActionType.HARVESTING, StopReason.EVENT_CANCELLED);
        }
        
        // 检查激活条件
        if (!checkActivationConditions(context, config)) {
            return ChainActionResult.cancelled(ChainActionType.HARVESTING, StopReason.EVENT_CANCELLED);
        }
        
        // 收集收割目标（同类型成熟作物）
        List<BlockPos> targets = collectHarvestTargets(context, config);
        
        if (targets.isEmpty()) {
            return ChainActionResult.cancelled(ChainActionType.HARVESTING, StopReason.COMPLETED);
        }
        
        // 触发 PreActionEvent
        PreActionEvent preEvent = new PreActionEvent(
                context.getPlayer(),
                context.getLevel(),
                context.getOriginPos(),
                targets,
                context.getHeldItem(),
                ChainActionType.HARVESTING
        );
        ChainEvents.firePreActionEvent(preEvent);
        
        if (preEvent.isCancelled()) {
            return ChainActionResult.cancelled(ChainActionType.HARVESTING, StopReason.EVENT_CANCELLED);
        }
        
        // 执行收割
        return performHarvesting(context, preEvent.getTargetPositions(), config);
    }
    
    /**
     * 收集收割目标
     * 
     * <p>使用水平BFS搜索同类型成熟作物</p>
     */
    private static List<BlockPos> collectHarvestTargets(ChainActionContext context, MinerConfig config) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        
        Level level = context.getLevel();
        BlockPos originPos = context.getOriginPos();
        Block originBlock = context.getOriginState().getBlock();
        
        int maxBlocks = context.isCreativeMode() ? config.maxBlocksCreative : config.maxBlocks;
        int maxDistance = config.maxDistance;
        
        queue.add(originPos);
        visited.add(originPos);
        
        long startTime = System.currentTimeMillis();
        int iterations = 0;
        
        while (!queue.isEmpty() && result.size() < maxBlocks && iterations < MAX_ITERATIONS) {
            if (System.currentTimeMillis() - startTime > OPERATION_TIMEOUT_MS) {
                break;
            }
            
            iterations++;
            BlockPos current = queue.poll();
            
            if (current.distManhattan(originPos) > maxDistance) {
                continue;
            }
            
            BlockState state = level.getBlockState(current);
            if (state.getBlock() == originBlock && isMatureCrop(state)) {
                result.add(current);
                
                // 仅从匹配位置扩展搜索（水平方向）
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockPos neighbor = current.relative(dir);
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * 执行收割操作
     */
    private static ChainActionResult performHarvesting(
            ChainActionContext context,
            List<BlockPos> targets,
            MinerConfig config
    ) {
        ServerPlayer player = context.getPlayer();
        Level level = context.getLevel();
        
        List<BlockPos> harvestedPositions = new ArrayList<>();
        float exhaustion = 0f;
        StopReason stopReason = StopReason.COMPLETED;
        
        for (BlockPos pos : targets) {
            BlockState state = level.getBlockState(pos);
            if (!isMatureCrop(state)) continue;
            
            // 权限检查
            if (!context.isSkipPermissionCheck()) {
                if (!PlatformServices.getInstance().canPlayerBreakBlock(player, level, pos, state)) {
                    continue;
                }
            }
            
            // 获取掉落物（在方块被移除之前）
            List<ItemStack> drops = new ArrayList<>(
                    Block.getDrops(state, (ServerLevel) level, pos, level.getBlockEntity(pos))
            );
            
            // 移除方块（不触发自然掉落）
            level.removeBlock(pos, false);
            
            // 尝试补种
            if (config.harvestReplant) {
                tryReplant(level, pos, state, drops);
            }
            
            // 处理掉落物
            if (config.teleportDrops) {
                for (ItemStack drop : drops) {
                    if (!drop.isEmpty()) {
                        if (!player.getInventory().add(drop)) {
                            Block.popResource(level, player.blockPosition(), drop);
                        }
                    }
                }
            } else {
                for (ItemStack drop : drops) {
                    if (!drop.isEmpty()) {
                        Block.popResource(level, pos, drop);
                    }
                }
            }
            
            harvestedPositions.add(pos);
            
            // 计算饥饿消耗
            if (!context.isCreativeMode()) {
                exhaustion += config.hungerPerBlock;
            }
        }
        
        // 施加饥饿消耗
        if (exhaustion > 0) {
            player.causeFoodExhaustion(exhaustion);
        }
        
        ChainActionResult result = ChainActionResult.success(
                ChainActionType.HARVESTING,
                harvestedPositions,
                0,
                exhaustion,
                stopReason
        );
        
        PostActionEvent postEvent = new PostActionEvent(
                player,
                level,
                context.getOriginPos(),
                result
        );
        ChainEvents.firePostActionEvent(postEvent);
        
        return result;
    }
    
    /**
     * 尝试补种作物
     * 
     * <p>从掉落物中取出一个种子用于补种，并将方块重新放置为初始状态</p>
     */
    private static void tryReplant(Level level, BlockPos pos, BlockState harvestedState, List<ItemStack> drops) {
        Block block = harvestedState.getBlock();
        
        if (block instanceof CropBlock cropBlock) {
            Item seedItem = findSeedForCrop(cropBlock, drops);
            if (seedItem != null && removeItemFromDrops(seedItem, drops)) {
                level.setBlock(pos, cropBlock.defaultBlockState(), Block.UPDATE_ALL);
            }
        } else if (block instanceof NetherWartBlock) {
            if (removeItemFromDrops(Items.NETHER_WART, drops)) {
                level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
            }
        } else if (block instanceof SweetBerryBushBlock) {
            if (removeItemFromDrops(Items.SWEET_BERRIES, drops)) {
                level.setBlock(pos, block.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 1), Block.UPDATE_ALL);
            }
        } else if (block instanceof CocoaBlock) {
            if (removeItemFromDrops(Items.COCOA_BEANS, drops)) {
                level.setBlock(pos, harvestedState.setValue(CocoaBlock.AGE, 0), Block.UPDATE_ALL);
            }
        }
    }
    
    /**
     * 查找作物对应的种子物品
     * 
     * <p>优先检查掉落物中是否有该作物的种子（BlockItem → CropBlock 匹配），
     * 如果找不到则查找任意可种植物品作为后备</p>
     */
    private static Item findSeedForCrop(CropBlock cropBlock, List<ItemStack> drops) {
        // 优先：检查掉落物中是否有直接对应的 BlockItem
        for (ItemStack drop : drops) {
            if (drop.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == cropBlock) {
                return drop.getItem();
            }
        }
        // 后备：查找任意可种植物品
        for (ItemStack drop : drops) {
            if (!drop.isEmpty() && isPlantableItem(drop)) {
                return drop.getItem();
            }
        }
        return null;
    }
    
    /**
     * 从掉落物列表中移除一个指定物品
     * 
     * @return 是否成功移除
     */
    private static boolean removeItemFromDrops(Item item, List<ItemStack> drops) {
        for (int i = 0; i < drops.size(); i++) {
            if (drops.get(i).getItem() == item) {
                drops.get(i).shrink(1);
                if (drops.get(i).isEmpty()) {
                    drops.remove(i);
                }
                return true;
            }
        }
        return false;
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 检查激活条件
     * <p>始终使用按住按键激活模式</p>
     */
    private static boolean checkActivationConditions(ChainActionContext context, MinerConfig config) {
        ServerPlayer player = context.getPlayer();
        
        // 只检查按键按住状态
        return MiningStateManager.isHoldingKey(player);
    }
    
    /**
     * 计算玩家物品栏中指定物品的数量
     */
    private static int countItemsInInventory(ServerPlayer player, Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    /**
     * 检查玩家是否拥有指定物品
     */
    private static boolean hasItem(ServerPlayer player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).getItem() == item) {
                return true;
            }
        }
        return false;
    }
}
