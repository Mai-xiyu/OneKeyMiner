package org.xiyu.onekeyminer.shape.builtin;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.shape.ChainShape;
import org.xiyu.onekeyminer.shape.ShapeContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Compatibility shape matching the old ShapeMode.COLUMN behavior.
 */
public class ColumnShape implements ChainShape {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "column");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public String getTranslationKey() {
        return "onekeyminer.shape.column";
    }

    @Override
    public List<BlockPos> collectBlocks(ShapeContext context) {
        List<BlockPos> result = new ArrayList<>();
        BlockPos origin = context.getOriginPos();
        Level level = context.getLevel();

        for (int y = 1; y <= context.getMaxDistance() && result.size() < context.getMaxBlocks(); y++) {
            BlockPos pos = origin.above(y);
            BlockState state = level.getBlockState(pos);
            if (context.isMatchingBlock(state)) {
                result.add(pos);
            } else {
                break;
            }
        }

        for (int y = 1; y <= context.getMaxDistance() && result.size() < context.getMaxBlocks(); y++) {
            BlockPos pos = origin.below(y);
            BlockState state = level.getBlockState(pos);
            if (context.isMatchingBlock(state)) {
                result.add(pos);
            } else {
                break;
            }
        }
        return result;
    }
}
