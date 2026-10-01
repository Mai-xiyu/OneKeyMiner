package org.xiyu.onekeyminer.legacy.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Deterministic breadth-first traversal used by the legacy adapters.
 *
 * <p>The matcher is evaluated only for positions that are reached from a
 * matching position.  This means a non-matching block stops traversal in that
 * direction, which is the connected-component behavior expected by chain
 * mining.  Positions are marked when queued, so diagonal neighborhoods cannot
 * enqueue the same coordinate repeatedly.</p>
 */
public final class LegacyChainSearch {
    private static final int[][] CARDINAL_OFFSETS = {
            {0, 1, 0}, {0, -1, 0}, {0, 0, -1},
            {0, 0, 1}, {1, 0, 0}, {-1, 0, 0}
    };

    private static final int[][] DIAGONAL_OFFSETS = buildDiagonalOffsets();

    private LegacyChainSearch() {
    }

    @FunctionalInterface
    public interface PositionMatcher {
        boolean matches(LegacyPosition position);
    }

    /**
     * Finds a connected component using a bounded breadth-first search.
     *
     * @param start starting coordinate
     * @param maxBlocks maximum number of matching positions to return
     * @param maxDistance maximum breadth-first search distance from {@code start}
     * @param includeDiagonal whether all 26 neighboring offsets are allowed
     * @param matcher version-specific world/state predicate
     * @return positions in stable breadth-first order
     * @complexity O(V) time and O(V) space, where V is the visited bound
     */
    public static List<LegacyPosition> findConnected(
            LegacyPosition start,
            int maxBlocks,
            int maxDistance,
            boolean includeDiagonal,
            PositionMatcher matcher) {
        if (start == null || matcher == null || maxBlocks <= 0 || maxDistance < 0) {
            return Collections.emptyList();
        }

        long calculatedLimit = Math.max(64L, (long) maxBlocks * 8L);
        int maxVisited = calculatedLimit > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) calculatedLimit;
        return findConnected(start, maxBlocks, maxDistance, includeDiagonal,
                maxVisited, matcher);
    }

    /**
     * Variant with an explicit visited-node limit for callers that need a
     * tighter server tick budget.
     */
    public static List<LegacyPosition> findConnected(
            LegacyPosition start,
            int maxBlocks,
            int maxDistance,
            boolean includeDiagonal,
            int maxVisited,
            PositionMatcher matcher) {
        if (start == null || matcher == null || maxBlocks <= 0
                || maxDistance < 0 || maxVisited <= 0) {
            return Collections.emptyList();
        }

        List<LegacyPosition> result = new ArrayList<LegacyPosition>(Math.min(maxBlocks, 64));
        Set<LegacyPosition> visited = new HashSet<LegacyPosition>();
        Queue<Node> pending = new ArrayDeque<Node>();
        pending.add(new Node(start, 0));
        visited.add(start);

        int examined = 0;
        while (!pending.isEmpty() && result.size() < maxBlocks && examined < maxVisited) {
            Node node = pending.remove();
            examined++;

            if (node.depth > maxDistance) {
                continue;
            }
            if (!matcher.matches(node.position)) {
                continue;
            }

            result.add(node.position);
            if (result.size() >= maxBlocks) {
                break;
            }

            if (node.depth >= maxDistance) {
                continue;
            }

            int[][] offsets = includeDiagonal ? DIAGONAL_OFFSETS : CARDINAL_OFFSETS;
            for (int[] offset : offsets) {
                LegacyPosition neighbor = node.position.offset(offset[0], offset[1], offset[2]);
                if (visited.add(neighbor)) {
                    pending.add(new Node(neighbor, node.depth + 1));
                }
            }
        }
        return result;
    }

    private static int[][] buildDiagonalOffsets() {
        List<int[]> offsets = new ArrayList<int[]>(26);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    offsets.add(new int[]{dx, dy, dz});
                }
            }
        }
        return offsets.toArray(new int[offsets.size()][]);
    }

    private static final class Node {
        private final LegacyPosition position;
        private final int depth;

        private Node(LegacyPosition position, int depth) {
            this.position = position;
            this.depth = depth;
        }
    }
}
