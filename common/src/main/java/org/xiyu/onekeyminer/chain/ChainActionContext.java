package org.xiyu.onekeyminer.chain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.xiyu.onekeyminer.api.OneKeyMinerAPI;

import java.util.Objects;
import java.util.UUID;

/**
 * 链式操作上下文
 *
 * <p>封装链式操作所需的所有上下文信息，作为操作执行的输入参数。
 * 使用 Builder 模式构建，确保所有必需参数都被正确设置。</p>
 *
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.21.5
 */
public final class ChainActionContext {

    // ========== 核心参数 ==========

    /** 执行操作的玩家 */
    private final ServerPlayer player;

    /** 世界实例 */
    private final Level level;

    /** 起始位置 */
    private final BlockPos originPos;

    /** 起始方块状态（可能为 null，用于非方块操作） */
    private final BlockState originState;

    /** 操作类型 */
    private final ChainActionType actionType;

    /** 使用的物品（工具/种子等） */
    private final ItemStack heldItem;

    /** 交互使用的手 */
    private final InteractionHand hand;

    /** 交互类型覆盖（仅用于交互操作） */
    private final InteractionOverride interactionOverride;

    /** Exact entity selected by the player for entity interactions. */
    private final UUID originEntityId;

    /** Original click geometry for block interactions. */
    private final BlockHitResult blockHitResult;

    // ========== 可选参数 ==========

    /** 最大操作数量（覆盖配置） */
    private final int maxCount;

    /** 最大距离（覆盖配置） */
    private final int maxDistance;

    /** 是否允许对角线搜索 */
    private final boolean allowDiagonal;

    /** 是否跳过权限检查 */
    private final boolean skipPermissionCheck;

    /** Loader event already observed activation for this completed break. */
    private final boolean activationVerified;

    /**
     * The original vanilla/loader interaction completed successfully before
     * this context was dispatched. It remains an authorization token but must
     * not be simulated a second time.
     */
    private final boolean originAlreadyHandled;

    /** Exact API rule selected before the original interaction mutated state. */
    private final OneKeyMinerAPI.ToolActionRule matchedToolActionRule;

    /**
     * 私有构造函数，使用 Builder 构建
     */
    private ChainActionContext(Builder builder) {
        this.player = builder.player;
        this.level = builder.level;
        this.originPos = builder.originPos.immutable();
        this.originState = builder.originState;
        this.actionType = builder.actionType;
        this.heldItem = builder.heldItem.copy();
        this.hand = builder.hand;
        this.interactionOverride = builder.interactionOverride;
        this.originEntityId = builder.originEntityId;
        this.blockHitResult = builder.blockHitResult;
        this.maxCount = builder.maxCount > 0 ? Math.min(builder.maxCount, 10_240) : -1;
        this.maxDistance = builder.maxDistance > 0 ? Math.min(builder.maxDistance, 128) : -1;
        this.allowDiagonal = builder.allowDiagonal;
        this.skipPermissionCheck = builder.skipPermissionCheck;
        this.activationVerified = builder.activationVerified;
        this.originAlreadyHandled = builder.originAlreadyHandled;
        this.matchedToolActionRule = builder.matchedToolActionRule;
    }

    // ========== Getters ==========

    public ServerPlayer getPlayer() {
        return player;
    }

    public Level getLevel() {
        return level;
    }

    public BlockPos getOriginPos() {
        return originPos;
    }

    public BlockState getOriginState() {
        return originState;
    }

    public ChainActionType getActionType() {
        return actionType;
    }

    public ItemStack getHeldItem() {
        return heldItem.copy();
    }

    public InteractionHand getHand() {
        return hand;
    }

    public InteractionOverride getInteractionOverride() {
        return interactionOverride;
    }

    public UUID getOriginEntityId() {
        return originEntityId;
    }

