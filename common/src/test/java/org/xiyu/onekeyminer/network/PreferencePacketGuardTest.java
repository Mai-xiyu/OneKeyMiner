package org.xiyu.onekeyminer.network;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class PreferencePacketGuardTest {
    @Test
    void acceptsSmallBurstButBoundsOneTick() {
        PreferencePacketGuard guard = new PreferencePacketGuard(600, 4);
        UUID player = UUID.randomUUID();

        assertTrue(guard.tryAcquire(player, 10));
        assertTrue(guard.tryAcquire(player, 10));
        assertTrue(guard.tryAcquire(player, 10));
        assertTrue(guard.tryAcquire(player, 10));
        assertFalse(guard.tryAcquire(player, 10));
        assertTrue(guard.tryAcquire(player, 11));
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
