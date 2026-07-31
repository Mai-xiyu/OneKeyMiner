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
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "cube");
    private static final int MIN_SCAN_BUDGET = 256;
    private static final int MAX_SCAN_BUDGET = 10_000;
    private static final int SCANS_PER_RESULT = 64;

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
        int maxBlocks = Math.max(0, context.getMaxBlocks());
        int scanBudget = Math.min(
                MAX_SCAN_BUDGET,
                Math.max(MIN_SCAN_BUDGET, maxBlocks * SCANS_PER_RESULT)
        );
        int scanned = 0;

        // Scan Chebyshev shells nearest-first without inspecting millions of
        // positions or loading remote chunks for a large configured radius.
        for (int distance = 1;
             distance <= radius && result.size() < maxBlocks && scanned < scanBudget;
             distance++) {
            for (int x = -distance;
                 x <= distance && result.size() < maxBlocks && scanned < scanBudget;
                 x++) {
                for (int y = -distance;
                     y <= distance && result.size() < maxBlocks && scanned < scanBudget;
                     y++) {
                    for (int z = -distance;
                         z <= distance && result.size() < maxBlocks && scanned < scanBudget;
                         z++) {
                        if (Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.abs(z))
                                != distance) {
                            continue;
                        }
                        scanned++;
                        BlockPos pos = origin.offset(x, y, z);
                        if (!level.hasChunkAt(pos)) {
                            continue;
                        }
                        BlockState state = level.getBlockState(pos);
                        if (context.isMatchingBlock(state)) {
                            result.add(pos.immutable());
                        }
                    }
                }
            }
        }
        return result;
    }
}
