package org.xiyu.onekeyminer.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiPredicate;

/**
 * 形状搜索上下文
 * 
 * <p>封装形状搜索所需的所有参数，使用 Builder 模式构建。
 * 传递给 {@link ChainShape#collectBlocks(ShapeContext)} 方法。</p>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.0
 * @since Minecraft 1.20.1
 */
public class ShapeContext {
    
    private final Level level;
    private final BlockPos originPos;
    private final BlockState originState;
    private final Direction playerFacing;
    private final Direction playerLookingVertical;
    private final int maxBlocks;
    private final int maxDistance;
    private final boolean allowDiagonal;
    private final BiPredicate<BlockState, BlockState> blockMatcher;
    
    private ShapeContext(Builder builder) {
        this.level = builder.level;
        this.originPos = builder.originPos;
        this.originState = builder.originState;
        this.playerFacing = builder.playerFacing;
        this.playerLookingVertical = builder.playerLookingVertical;
        this.maxBlocks = builder.maxBlocks;
        this.maxDistance = builder.maxDistance;
        this.allowDiagonal = builder.allowDiagonal;
        this.blockMatcher = builder.blockMatcher;
    }
    
    /** 获取世界实例 */
    public Level getLevel() { return level; }
    
    /** 获取起始方块位置 */
    public BlockPos getOriginPos() { return originPos; }
    
    /** 获取起始方块状态 */
    public BlockState getOriginState() { return originState; }
    
    /** 获取玩家水平朝向（NORTH/SOUTH/EAST/WEST） */
    public Direction getPlayerFacing() { return playerFacing; }
    
    /** 获取玩家垂直视线方向（UP/DOWN，平视时为 null） */
    public Direction getPlayerLookingVertical() { return playerLookingVertical; }
    
    /** 获取最大方块数量限制 */
    public int getMaxBlocks() { return maxBlocks; }
    
    /** 获取最大搜索距离 */
    public int getMaxDistance() { return maxDistance; }
    
    /** 是否允许对角线搜索 */
    public boolean isAllowDiagonal() { return allowDiagonal; }
    
    /**
     * 检查目标方块是否与起始方块匹配
     * 
     * @param target 目标方块状态
     * @return 如果匹配返回 true
     */
    public boolean isMatchingBlock(BlockState target) {
        if (blockMatcher != null) {
            return blockMatcher.test(originState, target);
        }
        return !target.isAir() && target.getBlock() == originState.getBlock();
    }
    
    /**
     * 获取通道延伸方向
     * 
     * <p>优先使用水平朝向，若玩家近乎垂直看则使用垂直方向。
     * 用于通道型形状确定延伸方向。</p>
     * 
     * @return 延伸方向
     */
    public Direction getTunnelDirection() {
        if (playerLookingVertical != null) {
            return playerLookingVertical;
        }
        return playerFacing != null ? playerFacing : Direction.NORTH;
    }
    
    /**
     * Builder 模式构造器
     */
    public static class Builder {
        private Level level;
        private BlockPos originPos;
        private BlockState originState;
        private Direction playerFacing;
        private Direction playerLookingVertical;
        private int maxBlocks = 64;
        private int maxDistance = 16;
        private boolean allowDiagonal = true;
        private BiPredicate<BlockState, BlockState> blockMatcher;
        
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
        
        public Builder playerFacing(Direction facing) {
            this.playerFacing = facing;
            return this;
        }
        
        public Builder playerLookingVertical(Direction vertical) {
            this.playerLookingVertical = vertical;
            return this;
        }
        
        public Builder maxBlocks(int max) {
            this.maxBlocks = max;
            return this;
        }
        
        public Builder maxDistance(int max) {
            this.maxDistance = max;
            return this;
        }
        
        public Builder allowDiagonal(boolean allow) {
            this.allowDiagonal = allow;
            return this;
        }
        
        public Builder blockMatcher(BiPredicate<BlockState, BlockState> matcher) {
            this.blockMatcher = matcher;
            return this;
        }
        
        public ShapeContext build() {
            if (level == null) throw new IllegalStateException("Level must not be null");
            if (originPos == null) throw new IllegalStateException("OriginPos must not be null");
            if (originState == null) throw new IllegalStateException("OriginState must not be null");
            return new ShapeContext(this);
        }
    }
}
