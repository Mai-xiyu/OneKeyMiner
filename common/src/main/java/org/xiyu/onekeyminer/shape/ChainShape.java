package org.xiyu.onekeyminer.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Search shape used by chain mining.
 */
public interface ChainShape {
    Identifier getId();

    String getTranslationKey();

    List<BlockPos> collectBlocks(ShapeContext context);

    default List<BlockPos> getPreviewPositions(ShapeContext context) {
        return collectBlocks(context);
    }

    default boolean requiresDirection() {
        return false;
    }
}
