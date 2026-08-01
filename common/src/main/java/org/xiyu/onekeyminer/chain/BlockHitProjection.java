package org.xiyu.onekeyminer.chain;

/** Safe projection of an original block-local hit coordinate to a derived target. */
final class BlockHitProjection {
    private BlockHitProjection() {
    }

    static double projectCoordinate(
            double originalHitCoordinate,
            int originalBlockCoordinate,
            int targetBlockCoordinate
    ) {
        double localCoordinate = originalHitCoordinate - originalBlockCoordinate;
        if (!Double.isFinite(localCoordinate)) {
            localCoordinate = 0.5d;
        }
        return targetBlockCoordinate + Math.max(0.0d, Math.min(1.0d, localCoordinate));
    }

    static int projectSupportCoordinate(
            int originalSupportCoordinate,
            int originalTargetCoordinate,
            int targetCoordinate
    ) {
        long offset = (long) originalSupportCoordinate - originalTargetCoordinate;
        int adjacentOffset = (int) Math.max(-1L, Math.min(1L, offset));
        return Math.addExact(targetCoordinate, adjacentOffset);
    }
}
