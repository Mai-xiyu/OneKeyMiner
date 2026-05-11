package org.xiyu.onekeyminer.shape.builtin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.shape.ChainShape;
import org.xiyu.onekeyminer.shape.ShapeContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Single 3x3 plane perpendicular to the player's look direction.
 */
public class SmallSquareShape implements ChainShape {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "small_square");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public String getTranslationKey() {
        return "onekeyminer.shape.small_square";
    }

    @Override
    public List<BlockPos> collectBlocks(ShapeContext context) {
        List<BlockPos> result = new ArrayList<>();
        Direction[] axes = getCrossAxes(context.getTunnelDirection());
        BlockPos originPos = context.getOriginPos();
        Level level = context.getLevel();

        for (int a = -1; a <= 1 && result.size() < context.getMaxBlocks(); a++) {
            for (int b = -1; b <= 1 && result.size() < context.getMaxBlocks(); b++) {
                BlockPos pos = originPos.relative(axes[0], a).relative(axes[1], b);
                if (pos.equals(originPos)) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (context.isMatchingBlock(state)) {
                    result.add(pos);
                }
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
