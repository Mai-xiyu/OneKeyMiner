package org.xiyu.onekeyminer.chain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UseDispatchObserverTest {

    @Test
    void recordsOnlyAnActuallyDispatchedAction() {
        UseDispatchObserver observer = new UseDispatchObserver();

        UseDispatchObserver.Observed<String> unhandled =
                observer.observe(() -> "pass");
        UseDispatchObserver.Observed<String> handled =
                observer.observe(() -> {
                    observer.markDispatched();
                    return "success";
                });

        assertFalse(unhandled.dispatched());
        assertTrue(handled.dispatched());
        assertEquals("success", handled.result());
    }

    @Test
    void exceptionDoesNotLeakAnObservationFrame() {
        UseDispatchObserver observer = new UseDispatchObserver();

        assertThrows(IllegalStateException.class, () -> observer.observe(() -> {
            observer.markDispatched();
            throw new IllegalStateException("boom");
        }));

        assertFalse(observer.observe(() -> "next").dispatched());
    }

    @Test
    void nestedObservationMarksOnlyItsCurrentFrame() {
        UseDispatchObserver observer = new UseDispatchObserver();

        UseDispatchObserver.Observed<UseDispatchObserver.Observed<String>> outer =
                observer.observe(() -> observer.observe(() -> {
                    observer.markDispatched();
                    return "inner";
                }));

        assertFalse(outer.dispatched());
        assertTrue(outer.result().dispatched());
    }
}
