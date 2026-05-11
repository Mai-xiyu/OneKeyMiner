package org.xiyu.onekeyminer.shape.builtin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.shape.ChainShape;
import org.xiyu.onekeyminer.shape.ShapeContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Solid 3x3 tunnel extending along the player's look direction.
 */
public class LargeTunnelShape implements ChainShape {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "large_tunnel");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public String getTranslationKey() {
        return "onekeyminer.shape.large_tunnel";
    }

    @Override
    public List<BlockPos> collectBlocks(ShapeContext context) {
        List<BlockPos> result = new ArrayList<>();
        Direction direction = context.getTunnelDirection();
        BlockPos originPos = context.getOriginPos();
        Level level = context.getLevel();
        Direction[] axes = getCrossAxes(direction);

        for (int depth = 0; depth <= context.getMaxDistance() && result.size() < context.getMaxBlocks(); depth++) {
            BlockPos center = originPos.relative(direction, depth);
            boolean anyMatch = false;
            for (int a = -1; a <= 1 && result.size() < context.getMaxBlocks(); a++) {
                for (int b = -1; b <= 1 && result.size() < context.getMaxBlocks(); b++) {
                    BlockPos pos = center.relative(axes[0], a).relative(axes[1], b);
                    if (pos.equals(originPos)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (context.isMatchingBlock(state)) {
                        result.add(pos);
                        anyMatch = true;
                    }
                }
            }
            if (depth > 0 && !anyMatch) {
                break;
            }
        }
        return result;
    }

    private Direction[] getCrossAxes(Direction tunnelDir) {
        return switch (tunnelDir) {
            case NORTH, SOUTH -> new Direction[]{Direction.EAST, Direction.UP};
            case EAST, WEST -> new Direction[]{Direction.SOUTH, Direction.UP};
            case UP, DOWN -> new Direction[]{Direction.EAST, Direction.SOUTH};
        };
    }

    @Override
    public boolean requiresDirection() {
        return true;
    }
}
