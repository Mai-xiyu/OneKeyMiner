package org.xiyu.onekeyminer.api.event;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertThrows;

final class ChainEventsRegistrationTest {
    @Test
    void preActionRegistrationRejectsNullArguments() {
        assertThrows(NullPointerException.class,
                () -> ChainEvents.registerPreActionListener((Consumer<PreActionEvent>) null));
        assertThrows(NullPointerException.class,
                () -> ChainEvents.registerPreActionListener(
                        (org.xiyu.onekeyminer.chain.ChainActionType) null,
                        event -> { }));
        assertThrows(NullPointerException.class,
                () -> ChainEvents.registerPreActionListener(
                        (Predicate<PreActionEvent>) null, event -> { }));
        assertThrows(NullPointerException.class,
                () -> ChainEvents.registerPreActionListener(
                        event -> true, (Consumer<PreActionEvent>) null));
    }

    @Test
    void postActionRegistrationRejectsNullArguments() {
        assertThrows(NullPointerException.class,
                () -> ChainEvents.registerPostActionListener((Consumer<PostActionEvent>) null));
        assertThrows(NullPointerException.class,
                () -> ChainEvents.registerPostActionListener(
                        (org.xiyu.onekeyminer.chain.ChainActionType) null,
                        event -> { }));
        assertThrows(NullPointerException.class,
                () -> ChainEvents.registerPostActionListener(
                        (Predicate<PostActionEvent>) null, event -> { }));
        assertThrows(NullPointerException.class,
                () -> ChainEvents.registerPostActionListener(
                        event -> true, (Consumer<PostActionEvent>) null));
    }
}
