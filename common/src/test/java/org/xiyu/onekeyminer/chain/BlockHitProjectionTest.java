package org.xiyu.onekeyminer.chain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BlockHitProjectionTest {
    @Test
    void preservesBlockLocalCoordinate() {
        assertEquals(
                -3.75d,
                BlockHitProjection.projectCoordinate(10.25d, 10, -4),
                0.0d
        );
    }

    @Test
    void clampsUntrustedCoordinateToTargetBlock() {
        assertEquals(7.0d, BlockHitProjection.projectCoordinate(-100.0d, 0, 7), 0.0d);
        assertEquals(9.0d, BlockHitProjection.projectCoordinate(500.0d, 0, 8), 0.0d);
        assertEquals(10.0d, BlockHitProjection.projectCoordinate(2.5d, 0, 9), 0.0d);
    }

    @Test
    void preservesPlacementSupportOffset() {
        assertEquals(-5, BlockHitProjection.projectSupportCoordinate(10, 11, -4));
        assertEquals(21, BlockHitProjection.projectSupportCoordinate(4, 3, 20));
        assertEquals(7, BlockHitProjection.projectSupportCoordinate(8, 8, 7));
    }
}