    public BlockHitResult getBlockHitResult() {
        return blockHitResult;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public int getMaxDistance() {
        return maxDistance;
    }

    public boolean isAllowDiagonal() {
        return allowDiagonal;
    }

    /**
     * @deprecated Permission checks are always enforced by the authoritative
     * server interaction path. This compatibility flag is ignored.
     */
    @Deprecated(forRemoval = false)
    public boolean isSkipPermissionCheck() {
        return skipPermissionCheck;
    }

    /**
     * 检查玩家是否处于创造模式
     *
     * @return 如果是创造模式返回 true
     */
    public boolean isCreativeMode() {
        return player.isCreative();
    }

    /**
     * 创建新的 Builder 实例
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 快速创建挖掘操作上下文
     *
     * @param player 玩家
     * @param level 世界
     * @param pos 位置
     * @param state 方块状态
     * @return 上下文实例
     */
    public static ChainActionContext forMining(
            ServerPlayer player,
            Level level,
            BlockPos pos,
            BlockState state
    ) {
        return builder()
                .player(player)
                .level(level)
                .originPos(pos)
                .originState(state)
                .actionType(ChainActionType.MINING)
                .heldItem(player.getMainHandItem())
                .hand(InteractionHand.MAIN_HAND)
                .build();
    }

    static ChainActionContext forVerifiedMining(
            ServerPlayer player,
            Level level,
            BlockPos pos,
            BlockState state,
            ItemStack originalTool
    ) {
        Builder builder = builder()
                .player(player)
                .level(level)
                .originPos(pos)
                .originState(state)
                .actionType(ChainActionType.MINING)
                .heldItem(originalTool)
                .hand(InteractionHand.MAIN_HAND);
        builder.activationVerified = true;
        return builder.build();
    }

    boolean isActivationVerified() {
        return activationVerified;
    }

    boolean isOriginAlreadyHandled() {
        return originAlreadyHandled;
    }

    OneKeyMinerAPI.ToolActionRule getMatchedToolActionRule() {
        return matchedToolActionRule;
    }

    static ChainActionContext forCompletedBlockUse(
            ServerPlayer player,
            Level level,
            BlockPos originPos,
            BlockState originState,
            ChainActionType actionType,
            ItemStack originalItem,
            InteractionHand hand,
            InteractionOverride interactionOverride,
            OneKeyMinerAPI.ToolActionRule matchedToolActionRule,
            BlockHitResult hitResult
    ) {
        if (actionType != ChainActionType.INTERACTION
                && actionType != ChainActionType.PLANTING) {
            throw new IllegalArgumentException(
                    "completed block use must be interaction or planting"
            );
        }
        Builder builder = builder()
                .player(player)
                .level(level)
                .originPos(originPos)
                .originState(originState)
                .actionType(actionType)
                .heldItem(originalItem)
                .hand(hand)
                .interactionOverride(interactionOverride)
                .blockHitResult(hitResult);
        builder.activationVerified = true;
        builder.originAlreadyHandled = true;
        builder.matchedToolActionRule = matchedToolActionRule;
        return builder.build();
    }

    static ChainActionContext forCompletedEntityUse(
            ServerPlayer player,
            Level level,
            BlockPos originPos,
            BlockState originState,
            ItemStack originalItem,
            InteractionHand hand,
            UUID originEntityId,
            OneKeyMinerAPI.ToolActionRule matchedToolActionRule
    ) {
        Builder builder = builder()
                .player(player)
                .level(level)
                .originPos(originPos)
                .originState(originState)
                .actionType(ChainActionType.INTERACTION)
                .heldItem(originalItem)
                .hand(hand)
                .interactionOverride(InteractionOverride.SHEARING)
                .originEntityId(originEntityId);
        builder.activationVerified = true;
        builder.originAlreadyHandled = true;
        builder.matchedToolActionRule = matchedToolActionRule;
        return builder.build();
    }

    /**
     * 快速创建交互操作上下文
     *
     * @param player 玩家
     * @param level 世界
     * @param pos 位置
     * @param state 方块状态
     * @param hand 交互手
     * @return 上下文实例
     */
    public static ChainActionContext forInteraction(
            ServerPlayer player,
            Level level,
            BlockPos pos,
            BlockState state,
            InteractionHand hand
    ) {
        return builder()
                .player(player)
                .level(level)
                .originPos(pos)
                .originState(state)
                .actionType(ChainActionType.INTERACTION)
                .heldItem(player.getItemInHand(hand))
                .hand(hand)
                .build();
    }

    /**
     * 快速创建种植操作上下文
     *
     * @param player 玩家
     * @param level 世界
     * @param pos 位置
     * @param hand 交互手
     * @return 上下文实例
     */
    public static ChainActionContext forPlanting(
            ServerPlayer player,
            Level level,
            BlockPos pos,
            InteractionHand hand
    ) {
        return builder()
                .player(player)
                .level(level)
                .originPos(pos)
                .originState(level.getBlockState(pos))
                .actionType(ChainActionType.PLANTING)
                .heldItem(player.getItemInHand(hand))
                .hand(hand)
                .build();
    }

    /**
     * 快速创建收割操作上下文
     *
     * @param player 玩家
     * @param level 世界
     * @param pos 位置
     * @param hand 交互手
     * @return 上下文实例
     */
    public static ChainActionContext forHarvesting(
            ServerPlayer player,
            Level level,
            BlockPos pos,
            InteractionHand hand
    ) {
        return builder()
                .player(player)
                .level(level)
                .originPos(pos)
                .originState(level.getBlockState(pos))
                .actionType(ChainActionType.HARVESTING)
                .heldItem(player.getItemInHand(hand))
                .hand(hand)
                .build();
    }

    /**
     * Context Builder
     */
    public static final class Builder {
        private ServerPlayer player;
        private Level level;
        private BlockPos originPos;
        private BlockState originState;
        private ChainActionType actionType = ChainActionType.MINING;
        private ItemStack heldItem = ItemStack.EMPTY;
        private InteractionHand hand = InteractionHand.MAIN_HAND;
        private InteractionOverride interactionOverride;
        private UUID originEntityId;
        private BlockHitResult blockHitResult;
        private int maxCount = -1;  // -1 表示使用配置值
        private int maxDistance = -1;
        private boolean allowDiagonal = true;
        private boolean skipPermissionCheck = false;
        private boolean activationVerified = false;
        private boolean originAlreadyHandled = false;
        private OneKeyMinerAPI.ToolActionRule matchedToolActionRule;

        public Builder player(ServerPlayer player) {
            this.player = player;
            return this;
        }

        public Builder level(Level level) {
            this.level = level;
            return this;
        }

        public Builder originPos(BlockPos pos) {
            this.originPos = pos;
            return this;
        }

        public Builder originState(BlockState state) {
            this.originState = state;
            return this;
        }

        public Builder actionType(ChainActionType type) {
            this.actionType = type;
            return this;
        }

        public Builder heldItem(ItemStack item) {
            this.heldItem = item;
            return this;
        }

        public Builder hand(InteractionHand hand) {
            this.hand = hand;
            return this;
        }

        public Builder interactionOverride(InteractionOverride override) {
            this.interactionOverride = override;
            return this;
        }

        public Builder originEntityId(UUID entityId) {
            this.originEntityId = entityId;
            return this;
        }

        public Builder blockHitResult(BlockHitResult hitResult) {
            this.blockHitResult = hitResult;
            return this;
        }

        public Builder maxCount(int maxCount) {
            this.maxCount = maxCount;
            return this;
        }

        public Builder maxDistance(int maxDistance) {
            this.maxDistance = maxDistance;
            return this;
        }

        public Builder allowDiagonal(boolean allow) {
            this.allowDiagonal = allow;
            return this;
        }

        /**
         * @deprecated Permission checks cannot be bypassed. The value is retained
         * only for source compatibility and has no effect on execution.
         */
        @Deprecated(forRemoval = false)
        public Builder skipPermissionCheck(boolean skip) {
            this.skipPermissionCheck = skip;
            return this;
        }

        /**
         * 构建上下文实例
         *
         * @return 上下文实例
         * @throws IllegalStateException 如果必需参数未设置
         */
        public ChainActionContext build() {
            Objects.requireNonNull(player, "player");
            Objects.requireNonNull(level, "level");
            Objects.requireNonNull(originPos, "originPos");
            Objects.requireNonNull(originState, "originState");
            Objects.requireNonNull(actionType, "actionType");
            Objects.requireNonNull(heldItem, "heldItem");
            Objects.requireNonNull(hand, "hand");
            if (player.level() != level) {
                throw new IllegalArgumentException("level must be the player's current server level");
            }
            return new ChainActionContext(this);
        }
    }

    /**
     * 交互类型覆盖枚举（用于自定义工具规则）
     */
    public enum InteractionOverride {
        SHEARING,
        TILLING,
        STRIPPING,
        PATH_MAKING,
        BRUSHING,
        ITEM_USE,
        GENERIC
    }
}
