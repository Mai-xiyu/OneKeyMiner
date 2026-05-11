package org.xiyu.onekeyminer.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiPredicate;

/**
 * Immutable context passed to chain shape implementations.
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

    public Level getLevel() {
        return level;
    }

    public BlockPos getOriginPos() {
        return originPos;
    }

    public BlockState getOriginState() {
        return originState;
    }

    public Direction getPlayerFacing() {
        return playerFacing;
    }

    public Direction getPlayerLookingVertical() {
        return playerLookingVertical;
    }

    public int getMaxBlocks() {
        return maxBlocks;
    }

    public int getMaxDistance() {
        return maxDistance;
    }

    public boolean isAllowDiagonal() {
        return allowDiagonal;
    }

    public boolean isMatchingBlock(BlockState target) {
        if (blockMatcher != null) {
            return blockMatcher.test(originState, target);
        }
        return !target.isAir() && target.getBlock() == originState.getBlock();
    }

    public Direction getTunnelDirection() {
        if (playerLookingVertical != null) {
            return playerLookingVertical;
        }
        return playerFacing != null ? playerFacing : Direction.NORTH;
    }

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
