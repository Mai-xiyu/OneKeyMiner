package org.xiyu.onekeyminer.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Search shape used by chain mining.
 */
public interface ChainShape {
    ResourceLocation getId();

    String getTranslationKey();

    List<BlockPos> collectBlocks(ShapeContext context);

    default List<BlockPos> getPreviewPositions(ShapeContext context) {
        return collectBlocks(context);
    }

    default boolean requiresDirection() {
        return false;
    }
}
