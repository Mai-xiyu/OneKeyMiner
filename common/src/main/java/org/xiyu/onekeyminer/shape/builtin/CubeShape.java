package org.xiyu.onekeyminer.shape.builtin;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.shape.ChainShape;
import org.xiyu.onekeyminer.shape.ShapeContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Compatibility shape matching the old ShapeMode.CUBE behavior.
 */
public class CubeShape implements ChainShape {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "cube");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public String getTranslationKey() {
        return "onekeyminer.shape.cube";
    }

    @Override
    public List<BlockPos> collectBlocks(ShapeContext context) {
        List<BlockPos> result = new ArrayList<>();
        BlockPos origin = context.getOriginPos();
        Level level = context.getLevel();
        int radius = context.getMaxDistance();

        for (int x = -radius; x <= radius && result.size() < context.getMaxBlocks(); x++) {
            for (int y = -radius; y <= radius && result.size() < context.getMaxBlocks(); y++) {
                for (int z = -radius; z <= radius && result.size() < context.getMaxBlocks(); z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    BlockPos pos = origin.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (context.isMatchingBlock(state)) {
                        result.add(pos);
                    }
                }
            }
        }
        return result;
    }
}
