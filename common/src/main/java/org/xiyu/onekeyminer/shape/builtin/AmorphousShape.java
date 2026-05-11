package org.xiyu.onekeyminer.shape.builtin;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.shape.ChainShape;
import org.xiyu.onekeyminer.shape.ShapeContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Free-form BFS over connected matching blocks.
 */
public class AmorphousShape implements ChainShape {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "amorphous");

    private static final BlockPos[] ORTHOGONAL_OFFSETS = {
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
            new BlockPos(0, 1, 0), new BlockPos(0, -1, 0),
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1)
    };

    private static final BlockPos[] DIAGONAL_OFFSETS;
    private static final long TIMEOUT_MS = 2000;
    private static final int MAX_ITERATIONS = 10000;

    static {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        offsets.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        DIAGONAL_OFFSETS = offsets.toArray(new BlockPos[0]);
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public String getTranslationKey() {
        return "onekeyminer.shape.amorphous";
    }

    @Override
    public List<BlockPos> collectBlocks(ShapeContext context) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        BlockPos originPos = context.getOriginPos();
        Level level = context.getLevel();
        int maxBlocks = context.getMaxBlocks();
        int maxDistance = context.getMaxDistance();
        BlockPos[] offsets = context.isAllowDiagonal() ? DIAGONAL_OFFSETS : ORTHOGONAL_OFFSETS;

        visited.add(originPos);
        for (BlockPos offset : offsets) {
            BlockPos neighbor = originPos.offset(offset);
            if (visited.add(neighbor)) {
                BlockState neighborState = level.getBlockState(neighbor);
                if (context.isMatchingBlock(neighborState)) {
                    queue.add(neighbor);
                }
            }
        }

        long start = System.currentTimeMillis();
        int iterations = 0;
        while (!queue.isEmpty() && result.size() < maxBlocks && iterations < MAX_ITERATIONS) {
            if (System.currentTimeMillis() - start > TIMEOUT_MS) {
                break;
            }
            iterations++;
            BlockPos current = queue.poll();
            if (current.distManhattan(originPos) > maxDistance) {
                continue;
            }
            result.add(current);

            for (BlockPos offset : offsets) {
                BlockPos neighbor = current.offset(offset);
                if (neighbor.distManhattan(originPos) <= maxDistance && visited.add(neighbor)) {
                    BlockState neighborState = level.getBlockState(neighbor);
                    if (context.isMatchingBlock(neighborState)) {
                        queue.add(neighbor);
                    }
                }
            }
        }
        return result;
    }
}
