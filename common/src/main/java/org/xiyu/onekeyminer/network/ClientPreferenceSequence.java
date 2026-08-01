package org.xiyu.onekeyminer.network;

/** Serial-number ordering for the positive, wrapping client sequence space. */
public final class ClientPreferenceSequence {
    private static final long HALF_RANGE = Integer.MAX_VALUE / 2L;

    private ClientPreferenceSequence() {
    }

    public static Admission classify(int previous, int candidate) {
        if (candidate <= 0) {
            return Admission.STALE;
        }
        if (previous <= 0) {
            return Admission.NEW;
        }
        if (candidate == previous) {
            return Admission.RETRY;
        }
        long forwardDistance = candidate > previous
                ? (long) candidate - previous
                : (long) Integer.MAX_VALUE - previous + candidate;
        return forwardDistance > 0 && forwardDistance <= HALF_RANGE
                ? Admission.NEW
                : Admission.STALE;
    }

    public enum Admission {
        NEW,
        RETRY,
        STALE
    }
}
