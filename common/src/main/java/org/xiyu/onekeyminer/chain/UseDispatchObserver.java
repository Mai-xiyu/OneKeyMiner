package org.xiyu.onekeyminer.chain;

import java.util.ArrayDeque;
import java.util.function.Supplier;

/**
 * Exception-safe, nested observation of an authoritative use call.
 */
final class UseDispatchObserver {

    private final ThreadLocal<ArrayDeque<Frame>> frames =
            ThreadLocal.withInitial(ArrayDeque::new);

    <T> Observed<T> observe(Supplier<T> authoritativeUse) {
        ArrayDeque<Frame> stack = frames.get();
        Frame frame = new Frame();
        stack.push(frame);
        try {
            return new Observed<>(authoritativeUse.get(), frame.dispatched);
        } finally {
            stack.pop();
            if (stack.isEmpty()) {
                frames.remove();
            }
        }
    }

    void markDispatched() {
        ArrayDeque<Frame> stack = frames.get();
        if (!stack.isEmpty()) {
            stack.peek().dispatched = true;
        } else {
            frames.remove();
        }
    }

    record Observed<T>(T result, boolean dispatched) {
    }

    private static final class Frame {
        private boolean dispatched;
    }
}
