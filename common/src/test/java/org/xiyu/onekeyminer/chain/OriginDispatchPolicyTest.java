package org.xiyu.onekeyminer.chain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class OriginDispatchPolicyTest {

    private static final String ORIGIN = "origin";
    private static final String NEIGHBOR = "neighbor";

    @Test
    void removesAlreadyHandledOriginButKeepsDerivedTargets() {
        assertEquals(
                List.of(NEIGHBOR),
                OriginDispatchPolicy.authorizeAndOrder(
                        ORIGIN,
                        List.of(ORIGIN, NEIGHBOR),
                        true
                )
        );
    }

    @Test
    void rejectsDerivedTargetsWhenListenerRemovedAuthorizationOrigin() {
        assertEquals(
                List.of(),
                OriginDispatchPolicy.authorizeAndOrder(
                        ORIGIN,
                        List.of(NEIGHBOR),
                        true
                )
        );
    }

    @Test
    void keepsOriginFirstForActionsThatHaveNotRunYet() {
        assertEquals(
                List.of(ORIGIN, NEIGHBOR),
                OriginDispatchPolicy.authorizeAndOrder(
                        ORIGIN,
                        List.of(NEIGHBOR, ORIGIN),
                        false
                )
        );
    }

    @Test
    void removesOnlyOneOriginOccurrenceForCoLocatedEntities() {
        assertEquals(
                List.of(ORIGIN, NEIGHBOR),
                OriginDispatchPolicy.authorizeAndOrder(
                        ORIGIN,
                        List.of(ORIGIN, ORIGIN, NEIGHBOR),
                        true
                )
        );
    }
}
