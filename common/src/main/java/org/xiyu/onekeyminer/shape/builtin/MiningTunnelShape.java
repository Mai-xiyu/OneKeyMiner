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
 * Descending 1x2 stair tunnel.
 */
public class MiningTunnelShape implements ChainShape {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "mining_tunnel");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public String getTranslationKey() {
        return "onekeyminer.shape.mining_tunnel";
    }

    @Override
    public List<BlockPos> collectBlocks(ShapeContext context) {
        List<BlockPos> result = new ArrayList<>();
        Direction horizontal = horizontalFacing(context);
        BlockPos stepPos = context.getOriginPos();
        Level level = context.getLevel();

        for (int step = 1; step <= context.getMaxDistance() && result.size() < context.getMaxBlocks(); step++) {
            stepPos = stepPos.relative(horizontal).below();
            for (int h = 0; h <= 1 && result.size() < context.getMaxBlocks(); h++) {
                BlockPos pos = stepPos.above(h);
                if (pos.equals(context.getOriginPos())) {
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

    private Direction horizontalFacing(ShapeContext context) {
        Direction facing = context.getPlayerFacing();
        return facing == null || facing == Direction.UP || facing == Direction.DOWN ? Direction.NORTH : facing;
    }

    @Override
    public boolean requiresDirection() {
        return true;
    }
}
