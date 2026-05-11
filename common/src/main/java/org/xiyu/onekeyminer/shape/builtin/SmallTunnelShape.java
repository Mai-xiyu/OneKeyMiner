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
 * One-block-wide straight tunnel.
 */
public class SmallTunnelShape implements ChainShape {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "small_tunnel");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public String getTranslationKey() {
        return "onekeyminer.shape.small_tunnel";
    }

    @Override
    public List<BlockPos> collectBlocks(ShapeContext context) {
        List<BlockPos> result = new ArrayList<>();
        Direction direction = context.getTunnelDirection();
        BlockPos current = context.getOriginPos();
        Level level = context.getLevel();

        for (int i = 1; i <= context.getMaxDistance() && result.size() < context.getMaxBlocks(); i++) {
            current = current.relative(direction);
            BlockState state = level.getBlockState(current);
            if (context.isMatchingBlock(state)) {
                result.add(current);
            } else {
                break;
            }
        }
        return result;
    }

    @Override
    public boolean requiresDirection() {
        return true;
    }
}
