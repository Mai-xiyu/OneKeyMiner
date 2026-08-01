package org.xiyu.onekeyminer.network;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Per-player packet admission and invalid-log sampling. */
public final class PreferencePacketGuard {
    private final long invalidLogIntervalTicks;
    private final ConcurrentHashMap<UUID, Long> lastAcceptedTicks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastInvalidLogTicks = new ConcurrentHashMap<>();

    public PreferencePacketGuard(long invalidLogIntervalTicks) {
        if (invalidLogIntervalTicks < 1) {
            throw new IllegalArgumentException("invalidLogIntervalTicks must be positive");
        }
        this.invalidLogIntervalTicks = invalidLogIntervalTicks;
    }

    /** Rejects duplicate snapshots received during the same server tick. */
    public boolean tryAcquire(UUID playerId, long tick) {
        Objects.requireNonNull(playerId, "playerId");
        Long previous = lastAcceptedTicks.put(playerId, tick);
        if (previous == null || previous != tick) {
            return true;
        }
        lastAcceptedTicks.put(playerId, previous);
        return false;
    }

    public boolean shouldLogInvalid(UUID playerId, long tick) {
        Objects.requireNonNull(playerId, "playerId");
        final boolean[] shouldLog = {false};
        lastInvalidLogTicks.compute(playerId, (ignored, previous) -> {
            if (previous == null
                    || tick < previous
                    || tick - previous >= invalidLogIntervalTicks) {
                shouldLog[0] = true;
                return tick;
            }
            return previous;
        });
        return shouldLog[0];
    }

    public void clear(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        lastAcceptedTicks.remove(playerId);
        lastInvalidLogTicks.remove(playerId);
    }

    public void clearAll() {
        lastAcceptedTicks.clear();
        lastInvalidLogTicks.clear();
    }
}
