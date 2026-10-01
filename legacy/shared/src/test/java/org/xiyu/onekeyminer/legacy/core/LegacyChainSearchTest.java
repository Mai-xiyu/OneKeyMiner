package org.xiyu.onekeyminer.legacy.core;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyChainSearchTest {
    @Test
    void followsOnlyMatchingConnectedBlocks() {
        LegacyPosition start = new LegacyPosition(0, 64, 0);
        List<LegacyPosition> result = LegacyChainSearch.findConnected(
                start, 16, 16, false,
                position -> Math.abs(position.getX()) <= 1
                        && position.getY() == 64
                        && position.getZ() == 0);

        assertEquals(3, result.size());
        assertEquals(start, result.get(0));
        assertTrue(result.contains(new LegacyPosition(1, 64, 0)));
        assertTrue(result.contains(new LegacyPosition(-1, 64, 0)));
    }

    @Test
    void enforcesBlockLimitAndDoesNotDuplicateDiagonalVisits() {
        LegacyPosition start = new LegacyPosition(-30, 10, -30);
        List<LegacyPosition> result = LegacyChainSearch.findConnected(
                start, 7, 4, true, position -> true);

        Set<LegacyPosition> unique = new HashSet<LegacyPosition>(result);
        assertEquals(7, result.size());
        assertEquals(result.size(), unique.size());
    }

    @Test
    void enforcesSearchDistanceAndSupportsNegativeCoordinates() {
        LegacyPosition start = new LegacyPosition(-2, 100, -3);
        List<LegacyPosition> result = LegacyChainSearch.findConnected(
                start, 64, 1, false,
                position -> position.getX() == -2
                        && position.getZ() == -3
                        && position.getY() >= 99
                        && position.getY() <= 101);

        assertEquals(3, result.size());
        assertFalse(result.contains(new LegacyPosition(-2, 98, -3)));
    }

    @Test
    void doesNotCrossConfiguredSearchDistance() {
        LegacyPosition start = new LegacyPosition(0, 0, 0);
        List<LegacyPosition> result = LegacyChainSearch.findConnected(
                start, 64, 2, false, position -> true);

        assertTrue(result.contains(new LegacyPosition(2, 0, 0)));
        assertFalse(result.contains(new LegacyPosition(3, 0, 0)));
    }

    @Test
    void rejectsInvalidLimitsWithoutCallingMatcher() {
        final boolean[] called = {false};
        List<LegacyPosition> result = LegacyChainSearch.findConnected(
                new LegacyPosition(0, 0, 0), 0, 1, false,
                position -> {
                    called[0] = true;
                    return true;
                });

        assertTrue(result.isEmpty());
        assertFalse(called[0]);
    }

    @Test
    void enforcesVisitedBudgetBeforeExploringAnUnboundedComponent() {
        List<LegacyPosition> result = LegacyChainSearch.findConnected(
                new LegacyPosition(0, 0, 0), 64, 64, true, 5,
                position -> true);

        assertEquals(5, result.size());
        assertEquals(result.size(), new HashSet<LegacyPosition>(result).size());
    }
}
