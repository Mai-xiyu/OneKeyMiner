package org.xiyu.onekeyminer.chain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OriginDispatchPolicyTest {
    @Test
    void missingOriginRejectsDerivedTargets() {
        assertTrue(OriginDispatchPolicy.authorizeAndOrder(
                "origin",
                List.of("other"),
                true
        ).isEmpty());
    }

    @Test
    void completedOriginIsRemovedIncludingDuplicates() {
        assertEquals(
                List.of("first", "second"),
                OriginDispatchPolicy.authorizeAndOrder(
                        "origin",
                        List.of("first", "origin", "origin", "second"),
                        true
                )
        );
    }

    @Test
    void pendingOriginIsRestoredToFrontExactlyOnce() {
        assertEquals(
                List.of("origin", "first", "second"),
                OriginDispatchPolicy.authorizeAndOrder(
                        "origin",
                        List.of("first", "origin", "second", "origin"),
                        false
                )
        );
    }

    @Test
    void inspectionLimitRejectsAnOriginHiddenAfterTheBound() {
        assertTrue(OriginDispatchPolicy.authorizeAndOrder(
                "origin",
                List.of("first", "second", "origin"),
                true,
                2
        ).isEmpty());
    }

    @Test
    void inspectionLimitBoundsTheAuthorizedCopy() {
        assertEquals(
                List.of("first", "second"),
                OriginDispatchPolicy.authorizeAndOrder(
                        "origin",
                        List.of("origin", "first", "second", "third"),
                        true,
                        3
                )
        );
    }
}
