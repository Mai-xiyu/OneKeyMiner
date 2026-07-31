package org.xiyu.onekeyminer.chain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
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
import org.xiyu.onekeyminer.shape.ChainShape;
import org.xiyu.onekeyminer.shape.ShapeContext;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

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

    private static final String FOOD_LEVEL_KEY = "foodLevel";
    private static final String FOOD_SATURATION_KEY = "foodSaturationLevel";
    private static final String FOOD_EXHAUSTION_KEY = "foodExhaustionLevel";

    private ChainActionLogic() {
        // 工具类，禁止实例化
    }

    /**
     * Returns whether the current server thread is already dispatching a
     * derived chain action.
     */
    public static boolean isProcessing() {
        return IS_PROCESSING.get();
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
        Objects.requireNonNull(context, "context");
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
            IS_PROCESSING.remove();
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
        return onBlockBreak(player, level, pos, state, player.getMainHandItem());
    }

    /**
     * Handles a completed original break using the tool snapshot that caused it.
     */
    public static ChainActionResult onBlockBreak(
            ServerPlayer player,
            Level level,
            BlockPos pos,
            BlockState state,
            ItemStack originalTool
    ) {
        ChainActionContext context = ChainActionContext.builder()
                .player(player)
                .level(level)
                .originPos(pos)
                .originState(state)
                .actionType(ChainActionType.MINING)
                .heldItem(originalTool)
                .hand(InteractionHand.MAIN_HAND)
                .build();
        return execute(context);
    }

    /**
     * Loader bridge for a break whose activation state was already observed
     * before a loader-specific deferred after-break check.
     */
    public static ChainActionResult onVerifiedBlockBreak(
            ServerPlayer player,
            Level level,
            BlockPos pos,
            BlockState state,
            ItemStack originalTool
    ) {
        return execute(ChainActionContext.forVerifiedMining(
                player,
                level,
                pos,
                state,
                originalTool
        ));
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
                .originEntityId(entity.getUUID())
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
        OneKeyMinerAPI.ToolActionRule activeMiningRule =
                OneKeyMinerAPI.findToolActionForBlock(
                        context.getHeldItem(),
                        context.getOriginState(),
                        ChainActionType.MINING
                )
                .orElse(null);
        boolean allowedByRule = activeMiningRule != null;
        if (OneKeyMinerAPI.isBlockBlacklisted(
                context.getOriginState().getBlock()
        ) || OneKeyMinerAPI.isToolBlacklisted(context.getHeldItem())) {
            return ChainActionResult.cancelled(
                    ChainActionType.MINING,
                    StopReason.EVENT_CANCELLED
            );
        }
        if (!OneKeyMinerAPI.isBlockAllowed(context.getOriginState().getBlock()) && !allowedByRule) {
            return ChainActionResult.cancelled(ChainActionType.MINING, StopReason.EVENT_CANCELLED);
        }

        // 检查工具是否允许（允许自定义工具规则绕过）
        if (!OneKeyMinerAPI.isToolAllowed(context.getHeldItem()) && !allowedByRule) {
            return ChainActionResult.cancelled(ChainActionType.MINING, StopReason.EVENT_CANCELLED);
        }

        // 检查工具挖掘等级是否足够（方块需要正确工具才掉落物品时检查）
        // 例如：石镐无法让钻石矿掉落，此时不应触发连锁挖掘
        // 注意：如果允许空手且玩家空手，则跳过此检查（允许空手挖掘不需要特殊工具的方块）
        if (!context.isCreativeMode()
                && !context.getHeldItem().isEmpty()
                && !canToolHarvestBlock(
                        context.getHeldItem(),
                        context.getOriginState()
                )) {
            // 工具挖掘等级不足，不触发连锁，但放行原事件
            return ChainActionResult.cancelled(ChainActionType.MINING, StopReason.EVENT_CANCELLED);
        }

        // 收集要挖掘的方块
        List<BlockPos> blocksToMine = sanitizeTargets(
                context,
                collectMiningBlocks(context, config, activeMiningRule),
                config
        );

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
        List<BlockPos> finalBlocks = sanitizeTargets(
                context,
                preEvent.getTargetPositions(),
                getMaxTargetCount(context, config),
                getMaxTargetDistance(context, config),
                pos -> !pos.equals(context.getOriginPos())
                        && isMatchingMiningBlock(
                                context.getOriginState(),
                                context.getLevel().getBlockState(pos),
                                config
                        )
                        && matchesActiveBlockRule(
                                activeMiningRule,
                                context.getHeldItem(),
                                context.getLevel().getBlockState(pos)
                        )
        );

        // 执行挖掘
        return performMining(context, finalBlocks, config, activeMiningRule);
    }

    /**
     * 使用 BFS 收集相连的同类方块
     */
    private static List<BlockPos> collectMiningBlocks(
            ChainActionContext context,
            MinerConfig config,
            OneKeyMinerAPI.ToolActionRule activeMiningRule
    ) {
        int maxBlocks = getMaxTargetCount(context, config);
        int maxDistance = getMaxTargetDistance(context, config);
        boolean allowDiagonal = context.isAllowDiagonal() && config.allowDiagonal;

        ResourceLocation shapeId = MiningStateManager.getPlayerShape(context.getPlayer());
        ChainShape shape = shapeId != null
                ? ShapeRegistry.getShapeOrDefault(shapeId)
                : ShapeRegistry.getShapeOrDefault(config.selectedShape);

        if (shape == null) {
            OneKeyMiner.LOGGER.warn("No chain shape is registered; skipping target collection");
            return Collections.emptyList();
        }

        ServerPlayer player = context.getPlayer();
        ShapeContext.Builder builder = new ShapeContext.Builder()
                .level(context.getLevel())
                .originPos(context.getOriginPos())
                .originState(context.getOriginState())
                .maxBlocks(maxBlocks)
                .maxDistance(maxDistance)
                .allowDiagonal(allowDiagonal)
                .blockMatcher((origin, target) ->
                        isMatchingMiningBlock(origin, target, config)
                                && matchesActiveBlockRule(
                                        activeMiningRule,
                                        context.getHeldItem(),
                                        target
                                )
                );

        if (player != null) {
            builder.playerFacing(player.getDirection());
            float xRot = player.getXRot();
            if (xRot < -45) {
                builder.playerLookingVertical(Direction.UP);
            } else if (xRot > 45) {
                builder.playerLookingVertical(Direction.DOWN);
            }
        }

        return shape.collectBlocks(builder.build());
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
        int iterationBudget = Math.min(
                MAX_ITERATIONS,
                Math.max(256, maxBlocks * (allowDiagonal ? 32 : 8))
        );

        while (!queue.isEmpty() && result.size() < maxBlocks && iterations < iterationBudget) {
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
            MinerConfig config,
            OneKeyMinerAPI.ToolActionRule activeMiningRule
    ) {
        ServerPlayer player = context.getPlayer();
        Level level = context.getLevel();
        InteractionHand hand = context.getHand();
        boolean hasTool = !context.getHeldItem().isEmpty();
        float hungerPerBlock = config.hungerPerBlock * Math.max(0f, config.hungerMultiplier);
        boolean chargesHunger = config.consumeHunger
                && !context.isCreativeMode()
                && hungerPerBlock > 0f;
        boolean teleportDrops = config.isDropTeleportEnabled(
                MiningStateManager.isTeleportDrops(player)
        );
        boolean teleportExp = config.isExperienceTeleportEnabled(
                MiningStateManager.isTeleportExp(player)
        );

        ServerLevel serverLevel = level instanceof ServerLevel sl ? sl : null;
        Set<Integer> existingEntityIds = new HashSet<>();
        Set<Integer> existingExperienceIds = new HashSet<>();
        if (serverLevel != null && (teleportDrops || teleportExp)) {
            AABB searchArea = calculateSearchArea(blocks);
            if (teleportDrops) {
                for (ItemEntity entity : serverLevel.getEntitiesOfClass(ItemEntity.class, searchArea)) {
                    existingEntityIds.add(entity.getId());
                }
            }
            if (teleportExp) {
                for (ExperienceOrb entity : serverLevel.getEntitiesOfClass(ExperienceOrb.class, searchArea)) {
                    existingExperienceIds.add(entity.getId());
                }
            }
        }

        List<BlockPos> minedBlocks = new ArrayList<>();
        int durabilityUsed = 0;
        float hungerUsed = 0f;
        StopReason stopReason = StopReason.COMPLETED;

        for (BlockPos pos : blocks) {
            ItemStack tool = player.getItemInHand(hand);
            BlockState currentState = level.getBlockState(pos);
            if (!isMatchingMiningBlock(context.getOriginState(), currentState, config)
                    || !matchesActiveBlockRule(activeMiningRule, tool, currentState)
                    || (!context.isCreativeMode()
                            && !canToolHarvestBlock(tool, currentState))) {
                continue;
            }
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
            if (chargesHunger
                    && !canConsumeHunger(player, hungerPerBlock, config.minHungerLevel)) {
                stopReason = StopReason.HUNGER_LOW;
                break;
            }

            // The game-mode path is authoritative: loader protection, loot,
            // tool hooks and cancellable break events run exactly once.
            ItemStack toolBefore = tool.copy();
            boolean success = PlatformServices.getInstance().simulateBlockBreak(player, level, pos);
            ItemStack toolAfter = player.getItemInHand(hand);
            if (!config.consumeDurability && !context.isCreativeMode()) {
                restoreDurability(player, hand, toolBefore);
                toolAfter = player.getItemInHand(hand);
            }

            if (success) {
                minedBlocks.add(pos);

                if (!context.isCreativeMode()) {
                    if (config.consumeDurability) {
                        durabilityUsed += calculateDurabilityDelta(toolBefore, toolAfter);
                    }
                    if (chargesHunger
                            && consumeHunger(player, hungerPerBlock, config.minHungerLevel)) {
                        hungerUsed += hungerPerBlock;
                    } else if (chargesHunger) {
                        // A break hook changed hunger after the pre-check.
                        // Keep the completed block, but stop before another.
                        stopReason = StopReason.HUNGER_LOW;
                        break;
                    }
                }
            }

            // 检查工具是否损坏
            if (hasTool && player.getItemInHand(hand).isEmpty()) {
                stopReason = StopReason.TOOL_BROKEN;
                break;
            }
        }

        List<ItemStack> collectedDrops = Collections.emptyList();
        int totalExpCollected = 0;
        if (serverLevel != null && !minedBlocks.isEmpty()) {
            AABB searchArea = calculateSearchArea(minedBlocks);
            if (teleportDrops) {
                collectedDrops = collectAndTeleportDrops(serverLevel, player, searchArea, existingEntityIds);
            }
            if (teleportExp) {
                totalExpCollected = collectAndTeleportExp(
                        serverLevel,
                        player,
                        searchArea,
                        existingExperienceIds
                );
            }
        }

        // 触发 PostActionEvent
        ChainActionResult result = ChainActionResult.success(
                ChainActionType.MINING,
                minedBlocks,
                durabilityUsed,
                hungerUsed,
                stopReason,
                collectedDrops,
                totalExpCollected
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

    private static int calculateDurabilityDelta(ItemStack before, ItemStack after) {
        if (!before.isDamageableItem()) {
            return 0;
        }
        if (after.isEmpty()) {
            return Math.max(0, before.getMaxDamage() - before.getDamageValue());
        }
        if (!ItemStack.isSameItem(before, after)) {
            return 0;
        }
        return Math.max(0, after.getDamageValue() - before.getDamageValue());
    }

    private static void restoreDurability(
            ServerPlayer player,
            InteractionHand hand,
            ItemStack before
    ) {
        if (!before.isDamageableItem()) {
            return;
        }

        ItemStack current = player.getItemInHand(hand);
        if (current.isEmpty()) {
            player.setItemInHand(hand, before.copy());
        } else if (ItemStack.isSameItem(before, current)) {
            current.setDamageValue(before.getDamageValue());
        }
    }

    private static boolean canConsumeHunger(
            ServerPlayer player,
            float exhaustionCost,
            int minimumFoodLevel
    ) {
        return updateHunger(player, exhaustionCost, minimumFoodLevel, false);
    }

    private static boolean consumeHunger(
            ServerPlayer player,
            float exhaustionCost,
            int minimumFoodLevel
    ) {
        return updateHunger(player, exhaustionCost, minimumFoodLevel, true);
    }

    /**
     * Applies exhaustion without FoodData#addExhaustion's 40 point clamp.
     * FoodData exposes its exact exhaustion only through its public save API,
     * so the same persisted fields are read and restored atomically here.
     */
    private static boolean updateHunger(
            ServerPlayer player,
            float exhaustionCost,
            int minimumFoodLevel,
            boolean apply
    ) {
        if (!Float.isFinite(exhaustionCost) || exhaustionCost < 0f) {
            return false;
        }
        if (exhaustionCost == 0f) {
            return true;
        }

        FoodData foodData = player.getFoodData();
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                player.level().registryAccess()
        );
        foodData.addAdditionalSaveData(output);
        CompoundTag data = output.buildResult();

        int foodLevel = data.getIntOr(FOOD_LEVEL_KEY, foodData.getFoodLevel());
        if (foodLevel <= minimumFoodLevel) {
            return false;
        }

        float saturation = data.getFloatOr(
                FOOD_SATURATION_KEY,
                foodData.getSaturationLevel()
        );
        float totalExhaustion = data.getFloatOr(FOOD_EXHAUSTION_KEY, 0f)
                + exhaustionCost;
        if (!Float.isFinite(totalExhaustion)) {
            return false;
        }

        // Vanilla consumes one saturation/food point while exhaustion > 4,
        // leaving a remainder in the inclusive range (0, 4].
        long exhaustionUnits = totalExhaustion > 4f
                ? (long) Math.ceil(((double) totalExhaustion - 4d) / 4d)
                : 0L;
        long saturationUnits = saturation > 0f
                ? (long) Math.ceil(saturation)
                : 0L;
        long foodUnits = player.level().getDifficulty() == Difficulty.PEACEFUL
                ? 0L
                : Math.max(0L, exhaustionUnits - saturationUnits);
        if (foodUnits > foodLevel - minimumFoodLevel) {
            return false;
        }

        if (apply) {
            data.putInt(FOOD_LEVEL_KEY, foodLevel - (int) foodUnits);
            data.putFloat(
                    FOOD_SATURATION_KEY,
                    Math.max(0f, saturation - exhaustionUnits)
            );
            data.putFloat(
                    FOOD_EXHAUSTION_KEY,
                    totalExhaustion - exhaustionUnits * 4f
            );
            foodData.readAdditionalSaveData(TagValueInput.create(
                    ProblemReporter.DISCARDING,
                    player.level().registryAccess(),
                    data
            ));
        }
        return true;
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

        // API groups are checked first; the API method then falls back to
        // configured whitelist tags for the legacy loose-matching behavior.
        return OneKeyMinerAPI.areBlocksInSameGroup(originBlock, targetBlock);
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
        OneKeyMinerAPI.ToolActionRule activeInteractionRule =
                findActiveInteractionRule(context);
        if (OneKeyMinerAPI.isInteractionToolBlacklisted(heldItem)) {
            return ChainActionResult.cancelled(
                    ChainActionType.INTERACTION,
                    StopReason.EVENT_CANCELLED
            );
        }
        if (!isInteractionTool(heldItem) && activeInteractionRule == null) {
            return ChainActionResult.cancelled(ChainActionType.INTERACTION, StopReason.EVENT_CANCELLED);
        }

        // 检查激活条件
        if (!checkActivationConditions(context, config)) {
            return ChainActionResult.cancelled(ChainActionType.INTERACTION, StopReason.EVENT_CANCELLED);
        }

        // 根据工具类型选择交互目标
        InteractionType interactionType = determineInteractionType(context);

        // 收集交互目标
        List<BlockPos> collectedTargets = collectInteractionTargets(
                context,
                config,
                interactionType,
                activeInteractionRule
        );
        List<BlockPos> targets = interactionType == InteractionType.SHEARING
                ? sanitizeTargets(
                        context,
                        collectedTargets,
                        getMaxTargetCount(context, config),
                        getMaxTargetDistance(context, config),
                        ignored -> true,
                        true
                )
                : sanitizeTargets(context, collectedTargets, config);

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
        List<BlockPos> finalTargets = sanitizeTargets(
                context,
                preEvent.getTargetPositions(),
                getMaxTargetCount(context, config),
                getMaxTargetDistance(context, config),
                pos -> (pos.equals(context.getOriginPos())
                        && context.isOriginAlreadyHandled())
                        || (interactionType == InteractionType.SHEARING
                                ? hasReadyShearableAt(
                                        context,
                                        pos,
                                        activeInteractionRule
                                )
                                : isEligibleBlockInteractionTarget(
                                        context,
                                        pos,
                                        interactionType,
                                        activeInteractionRule,
                                        heldItem
                                )),
                interactionType == InteractionType.SHEARING
        );
        if (interactionType != InteractionType.SHEARING || context.getOriginEntityId() != null) {
            finalTargets = requireOriginFirst(context, finalTargets);
        }
        return performInteraction(
                context,
                finalTargets,
                config,
                interactionType,
                activeInteractionRule
        );
    }

    /**
     * 判断物品是否为交互工具
     *
     * <p>使用通用类型检查，自动支持模组工具。</p>
     */
    private static boolean isInteractionTool(ItemStack stack) {
        if (stack == null
                || stack.isEmpty()
                || OneKeyMinerAPI.isInteractionToolBlacklisted(stack)) {
            return false;
        }

        Item item = stack.getItem();

        // 使用物品类型继承检查，而非硬编码
        // 这样可以自动支持所有继承自这些基类的模组工具
        return item instanceof HoeItem ||        // 锄头类（耕地）
               item instanceof AxeItem ||        // 斧头类（剥皮）
               item instanceof ShovelItem ||     // 铲子类（土径）
               item instanceof ShearsItem ||     // 剪刀类（剪羊毛）
             OneKeyMinerAPI.isInteractionToolAllowed(stack) || // API 注册的工具
             OneKeyMinerAPI.isInteractiveItemAllowed(stack); // API 注册的交互物品
    }

    /**
     * Checks whether an item can attempt chained block interaction.
     */
    public static boolean canAttemptBlockInteraction(ItemStack stack) {
        if (stack == null
                || stack.isEmpty()
                || OneKeyMinerAPI.isInteractionToolBlacklisted(stack)) {
            return false;
        }

        Item item = stack.getItem();
        return item instanceof HoeItem ||
                item instanceof AxeItem ||
                item instanceof ShovelItem ||
                OneKeyMinerAPI.isInteractionToolAllowed(stack) ||
                OneKeyMinerAPI.isInteractiveItemAllowed(stack);
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
            case ITEM_USE -> ChainActionContext.InteractionOverride.ITEM_USE;
            case GENERIC -> ChainActionContext.InteractionOverride.GENERIC;
        };
    }

    /**
     * 检查工具是否可以与目标方块触发连锁交互
     */
    public static boolean isValidInteractionTarget(ItemStack stack, BlockState targetState) {
        if (stack == null || stack.isEmpty() || targetState == null) {
            return false;
        }

        if (OneKeyMinerAPI.findToolActionForBlock(
                stack,
                targetState,
                ChainActionType.INTERACTION
        ).isPresent()) {
            return true;
        }

        if (!isInteractionTool(stack)) {
            return false;
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

    private static OneKeyMinerAPI.ToolActionRule findActiveInteractionRule(
            ChainActionContext context
    ) {
        if (context.getMatchedToolActionRule() != null) {
            return context.getMatchedToolActionRule();
        }
        ItemStack stack = context.getHeldItem();
        if (context.getOriginEntityId() != null) {
            Entity originEntity = findOriginEntity(context);
            if (originEntity == null) {
                return null;
            }
            return OneKeyMinerAPI.findToolActionForEntity(
                            stack,
                            originEntity,
                            ChainActionType.INTERACTION
                    )
                    .orElse(null);
        }
        return OneKeyMinerAPI.findToolActionForBlock(
                        stack,
                        context.getOriginState(),
                        ChainActionType.INTERACTION
                )
                .orElse(null);
    }

    /**
     * 收集交互目标
     */
    private static List<BlockPos> collectInteractionTargets(
            ChainActionContext context,
            MinerConfig config,
            InteractionType interactionType,
            OneKeyMinerAPI.ToolActionRule activeRule
    ) {
        // 剪羊毛需要特殊处理（搜索实体而非方块）
        if (interactionType == InteractionType.SHEARING) {
            return collectShearingTargets(context, config, activeRule);
        }

        // 其他交互类型：搜索相邻的可交互方块
        return collectBlockInteractionTargets(
                context,
                config,
                interactionType,
                activeRule
        );
    }

    /**
     * 收集剪羊毛目标（方块位置，实际表示可剪实体的位置）
     */
    private static List<BlockPos> collectShearingTargets(
            ChainActionContext context,
            MinerConfig config,
            OneKeyMinerAPI.ToolActionRule activeRule
    ) {
        List<BlockPos> result = new ArrayList<>();
        ServerPlayer player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos originPos = context.getOriginPos();
        ItemStack heldItem = context.getHeldItem();

        int maxBlocks = getMaxTargetCount(context, config);
        int searchRadius = getMaxTargetDistance(context, config);

        Entity originEntity = findOriginEntity(context);
        if (context.isOriginAlreadyHandled()) {
            result.add(context.getOriginPos());
        } else if (isReadyShearable(originEntity)
                && matchesActiveEntityRule(activeRule, heldItem, originEntity)) {
            result.add(originEntity.blockPosition().immutable());
        }

        int remaining = Math.max(0, maxBlocks - result.size());
        if (remaining == 0) {
            return result;
        }

        // Bound the entity query itself. Materializing and sorting every
        // entity in an unusually dense farm can otherwise stall the server.
        AABB searchBox = new AABB(originPos).inflate(searchRadius);
        List<Entity> shearables = new ArrayList<>(remaining);
        level.getEntities(
                EntityTypeTest.forClass(Entity.class),
                searchBox,
                entity -> isReadyShearable(entity)
                        && matchesActiveEntityRule(activeRule, heldItem, entity)
                        && (originEntity == null
                                || !entity.getUUID().equals(originEntity.getUUID())),
                shearables,
                remaining
        );

        // Sort only the bounded candidate set, preferring nearby entities.
        shearables.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));

        for (Entity shearable : shearables) {
            result.add(shearable.blockPosition());
        }

        return result;
    }

    /**
     * 收集方块交互目标
     */
    private static List<BlockPos> collectBlockInteractionTargets(
            ChainActionContext context,
            MinerConfig config,
            InteractionType interactionType,
            OneKeyMinerAPI.ToolActionRule activeRule
    ) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        BlockPos originPos = context.getOriginPos();
        BlockState originState = context.getOriginState();
        Level level = context.getLevel();
        ItemStack heldItem = context.getHeldItem();

        int maxBlocks = getMaxTargetCount(context, config);
        int maxDistance = getMaxTargetDistance(context, config);
        boolean allowDiagonal = context.isAllowDiagonal() && config.allowDiagonal;

        BlockPos[] offsets = allowDiagonal ? DIAGONAL_OFFSETS : ORTHOGONAL_OFFSETS;

        // 从起始位置开始（包括起始位置）
        queue.add(originPos);
        visited.add(originPos);

        long startTime = System.currentTimeMillis();
        int iterations = 0;
        int iterationBudget = Math.min(
                MAX_ITERATIONS,
                Math.max(256, maxBlocks * (allowDiagonal ? 32 : 8))
        );

        while (!queue.isEmpty() && result.size() < maxBlocks && iterations < iterationBudget) {
            if (System.currentTimeMillis() - startTime > OPERATION_TIMEOUT_MS) {
                break;
            }

            iterations++;
            BlockPos current = queue.poll();

            if (current.distManhattan(originPos) > maxDistance) {
                continue;
            }
            if (!level.hasChunkAt(current)) {
                continue;
            }

            BlockState currentState = level.getBlockState(current);
            boolean completedOrigin = context.isOriginAlreadyHandled()
                    && current.equals(originPos);

            // A completed origin remains the authorization/traversal token,
            // but only neighboring targets are simulated.
            if (completedOrigin
                    || canInteractAt(currentState, interactionType, originState, heldItem)
                            && matchesActiveBlockRule(
                                    activeRule,
                                    heldItem,
                                    currentState
                            )) {
                result.add(current);

                // Only traverse the connected component of eligible targets.
                for (BlockPos offset : offsets) {
                    BlockPos neighbor = current.offset(offset);
                    if (!visited.contains(neighbor) && level.hasChunkAt(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }

        return result;
    }

    /**
     * 检查指定位置是否可以进行交互
     */
    private static boolean canInteractAt(
            BlockState state,
            InteractionType interactionType,
            BlockState originState,
            ItemStack heldItem
    ) {
        if (state.isAir()) {
            return false;
        }

        // 根据交互类型检查方块是否匹配
        return switch (interactionType) {
            case TILLING -> canTill(state) || isSameBlock(state, originState);
            case STRIPPING -> canStrip(state) || isSameBlock(state, originState);
            case PATH_MAKING -> canMakePath(state) || isSameBlock(state, originState);
            case BRUSHING -> canBrush(state) || isSameBlock(state, originState);
            case ITEM_USE -> isSameBlock(state, originState)
                    && canItemUseOnBlock(heldItem, state);
            case GENERIC -> state.getBlock() == originState.getBlock();
            default -> false;
        };
    }

    private static boolean isEligibleBlockInteractionTarget(
            ChainActionContext context,
            BlockPos pos,
            InteractionType interactionType,
            OneKeyMinerAPI.ToolActionRule activeRule,
            ItemStack heldItem
    ) {
        if (!context.getLevel().hasChunkAt(pos)) {
            return false;
        }
        BlockState state = context.getLevel().getBlockState(pos);
        return canInteractAt(
                state,
                interactionType,
                context.getOriginState(),
                heldItem
        ) && matchesActiveBlockRule(activeRule, heldItem, state);
    }

    private static boolean matchesActiveBlockRule(
            OneKeyMinerAPI.ToolActionRule activeRule,
            ItemStack stack,
            BlockState state
    ) {
        return activeRule == null
                || OneKeyMinerAPI.isToolActionRuleApplicableToBlock(
                        activeRule,
                        stack,
                        state
                );
    }

    private static boolean matchesActiveEntityRule(
            OneKeyMinerAPI.ToolActionRule activeRule,
            ItemStack stack,
            Entity entity
    ) {
        return activeRule == null
                || OneKeyMinerAPI.isToolActionRuleApplicableToEntity(
                        activeRule,
                        stack,
                        entity
                );
    }

    /**
     * 检查方块是否可以耕地
     */
    private static boolean isSameBlock(BlockState state, BlockState originState) {
        return originState != null && state.getBlock() == originState.getBlock();
    }

    private static boolean canTill(BlockState state) {
        // 检查是否在可耕地标签中
        String prefix = PlatformServices.getInstance().getConventionalTagPrefix();
        return TagResolver.matchesBlock(state.getBlock(), "#minecraft:dirt") ||
               TagResolver.matchesBlock(state.getBlock(), "#" + prefix + ":tillable");
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
            InteractionType interactionType,
            OneKeyMinerAPI.ToolActionRule activeRule
    ) {
        // 剪羊毛需要特殊处理
        if (interactionType == InteractionType.SHEARING) {
            return performShearing(context, targets, config, activeRule);
        }

        return performBlockInteraction(context, targets, config, activeRule);
    }

    /**
     * 执行剪羊毛操作
     */
    private static ChainActionResult performShearing(
            ChainActionContext context,
            List<BlockPos> entityPositions,
            MinerConfig config,
            OneKeyMinerAPI.ToolActionRule activeRule
    ) {
        ServerPlayer player = context.getPlayer();
        Level level = context.getLevel();
        InteractionHand hand = context.getHand();

        List<BlockPos> shearedPositions = new ArrayList<>();
        if (context.isOriginAlreadyHandled()) {
            shearedPositions.add(context.getOriginPos());
        }
        int durabilityUsed = 0;
        StopReason stopReason = StopReason.COMPLETED;
        Set<UUID> processedEntities = new HashSet<>();
        UUID originEntityId = context.getOriginEntityId();
        if (context.isOriginAlreadyHandled() && originEntityId != null) {
            processedEntities.add(originEntityId);
        }
        Map<BlockPos, ArrayDeque<Entity>> candidatesByPosition =
                collectShearingCandidates(
                        level,
                        entityPositions,
                        player.getItemInHand(hand),
                        activeRule
                );

        for (BlockPos pos : entityPositions) {
            Entity target = null;
            boolean isOriginTarget = originEntityId != null && processedEntities.isEmpty();
            if (isOriginTarget) {
                Entity exactOrigin = findOriginEntity(context);
                if (isReadyShearable(exactOrigin)
                        && matchesActiveEntityRule(
                                activeRule,
                                player.getItemInHand(hand),
                                exactOrigin
                        )
                        && exactOrigin.blockPosition().equals(pos)) {
                    target = exactOrigin;
                }
            }

            if (target == null && !isOriginTarget) {
                ArrayDeque<Entity> candidates = candidatesByPosition.get(pos);
                while (candidates != null && !candidates.isEmpty()) {
                    Entity candidate = candidates.removeFirst();
                    if (!processedEntities.contains(candidate.getUUID())
                            && candidate.blockPosition().equals(pos)
                            && isReadyShearable(candidate)
                            && matchesActiveEntityRule(
                                    activeRule,
                                    player.getItemInHand(hand),
                                    candidate
                            )) {
                        target = candidate;
                        break;
                    }
                }
            }

            if (target == null) {
                if (isOriginTarget) {
                    stopReason = StopReason.PERMISSION_DENIED;
                    break;
                }
                continue;
            }
            ItemStack shears = player.getItemInHand(hand);
            if (!isSameCapturedItem(context, shears)) {
                stopReason = StopReason.TOOL_BROKEN;
                break;
            }
            if (!matchesActiveEntityRule(activeRule, shears, target)) {
                if (isOriginTarget) {
                    stopReason = StopReason.PERMISSION_DENIED;
                    break;
                }
                continue;
            }
            processedEntities.add(target.getUUID());

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

            ItemStack toolBefore = shears.copy();
            ShearingCompletionVerifier.Snapshot shearingSnapshot =
                    ShearingCompletionVerifier.capture(level, target);
            InteractionResult interactionResult = PlatformServices.getInstance()
                    .simulateEntityInteraction(player, level, target, hand);
            ItemStack toolAfter = player.getItemInHand(hand);
            if (!config.consumeDurability && !context.isCreativeMode()) {
                restoreDurability(player, hand, toolBefore);
                toolAfter = player.getItemInHand(hand);
            }

            boolean completedShearing = ShearingCompletionVerifier.completed(
                    level,
                    shearingSnapshot
            );
            if (!interactionResult.consumesAction() || !completedShearing) {
                if (isOriginTarget) {
                    stopReason = StopReason.PERMISSION_DENIED;
                    break;
                }
                continue;
            }
            shearedPositions.add(pos);

            if (!context.isCreativeMode() && config.consumeDurability) {
                durabilityUsed += calculateDurabilityDelta(toolBefore, toolAfter);
            }

            if (player.getItemInHand(hand).isEmpty()) {
                stopReason = StopReason.TOOL_BROKEN;
                break;
            }
        }

        ChainActionResult result = ChainActionResult.success(
                ChainActionType.INTERACTION,
                shearedPositions,
                durabilityUsed,
                0f,
                stopReason,
                Collections.emptyList(),
                0
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
            MinerConfig config,
            OneKeyMinerAPI.ToolActionRule activeRule
    ) {
        ServerPlayer player = context.getPlayer();
        Level level = context.getLevel();
        InteractionHand hand = context.getHand();

        List<BlockPos> interactedPositions = new ArrayList<>();
        if (context.isOriginAlreadyHandled()) {
            interactedPositions.add(context.getOriginPos());
        }
        int durabilityUsed = 0;
        StopReason stopReason = StopReason.COMPLETED;

        for (BlockPos pos : targets) {
            ItemStack tool = player.getItemInHand(hand);
            if (!isSameCapturedItem(context, tool)) {
                stopReason = StopReason.TOOL_BROKEN;
                break;
            }
            BlockState currentState = level.getBlockState(pos);
            if (!matchesActiveBlockRule(activeRule, tool, currentState)) {
                if (pos.equals(context.getOriginPos())) {
                    stopReason = StopReason.PERMISSION_DENIED;
                    break;
                }
                continue;
            }
            // 工具耐久检查
            if (config.consumeDurability && config.stopOnLowDurability && !context.isCreativeMode() && tool.isDamageableItem()) {
                int remaining = tool.getMaxDamage() - tool.getDamageValue();
                if (remaining <= config.preserveDurability) {
                    stopReason = StopReason.TOOL_DURABILITY_LOW;
                    break;
                }
            }

            // Delegate to ServerPlayerGameMode so loader hooks and protection
            // mods observe the authoritative interaction exactly once.
            ItemStack toolBefore = tool.copy();
            boolean accepted = simulateItemUse(context, pos, tool);
            BlockState completedState = level.getBlockState(pos);
            OriginalUseCompletionPolicy.BlockRequirement requirement =
                    activeRule != null || !isNativeTransformTool(tool)
                            ? OriginalUseCompletionPolicy.BlockRequirement.TRUST_RESULT
                            : OriginalUseCompletionPolicy.BlockRequirement.STATE_CHANGE;
            boolean success = accepted
                    && OriginalUseCompletionPolicy.permitsDerivedBlockUse(
                            requirement,
                            !completedState.equals(currentState),
                            !completedState.isAir()
                    );
            ItemStack toolAfter = player.getItemInHand(hand);
            if (!config.consumeDurability && !context.isCreativeMode()) {
                restoreDurability(player, hand, toolBefore);
                toolAfter = player.getItemInHand(hand);
            }

            if (success) {
                interactedPositions.add(pos);
                if (!context.isCreativeMode() && config.consumeDurability) {
                    durabilityUsed += calculateDurabilityDelta(toolBefore, toolAfter);
                }
            } else if (pos.equals(context.getOriginPos())) {
                stopReason = StopReason.PERMISSION_DENIED;
                break;
            }

            if (player.getItemInHand(hand).isEmpty()) {
                stopReason = StopReason.TOOL_BROKEN;
                break;
            }
        }

        ChainActionResult result = ChainActionResult.success(
                ChainActionType.INTERACTION,
                interactedPositions,
                durabilityUsed,
                0f,
                stopReason,
                Collections.emptyList(),
                0
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
            ChainActionContext context,
            BlockPos pos,
            ItemStack item
    ) {
        return PlatformServices.getInstance().simulateItemUseOnBlock(
                context.getPlayer(),
                context.getLevel(),
                translateHitResult(context.getBlockHitResult(), pos),
                context.getHand(),
                item
        );
    }

    private static BlockHitResult translateHitResult(
            BlockHitResult original,
            BlockPos targetPos
    ) {
        if (original == null) {
            return new BlockHitResult(
                    net.minecraft.world.phys.Vec3.atCenterOf(targetPos),
                    Direction.UP,
                    targetPos,
                    false
            );
        }

        BlockPos sourcePos = original.getBlockPos();
        return new BlockHitResult(
                original.getLocation().add(
                        (double) targetPos.getX() - sourcePos.getX(),
                        (double) targetPos.getY() - sourcePos.getY(),
                        (double) targetPos.getZ() - sourcePos.getZ()
                ),
                original.getDirection(),
                targetPos,
                original.isInside()
        );
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

    private static List<ItemStack> collectAndTeleportDrops(
            ServerLevel level,
            ServerPlayer player,
            AABB area,
            Set<Integer> existingEntityIds
    ) {
        List<ItemEntity> newItems = level.getEntitiesOfClass(ItemEntity.class, area,
                entity -> !existingEntityIds.contains(entity.getId()) && entity.isAlive());

        List<ItemStack> collectedDrops = new ArrayList<>();
        for (ItemEntity itemEntity : newItems) {
            ItemStack before = itemEntity.getItem().copy();
            int beforeCount = before.getCount();
            itemEntity.setNoPickUpDelay();
            itemEntity.teleportTo(player.getX(), player.getY(), player.getZ());

            // Use vanilla pickup so loader pickup events, advancements and
            // inventory rules remain observable.
            itemEntity.playerTouch(player);

            int remainingCount = 0;
            if (itemEntity.isAlive()
                    && ItemStack.isSameItemSameComponents(before, itemEntity.getItem())) {
                remainingCount = itemEntity.getItem().getCount();
                itemEntity.teleportTo(player.getX(), player.getY(), player.getZ());
            }
            int pickedUp = Math.max(0, beforeCount - remainingCount);
            if (pickedUp > 0) {
                collectedDrops.add(before.copyWithCount(pickedUp));
            }
        }
        return collectedDrops;
    }

    private static int collectAndTeleportExp(
            ServerLevel level,
            ServerPlayer player,
            AABB area,
            Set<Integer> existingExperienceIds
    ) {
        List<ExperienceOrb> newOrbs = level.getEntitiesOfClass(ExperienceOrb.class, area,
                entity -> entity.isAlive()
                        && !existingExperienceIds.contains(entity.getId()));

        int immediatelyCollected = 0;
        for (ExperienceOrb orb : newOrbs) {
            int pickupDelayBefore = player.takeXpDelay;
            int orbValue = orb.getValue();
            orb.teleportTo(player.getX(), player.getY(), player.getZ());

            // Keep the vanilla pickup path: merged orbs contain a private
            // count, and direct give/discard loses that count and bypasses
            // Mending plus loader pickup hooks. One immediate touch is safe;
            // any remaining count/orbs stay at the player for later ticks.
            orb.playerTouch(player);
            if (pickupDelayBefore == 0 && player.takeXpDelay != 0) {
                immediatelyCollected += orbValue;
            }
        }

        return immediatelyCollected;
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
        BlockState originSupport = context.getLevel().getBlockState(
                context.getOriginPos().below()
        );
        OneKeyMinerAPI.ToolActionRule activePlantingRule =
                context.getMatchedToolActionRule() != null
                        ? context.getMatchedToolActionRule()
                        : OneKeyMinerAPI.findToolActionForBlock(
                                heldItem,
                                originSupport,
                                ChainActionType.PLANTING
                        ).orElse(null);
        if (!isPlantableItem(heldItem) && activePlantingRule == null) {
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
        List<BlockPos> plantablePositions = sanitizeTargets(
                context,
                collectPlantablePositions(
                        context,
                        config,
                        activePlantingRule
                ),
                config
        );

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
        List<BlockPos> finalTargets = requireOriginFirst(
                context,
                sanitizeTargets(
                        context,
                        preEvent.getTargetPositions(),
                        getMaxTargetCount(context, config),
                        getMaxTargetDistance(context, config),
                        pos -> pos.equals(context.getOriginPos())
                                && context.isOriginAlreadyHandled()
                                || canPlantAt(
                                        context.getLevel(),
                                        pos,
                                        heldItem,
                                        config
                                ) && matchesActiveBlockRule(
                                        activePlantingRule,
                                        heldItem,
                                        context.getLevel().getBlockState(pos.below())
                                )
                )
        );
        return performPlanting(
                context,
                finalTargets,
                activePlantingRule
        );
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
        String prefix = PlatformServices.getInstance().getConventionalTagPrefix();
        return TagResolver.matchesItem(item, "#" + prefix + ":seeds") ||
               TagResolver.matchesItem(item, "#minecraft:saplings") ||
               OneKeyMinerAPI.isPlantableItemAllowed(stack);
    }

    /**
     * 收集可种植位置
     */
    private static List<BlockPos> collectPlantablePositions(
            ChainActionContext context,
            MinerConfig config,
            OneKeyMinerAPI.ToolActionRule activePlantingRule
    ) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        ServerPlayer player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos originPos = context.getOriginPos();
        ItemStack seedItem = context.getHeldItem();

        int maxBlocks = getMaxTargetCount(context, config);
        int maxDistance = getMaxTargetDistance(context, config);

        // 计算可用种子数量
        int availableSeeds = context.isCreativeMode()
                ? Integer.MAX_VALUE
                : player.getItemInHand(context.getHand()).getCount();
        if (context.isOriginAlreadyHandled()
                && availableSeeds < Integer.MAX_VALUE) {
            availableSeeds++;
        }

        queue.add(originPos);
        visited.add(originPos);

        long startTime = System.currentTimeMillis();
        int iterations = 0;
        int iterationBudget = Math.min(MAX_ITERATIONS, Math.max(256, maxBlocks * 8));

        while (!queue.isEmpty() && result.size() < maxBlocks &&
               result.size() < availableSeeds && iterations < iterationBudget) {

            if (System.currentTimeMillis() - startTime > OPERATION_TIMEOUT_MS) {
                break;
            }

            iterations++;
            BlockPos current = queue.poll();

            if (current.distManhattan(originPos) > maxDistance) {
                continue;
            }
            if (!level.hasChunkAt(current)) {
                continue;
            }

            boolean completedOrigin = context.isOriginAlreadyHandled()
                    && current.equals(originPos);

            // The original seed was placed by vanilla before the chain runs.
            if (completedOrigin
                    || canPlantAt(level, current, seedItem, config)
                            && matchesActiveBlockRule(
                                    activePlantingRule,
                                    seedItem,
                                    level.getBlockState(current.below())
                            )) {
                result.add(current);

                // Keep planting traversal on the connected plantable surface.
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockPos neighbor = current.relative(dir);
                    if (!visited.contains(neighbor) && level.hasChunkAt(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
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
        String prefix = PlatformServices.getInstance().getConventionalTagPrefix();
        return TagResolver.matchesBlock(belowState.getBlock(), "#minecraft:dirt") ||
               TagResolver.matchesBlock(belowState.getBlock(), "#" + prefix + ":farmland");
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
            OneKeyMinerAPI.ToolActionRule activePlantingRule
    ) {
        ServerPlayer player = context.getPlayer();
        Level level = context.getLevel();
        InteractionHand hand = context.getHand();

        List<BlockPos> plantedPositions = new ArrayList<>();
        if (context.isOriginAlreadyHandled()) {
            plantedPositions.add(context.getOriginPos());
        }
        StopReason stopReason = StopReason.COMPLETED;

        for (BlockPos pos : positions) {
            ItemStack currentSeed = player.getItemInHand(hand);
            // ServerPlayerGameMode uses the selected hand. Do not silently
            // consume matching items from unrelated inventory slots.
            if (!isSameCapturedItem(context, currentSeed)) {
                stopReason = StopReason.ITEMS_EXHAUSTED;
                break;
            }
            if (!matchesActiveBlockRule(
                    activePlantingRule,
                    currentSeed,
                    level.getBlockState(pos.below())
            )) {
                if (pos.equals(context.getOriginPos())) {
                    stopReason = StopReason.PERMISSION_DENIED;
                    break;
                }
                continue;
            }

            BlockState stateBeforePlanting = level.getBlockState(pos);
            boolean accepted = simulateItemUse(
                    context,
                    pos.below(),
                    currentSeed
            );
            BlockState stateAfterPlanting = level.getBlockState(pos);
            OriginalUseCompletionPolicy.BlockRequirement requirement =
                    OriginalUseCompletionPolicy.selectBlockRequirement(
                            true,
                            activePlantingRule != null,
                            false
                    );
            boolean success = accepted
                    && OriginalUseCompletionPolicy.permitsDerivedBlockUse(
                            requirement,
                            !stateAfterPlanting.equals(stateBeforePlanting),
                            !stateAfterPlanting.isAir()
                    );

            if (success) {
                plantedPositions.add(pos);
            } else if (pos.equals(context.getOriginPos())) {
                stopReason = StopReason.PERMISSION_DENIED;
                break;
            }
        }

        ChainActionResult result = ChainActionResult.success(
                ChainActionType.PLANTING,
                plantedPositions,
                0,
                0f,
                stopReason,
                Collections.emptyList(),
                0
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

        OneKeyMinerAPI.ToolActionRule activeHarvestingRule =
                OneKeyMinerAPI.findToolActionForBlock(
                        context.getHeldItem(),
                        context.getOriginState(),
                        ChainActionType.HARVESTING
                ).orElse(null);

        // 检查激活条件
        if (!checkActivationConditions(context, config)) {
            return ChainActionResult.cancelled(ChainActionType.HARVESTING, StopReason.EVENT_CANCELLED);
        }

        // 收集收割目标（同类型成熟作物）
        List<BlockPos> targets = sanitizeTargets(
                context,
                collectHarvestTargets(
                        context,
                        config,
                        activeHarvestingRule
                ),
                config
        );

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
        List<BlockPos> finalTargets = requireOriginFirst(
                context,
                sanitizeTargets(
                        context,
                        preEvent.getTargetPositions(),
                        getMaxTargetCount(context, config),
                        getMaxTargetDistance(context, config),
                        pos -> {
                            BlockState state = context.getLevel().getBlockState(pos);
                            return isMatchingHarvestTarget(
                                    context,
                                    state,
                                    activeHarvestingRule,
                                    context.getHeldItem()
                            );
                        }
                )
        );
        return performHarvesting(
                context,
                finalTargets,
                config,
                activeHarvestingRule
        );
    }

    /**
     * 收集收割目标
     *
     * <p>使用水平BFS搜索同类型成熟作物</p>
     */
    private static List<BlockPos> collectHarvestTargets(
            ChainActionContext context,
            MinerConfig config,
            OneKeyMinerAPI.ToolActionRule activeHarvestingRule
    ) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        Level level = context.getLevel();
        BlockPos originPos = context.getOriginPos();
        ItemStack heldItem = context.getHeldItem();

        int maxBlocks = getMaxTargetCount(context, config);
        int maxDistance = getMaxTargetDistance(context, config);

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
            if (!level.hasChunkAt(current)) {
                continue;
            }

            BlockState state = level.getBlockState(current);
            if (isMatchingHarvestTarget(
                    context,
                    state,
                    activeHarvestingRule,
                    heldItem
            )) {
                result.add(current);

                // 仅从匹配位置扩展搜索（水平方向）
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockPos neighbor = current.relative(dir);
                    if (!visited.contains(neighbor) && level.hasChunkAt(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }

        return result;
    }

    private static Map<BlockPos, ArrayDeque<Entity>> collectShearingCandidates(
            Level level,
            List<BlockPos> positions,
            ItemStack heldItem,
            OneKeyMinerAPI.ToolActionRule activeRule
    ) {
        if (positions.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<BlockPos, Integer> remainingByPosition = new HashMap<>();
        for (BlockPos pos : positions) {
            remainingByPosition.merge(pos, 1, Integer::sum);
        }

        List<Entity> candidates = new ArrayList<>(positions.size());
        level.getEntities(
                EntityTypeTest.forClass(Entity.class),
                calculateSearchArea(positions),
                entity -> {
                    BlockPos entityPos = entity.blockPosition();
                    int remaining = remainingByPosition.getOrDefault(entityPos, 0);
                    if (remaining == 0
                            || !isReadyShearable(entity)
                            || !matchesActiveEntityRule(activeRule, heldItem, entity)) {
                        return false;
                    }
                    remainingByPosition.put(entityPos, remaining - 1);
                    return true;
                },
                candidates,
                positions.size()
        );

        Map<BlockPos, List<Entity>> sortedByPosition = new HashMap<>();
        for (Entity candidate : candidates) {
            sortedByPosition.computeIfAbsent(
                    candidate.blockPosition().immutable(),
                    ignored -> new ArrayList<>()
            ).add(candidate);
        }

        Map<BlockPos, ArrayDeque<Entity>> result = new HashMap<>();
        sortedByPosition.forEach((pos, entities) -> {
            entities.sort(Comparator.comparingDouble(entity ->
                    entity.distanceToSqr(
                            pos.getX() + 0.5,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5
                    )));
            result.put(pos, new ArrayDeque<>(entities));
        });
        return result;
    }

    private static boolean isMatchingHarvestTarget(
            ChainActionContext context,
            BlockState state,
            OneKeyMinerAPI.ToolActionRule activeHarvestingRule,
            ItemStack heldItem
    ) {
        if (!isMatureCrop(state)) {
            return false;
        }
        if (activeHarvestingRule == null) {
            return state.getBlock() == context.getOriginState().getBlock();
        }
        return OneKeyMinerAPI.isToolActionRuleApplicableToBlock(
                activeHarvestingRule,
                heldItem,
                state
        );
    }

    /**
     * 执行收割操作
     */
    private static ChainActionResult performHarvesting(
            ChainActionContext context,
            List<BlockPos> targets,
            MinerConfig config,
            OneKeyMinerAPI.ToolActionRule activeHarvestingRule
    ) {
        ServerPlayer player = context.getPlayer();
        Level level = context.getLevel();
        InteractionHand hand = context.getHand();
        ServerLevel serverLevel = level instanceof ServerLevel sl ? sl : null;
        float hungerPerBlock = config.hungerPerBlock * Math.max(0f, config.hungerMultiplier);
        boolean chargesHunger = config.consumeHunger
                && !context.isCreativeMode()
                && hungerPerBlock > 0f;
        boolean teleportDrops = config.isDropTeleportEnabled(
                MiningStateManager.isTeleportDrops(player)
        );
        boolean teleportExp = config.isExperienceTeleportEnabled(
                MiningStateManager.isTeleportExp(player)
        );

        List<BlockPos> harvestedPositions = new ArrayList<>();
        int durabilityUsed = 0;
        float hungerUsed = 0f;
        StopReason stopReason = StopReason.COMPLETED;
        Set<Integer> existingEntityIds = new HashSet<>();
        Set<Integer> existingExperienceIds = new HashSet<>();
        AABB searchArea = calculateSearchArea(targets);
        if (serverLevel != null && (teleportDrops || teleportExp || config.harvestReplant)) {
            if (teleportDrops || config.harvestReplant) {
                for (ItemEntity entity : serverLevel.getEntitiesOfClass(ItemEntity.class, searchArea)) {
                    existingEntityIds.add(entity.getId());
                }
            }
            if (teleportExp) {
                for (ExperienceOrb entity : serverLevel.getEntitiesOfClass(ExperienceOrb.class, searchArea)) {
                    existingExperienceIds.add(entity.getId());
                }
            }
        }

        for (BlockPos pos : targets) {
            if (!level.hasChunkAt(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (!isMatchingHarvestTarget(
                    context,
                    state,
                    activeHarvestingRule,
                    player.getItemInHand(hand)
            )) {
                if (pos.equals(context.getOriginPos())) {
                    stopReason = StopReason.PERMISSION_DENIED;
                    break;
                }
                continue;
            }
            if (chargesHunger
                    && !canConsumeHunger(player, hungerPerBlock, config.minHungerLevel)) {
                stopReason = StopReason.HUNGER_LOW;
                break;
            }

            boolean berryHarvest = state.getBlock() instanceof SweetBerryBushBlock;
            // ServerPlayerGameMode#destroyBlock always uses the main hand.
            InteractionHand actionHand = berryHarvest ? hand : InteractionHand.MAIN_HAND;
            ItemStack toolBefore = player.getItemInHand(actionHand).copy();
            if (config.consumeDurability && config.stopOnLowDurability
                    && !context.isCreativeMode() && toolBefore.isDamageableItem()) {
                int remaining = toolBefore.getMaxDamage() - toolBefore.getDamageValue();
                if (remaining <= config.preserveDurability) {
                    stopReason = StopReason.TOOL_DURABILITY_LOW;
                    break;
                }
            }

            boolean success;
            if (berryHarvest) {
                success = simulateItemUse(
                        context,
                        pos,
                        player.getItemInHand(hand)
                ) && !level.getBlockState(pos).equals(state);
            } else {
                // ServerPlayerGameMode is authoritative: protection, loot,
                // experience and loader break hooks all run once.
                success = PlatformServices.getInstance().simulateBlockBreak(player, level, pos);
            }

            ItemStack toolAfter = player.getItemInHand(actionHand);
            if (!config.consumeDurability && !context.isCreativeMode()) {
                restoreDurability(player, actionHand, toolBefore);
                toolAfter = player.getItemInHand(actionHand);
            }

            if (!success && pos.equals(context.getOriginPos())) {
                stopReason = StopReason.PERMISSION_DENIED;
                break;
            }
            if (!success) {
                continue;
            }

            if (config.harvestReplant && serverLevel != null
                    && !berryHarvest) {
                tryReplantAfterBreak(serverLevel, pos, state, player, existingEntityIds);
            }

            harvestedPositions.add(pos.immutable());
            if (!context.isCreativeMode() && config.consumeDurability) {
                durabilityUsed += calculateDurabilityDelta(toolBefore, toolAfter);
            }
            if (chargesHunger
                    && consumeHunger(player, hungerPerBlock, config.minHungerLevel)) {
                hungerUsed += hungerPerBlock;
            } else if (chargesHunger) {
                stopReason = StopReason.HUNGER_LOW;
                break;
            }
            if (!toolBefore.isEmpty() && player.getItemInHand(actionHand).isEmpty()) {
                stopReason = StopReason.TOOL_BROKEN;
                break;
            }
        }

        List<ItemStack> collectedDrops = Collections.emptyList();
        int experienceCollected = 0;
        if (serverLevel != null && !harvestedPositions.isEmpty()) {
            AABB harvestedArea = calculateSearchArea(harvestedPositions);
            if (teleportDrops) {
                collectedDrops = collectAndTeleportDrops(
                        serverLevel,
                        player,
                        harvestedArea,
                        existingEntityIds
                );
            }
            if (teleportExp) {
                experienceCollected = collectAndTeleportExp(
                        serverLevel,
                        player,
                        harvestedArea,
                        existingExperienceIds
                );
            }
        }

        ChainActionResult result = ChainActionResult.success(
                ChainActionType.HARVESTING,
                harvestedPositions,
                durabilityUsed,
                hungerUsed,
                stopReason,
                collectedDrops,
                experienceCollected
        );
        ChainEvents.firePostActionEvent(new PostActionEvent(
                player,
                level,
                context.getOriginPos(),
                result
        ));
        return result;
    }

    private static Entity findOriginEntity(ChainActionContext context) {
        UUID entityId = context.getOriginEntityId();
        if (entityId == null || !(context.getLevel() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getEntity(entityId);
    }

    private static boolean isReadyShearable(Entity entity) {
        return entity instanceof Shearable shearable
                && shearable.readyForShearing()
                && entity.isAlive();
    }

    private static boolean isSameCapturedItem(
            ChainActionContext context,
            ItemStack current
    ) {
        return current != null
                && !current.isEmpty()
                && ItemStack.isSameItem(context.getHeldItem(), current);
    }

    private static boolean isNativeTransformTool(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof HoeItem
                || item instanceof AxeItem
                || item instanceof ShovelItem;
    }

    private static boolean hasReadyShearableAt(
            ChainActionContext context,
            BlockPos pos,
            OneKeyMinerAPI.ToolActionRule activeRule
    ) {
        AABB searchBox = new AABB(pos).inflate(1.5);
        ItemStack heldItem = context.getHeldItem();
        List<Entity> matches = new ArrayList<>(1);
        context.getLevel().getEntities(
                EntityTypeTest.forClass(Entity.class),
                searchBox,
                entity -> entity.blockPosition().equals(pos)
                        && isReadyShearable(entity)
                        && matchesActiveEntityRule(activeRule, heldItem, entity),
                matches,
                1
        );
        return !matches.isEmpty();
    }

    /**
     * 尝试补种作物
     *
     * <p>从本次掉落或背包预留一个种子，再走服务端原版物品使用路径补种。
     * 这样放置保护事件能够取消操作；失败时种子会退回玩家背包。</p>
     */
    private static void tryReplantAfterBreak(
            ServerLevel level,
            BlockPos pos,
            BlockState harvestedState,
            ServerPlayer player,
            Set<Integer> existingEntityIds
    ) {
        Block block = harvestedState.getBlock();
        Item seedItem;
        BlockPos clickedPos;
        Direction clickedFace;

        if (block instanceof CropBlock crop) {
            seedItem = findSeedForCrop(crop);
            clickedPos = pos.below();
            clickedFace = Direction.UP;
        } else if (block instanceof NetherWartBlock) {
            seedItem = Items.NETHER_WART;
            clickedPos = pos.below();
            clickedFace = Direction.UP;
        } else if (block instanceof CocoaBlock) {
            seedItem = Items.COCOA_BEANS;
            Direction supportDirection = harvestedState.getValue(CocoaBlock.FACING);
            clickedPos = pos.relative(supportDirection);
            clickedFace = supportDirection.getOpposite();
        } else {
            return;
        }

        if (seedItem == null
                || seedItem == Items.AIR
                || OneKeyMinerAPI.isSeedBlacklisted(seedItem)) {
            return;
        }

        boolean creative = player.isCreative();
        if (!creative
                && !consumeNewDropOrInventory(
                        level,
                        pos,
                        player,
                        seedItem,
                        existingEntityIds
                )) {
            return;
        }

        ItemStack previousMainHand = player.getMainHandItem();
        ItemStack plantingStack = new ItemStack(seedItem);
        Vec3 hitLocation = Vec3.atCenterOf(clickedPos).add(
                clickedFace.getStepX() * 0.5d,
                clickedFace.getStepY() * 0.5d,
                clickedFace.getStepZ() * 0.5d
        );
        BlockHitResult hitResult = new BlockHitResult(
                hitLocation,
                clickedFace,
                clickedPos,
                false
        );

        boolean accepted;
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, plantingStack);
            accepted = PlatformServices.getInstance().simulateItemUseOnBlock(
                    player,
                    level,
                    hitResult,
                    InteractionHand.MAIN_HAND,
                    plantingStack
            );
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, previousMainHand);
        }

        BlockState plantedState = level.getBlockState(pos);
        boolean planted = accepted && plantedState.getBlock() == block;
        if (planted && block instanceof CropBlock crop) {
            planted = crop.getAge(plantedState) == 0;
        } else if (planted && block instanceof NetherWartBlock) {
            planted = plantedState.getValue(NetherWartBlock.AGE) == 0;
        } else if (planted && block instanceof CocoaBlock) {
            planted = plantedState.getValue(CocoaBlock.AGE) == 0
                    && plantedState.getValue(CocoaBlock.FACING)
                    == harvestedState.getValue(CocoaBlock.FACING);
        }

        if (!planted
                && !creative
                && plantingStack.is(seedItem)
                && !plantingStack.isEmpty()) {
            player.getInventory().placeItemBackInInventory(new ItemStack(seedItem));
        }
    }

    /**
     * 查找作物对应的种子物品
     *
     * <p>CropBlock 的注册物品就是原版种植使用的种子/作物物品。</p>
     */
    private static Item findSeedForCrop(CropBlock crop) {
        Item blockItem = crop.asItem();
        return blockItem != Items.AIR ? blockItem : null;
    }

    /**
     * 从掉落物列表中移除一个指定物品
     *
     * @return 是否成功移除
     */
    private static boolean consumeNewDropOrInventory(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            Item item,
            Set<Integer> existingEntityIds
    ) {
        AABB area = new AABB(pos).inflate(1.5);
        List<ItemEntity> drops = level.getEntitiesOfClass(
                ItemEntity.class,
                area,
                entity -> !existingEntityIds.contains(entity.getId())
                        && entity.isAlive()
                        && entity.getItem().is(item)
        );
        for (ItemEntity entity : drops) {
            ItemStack stack = entity.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            stack.shrink(1);
            if (stack.isEmpty()) {
                entity.discard();
            } else {
                entity.setItem(stack);
            }
            return true;
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item) && !stack.isEmpty()) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    player.getInventory().setItem(slot, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }

    // ==================== 工具方法 ====================

    /**
     * Re-applies server-owned bounds after shape and API callbacks.
     */
    private static List<BlockPos> sanitizeTargets(
            ChainActionContext context,
            List<BlockPos> candidates,
            MinerConfig config
    ) {
        return sanitizeTargets(
                context,
                candidates,
                getMaxTargetCount(context, config),
                getMaxTargetDistance(context, config),
                ignored -> true
        );
    }

    private static List<BlockPos> sanitizeTargets(
            ChainActionContext context,
            Collection<BlockPos> candidates,
            int maxCount,
            int maxDistance,
            java.util.function.Predicate<BlockPos> validator
    ) {
        return sanitizeTargets(
                context,
                candidates,
                maxCount,
                maxDistance,
                validator,
                false
        );
    }

    private static List<BlockPos> sanitizeTargets(
            ChainActionContext context,
            Collection<BlockPos> candidates,
            int maxCount,
            int maxDistance,
            java.util.function.Predicate<BlockPos> validator,
            boolean preserveDuplicates
    ) {
        if (candidates == null || candidates.isEmpty() || maxCount <= 0) {
            return List.of();
        }

        Level level = context.getLevel();
        BlockPos origin = context.getOriginPos();
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> seen = preserveDuplicates ? null : new LinkedHashSet<>();
        int inspected = 0;
        int inspectionLimit = Math.min(40_960, Math.max(256, maxCount * 4));

        for (BlockPos pos : candidates) {
            if (++inspected > inspectionLimit) {
                break;
            }
            if (pos == null) {
                continue;
            }
            long dx = Math.abs((long) pos.getX() - origin.getX());
            long dy = Math.abs((long) pos.getY() - origin.getY());
            long dz = Math.abs((long) pos.getZ() - origin.getZ());
            if (Math.max(dx, Math.max(dy, dz)) > maxDistance
                    || !level.hasChunkAt(pos)
                    || !validator.test(pos)) {
                continue;
            }
            BlockPos immutablePos = pos.immutable();
            if (seen != null && !seen.add(immutablePos)) {
                continue;
            }
            result.add(immutablePos);
            if (result.size() >= maxCount) {
                break;
            }
        }
        return List.copyOf(result);
    }

    /**
     * The clicked target is the authorization gate for a derived interaction.
     * API listeners may filter it out, in which case the chain must not act on
     * neighboring targets. If they only reorder it, restore it to the front.
     */
    private static List<BlockPos> requireOriginFirst(
            ChainActionContext context,
            List<BlockPos> targets
    ) {
        return OriginDispatchPolicy.authorizeAndOrder(
                context.getOriginPos(),
                targets,
                context.isOriginAlreadyHandled()
        );
    }

    private static int getMaxTargetCount(ChainActionContext context, MinerConfig config) {
        int configured = context.isCreativeMode() ? config.maxBlocksCreative : config.maxBlocks;
        int requested = context.getMaxCount() > 0 ? context.getMaxCount() : configured;
        return Math.max(0, Math.min(requested, 10_240));
    }

    private static int getMaxTargetDistance(ChainActionContext context, MinerConfig config) {
        int requested = context.getMaxDistance() > 0 ? context.getMaxDistance() : config.maxDistance;
        return Math.max(0, Math.min(requested, 128));
    }

    /**
     * 检查激活条件
     * <p>始终使用按住按键激活模式</p>
     */
    private static boolean checkActivationConditions(ChainActionContext context, MinerConfig config) {
        ServerPlayer player = context.getPlayer();

        // 只检查按键按住状态
        return context.isActivationVerified()
                || MiningStateManager.isHoldingKey(player);
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
