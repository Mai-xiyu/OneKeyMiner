package org.xiyu.onekeyminer.network;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PreferencePacketGuardTest {

    @Test
    void acceptsSmallBurstsButBoundsPacketsPerPlayerPerTick() {
        PreferencePacketGuard guard = new PreferencePacketGuard(600, 4);
        UUID firstPlayer = UUID.randomUUID();
        UUID secondPlayer = UUID.randomUUID();

        assertTrue(guard.tryAcquire(firstPlayer, 10));
        assertTrue(guard.tryAcquire(firstPlayer, 10));
        assertTrue(guard.tryAcquire(firstPlayer, 10));
        assertTrue(guard.tryAcquire(firstPlayer, 10));
        assertFalse(guard.tryAcquire(firstPlayer, 10));
        assertTrue(guard.tryAcquire(firstPlayer, 11));
        assertTrue(guard.tryAcquire(secondPlayer, 10));
    }

    @Test
    void samplesInvalidPacketLogsEverySixHundredTicksPerPlayer() {
        PreferencePacketGuard guard = new PreferencePacketGuard(600);
        UUID player = UUID.randomUUID();

        assertTrue(guard.shouldLogInvalid(player, 100));
        assertFalse(guard.shouldLogInvalid(player, 699));
        assertTrue(guard.shouldLogInvalid(player, 700));
    }

    @Test
    void cleanupRemovesRateAndLogHistory() {
        PreferencePacketGuard guard = new PreferencePacketGuard(600);
        UUID player = UUID.randomUUID();
        guard.tryAcquire(player, 10);
        guard.shouldLogInvalid(player, 10);

        guard.clear(player);

        assertTrue(guard.tryAcquire(player, 10));
        assertTrue(guard.shouldLogInvalid(player, 10));
    }
}
