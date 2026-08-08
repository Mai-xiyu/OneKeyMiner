package org.xiyu.onekeyminer.network;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Per-player packet admission and invalid-log sampling. */
public final class PreferencePacketGuard {
    private final long invalidLogIntervalTicks;
    private final int maxPacketsPerTick;
    private final ConcurrentHashMap<UUID, TickWindow> acceptedWindows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastInvalidLogTicks = new ConcurrentHashMap<>();

    public PreferencePacketGuard(long invalidLogIntervalTicks) {
        this(invalidLogIntervalTicks, 4);
    }

    PreferencePacketGuard(long invalidLogIntervalTicks, int maxPacketsPerTick) {
        if (invalidLogIntervalTicks < 1) {
            throw new IllegalArgumentException("invalidLogIntervalTicks must be positive");
        }
        if (maxPacketsPerTick < 1) {
            throw new IllegalArgumentException("maxPacketsPerTick must be positive");
        }
        this.invalidLogIntervalTicks = invalidLogIntervalTicks;
        this.maxPacketsPerTick = maxPacketsPerTick;
    }

    /** Atomically admits a small burst while bounding abusive per-tick traffic. */
    public boolean tryAcquire(UUID playerId, long tick) {
        Objects.requireNonNull(playerId, "playerId");
        final boolean[] acquired = {false};
        acceptedWindows.compute(playerId, (ignored, previous) -> {
            if (previous == null || previous.tick() != tick) {
                acquired[0] = true;
                return new TickWindow(tick, 1);
            }
            if (previous.count() < maxPacketsPerTick) {
                acquired[0] = true;
                return new TickWindow(tick, previous.count() + 1);
            }
            return previous;
        });
        return acquired[0];
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
        acceptedWindows.remove(playerId);
        lastInvalidLogTicks.remove(playerId);
    }

    public void clearAll() {
        acceptedWindows.clear();
        lastInvalidLogTicks.clear();
    }

    private record TickWindow(long tick, int count) {
    }
}
