package org.xiyu.onekeyminer.chain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 链式操作上下文
 * 
 * <p>封装链式操作所需的所有上下文信息，作为操作执行的输入参数。
 * 使用 Builder 模式构建，确保所有必需参数都被正确设置。</p>
 * 
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.21.9
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
    
    // ========== 可选参数 ==========
    
    /** 最大操作数量（覆盖配置） */
    private final int maxCount;
    
    /** 最大距离（覆盖配置） */
    private final int maxDistance;
    
    /** 是否允许对角线搜索 */
    private final boolean allowDiagonal;
    
    /** 是否跳过权限检查 */
    private final boolean skipPermissionCheck;
    
    /**
     * 私有构造函数，使用 Builder 构建
     */
    private ChainActionContext(Builder builder) {
        this.player = builder.player;
        this.level = builder.level;
        this.originPos = builder.originPos;
        this.originState = builder.originState;
        this.actionType = builder.actionType;
        this.heldItem = builder.heldItem;
        this.hand = builder.hand;
        this.interactionOverride = builder.interactionOverride;
        this.maxCount = builder.maxCount;
        this.maxDistance = builder.maxDistance;
        this.allowDiagonal = builder.allowDiagonal;
        this.skipPermissionCheck = builder.skipPermissionCheck;
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
        return heldItem;
    }
    
    public InteractionHand getHand() {
        return hand;
    }

    public InteractionOverride getInteractionOverride() {
        return interactionOverride;
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
        private int maxCount = -1;  // -1 表示使用配置值
        private int maxDistance = -1;
        private boolean allowDiagonal = true;
        private boolean skipPermissionCheck = false;
        
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
            if (player == null) {
                throw new IllegalStateException("Player 不能为空");
            }
            if (level == null) {
                throw new IllegalStateException("Level 不能为空");
            }
            if (originPos == null) {
                throw new IllegalStateException("OriginPos 不能为空");
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
        GENERIC
    }
}
