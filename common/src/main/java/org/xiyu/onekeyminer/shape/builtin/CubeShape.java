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
    private static final int MAX_SCANNED_POSITIONS = 100_000;
    private static final int MIN_SCAN_BUDGET = 4_096;
    private static final int SCANS_PER_RESULT = 256;

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
        int scanBudget = Math.min(
                MAX_SCANNED_POSITIONS,
                Math.max(MIN_SCAN_BUDGET, context.getMaxBlocks() * SCANS_PER_RESULT)
        );
        int scanned = 0;

        scan:
        for (int distance = 1; distance <= radius; distance++) {
            for (int x = -distance; x <= distance; x++) {
                for (int y = -distance; y <= distance; y++) {
                    for (int sign = -1; sign <= 1; sign += 2) {
                        if (scanned++ >= scanBudget || result.size() >= context.getMaxBlocks()) break scan;
                        consider(context, level, result, origin.offset(x, y, sign * distance));
                    }
                }
            }
            for (int x = -distance; x <= distance; x++) {
                for (int z = -distance + 1; z <= distance - 1; z++) {
                    for (int sign = -1; sign <= 1; sign += 2) {
                        if (scanned++ >= scanBudget || result.size() >= context.getMaxBlocks()) break scan;
                        consider(context, level, result, origin.offset(x, sign * distance, z));
                    }
                }
            }
            for (int y = -distance + 1; y <= distance - 1; y++) {
                for (int z = -distance + 1; z <= distance - 1; z++) {
                    for (int sign = -1; sign <= 1; sign += 2) {
                        if (scanned++ >= scanBudget || result.size() >= context.getMaxBlocks()) break scan;
                        consider(context, level, result, origin.offset(sign * distance, y, z));
                    }
                }
            }
        }
        return result;
    }

    private static void consider(
            ShapeContext context,
            Level level,
            List<BlockPos> result,
            BlockPos pos
    ) {
        if (!level.hasChunkAt(pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (context.isMatchingBlock(state)) {
            result.add(pos);
        }
    }
}
