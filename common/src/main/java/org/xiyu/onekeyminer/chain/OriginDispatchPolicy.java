package org.xiyu.onekeyminer.chain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Requires the clicked origin as an authorization token and avoids replaying it. */
final class OriginDispatchPolicy {
    private OriginDispatchPolicy() {
    }

    static <T> List<T> authorizeAndOrder(
            T origin,
            Collection<T> targets,
            boolean originAlreadyHandled
    ) {
        return authorizeAndOrder(origin, targets, originAlreadyHandled, Integer.MAX_VALUE);
    }

    static <T> List<T> authorizeAndOrder(
            T origin,
            Collection<T> targets,
            boolean originAlreadyHandled,
            int inspectionLimit
    ) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(targets, "targets");
        if (inspectionLimit <= 0) {
            return List.of();
        }
        boolean foundOrigin = false;
        List<T> ordered = new ArrayList<>(Math.min(targets.size(), inspectionLimit));
        int inspected = 0;
        for (T target : targets) {
            if (++inspected > inspectionLimit) {
                break;
            }
            if (origin.equals(target)) {
                foundOrigin = true;
                continue;
            }
            if (target != null) {
                ordered.add(target);
            }
        }
        if (!foundOrigin) {
            return List.of();
        }
        if (!originAlreadyHandled) {
            ordered.add(0, origin);
        }
        return List.copyOf(ordered);
    }
}
