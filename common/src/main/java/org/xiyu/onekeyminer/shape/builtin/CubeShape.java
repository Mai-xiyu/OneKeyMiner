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
 * Distance-ordered compatibility volume.
 *
 * <p>The old implementation walked a Chebyshev cube from one corner and could
 * inspect about 17 million positions at radius 128. This implementation uses
 * the same Manhattan distance contract as the common chain engine, visits the
 * nearest positions first, never loads chunks, and has a hard scan budget.</p>
 */
public final class CubeShape implements ChainShape {

    public static final ResourceLocation ID =
            new ResourceLocation(OneKeyMiner.MOD_ID, "cube");

    private static final int MIN_SCAN_BUDGET = 256;
    private static final int MAX_SCAN_BUDGET = 65_536;
    private static final int SCANS_PER_RESULT = 32;

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
        int radius = Math.max(0, context.getMaxDistance());
        int scanBudget = (int) Math.min(
                MAX_SCAN_BUDGET,
                Math.max(
                        (long) MIN_SCAN_BUDGET,
                        (long) Math.max(1, context.getMaxBlocks()) * SCANS_PER_RESULT
                )
        );

        int scanned = 0;
        for (int distance = 1;
             distance <= radius
                     && result.size() < context.getMaxBlocks()
                     && scanned < scanBudget;
             distance++) {
            for (int x = -distance;
                 x <= distance
                         && result.size() < context.getMaxBlocks()
                         && scanned < scanBudget;
                 x++) {
                int remainingAfterX = distance - Math.abs(x);
                for (int y = -remainingAfterX;
                     y <= remainingAfterX
                             && result.size() < context.getMaxBlocks()
                             && scanned < scanBudget;
                     y++) {
                    int zMagnitude = remainingAfterX - Math.abs(y);
                    scanned += visit(
                            context,
                            level,
                            origin.offset(x, y, zMagnitude),
                            result
                    );
                    if (zMagnitude != 0
                            && result.size() < context.getMaxBlocks()
                            && scanned < scanBudget) {
                        scanned += visit(
                                context,
                                level,
                                origin.offset(x, y, -zMagnitude),
                                result
                        );
                    }
                }
            }
        }
        return result;
    }

    private static int visit(
            ShapeContext context,
            Level level,
            BlockPos pos,
            List<BlockPos> result
    ) {
        if (!level.hasChunkAt(pos)) {
            return 1;
        }
        BlockState state = level.getBlockState(pos);
        if (context.isMatchingBlock(state)) {
            result.add(pos);
        }
        return 1;
    }
}
