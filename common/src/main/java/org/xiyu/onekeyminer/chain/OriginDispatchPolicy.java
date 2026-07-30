package org.xiyu.onekeyminer.chain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Preserves the clicked origin as the authorization gate while deciding
 * whether it still needs to be dispatched by the chain executor.
 */
final class OriginDispatchPolicy {

    private OriginDispatchPolicy() {
    }

    static <T> List<T> authorizeAndOrder(
            T origin,
            Collection<T> targets,
            boolean originAlreadyHandled
    ) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(targets, "targets");

        boolean removedAuthorizationOccurrence = false;
        List<T> ordered = new ArrayList<>(targets.size());
        for (T target : targets) {
            if (!removedAuthorizationOccurrence && origin.equals(target)) {
                removedAuthorizationOccurrence = true;
                continue;
            }
            ordered.add(target);
        }
        if (!removedAuthorizationOccurrence) {
            return List.of();
        }
        if (!originAlreadyHandled) {
            ordered.addFirst(origin);
        }
        return List.copyOf(ordered);
    }
}
