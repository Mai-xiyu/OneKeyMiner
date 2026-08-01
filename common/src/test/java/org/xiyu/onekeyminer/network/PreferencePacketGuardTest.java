package org.xiyu.onekeyminer.network;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PreferencePacketGuardTest {
    @Test
    void admitsAtMostFourPacketsPerPlayerTickUnderConcurrency() throws Exception {
        PreferencePacketGuard guard = new PreferencePacketGuard(600);
        UUID player = UUID.randomUUID();
        int workers = 24;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(workers);
        AtomicInteger accepted = new AtomicInteger();

        for (int index = 0; index < workers; index++) {
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                    if (guard.tryAcquire(player, 50)) {
                        accepted.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
            thread.start();
        }
        start.countDown();
        done.await();

        assertEquals(4, accepted.get());
        assertTrue(guard.tryAcquire(player, 51));
    }

    @Test
    void invalidLogsAreSampledAndClockRollbackIsHandled() {
        PreferencePacketGuard guard = new PreferencePacketGuard(600);
        UUID player = UUID.randomUUID();

        assertTrue(guard.shouldLogInvalid(player, 1000));
        assertFalse(guard.shouldLogInvalid(player, 1200));
        assertTrue(guard.shouldLogInvalid(player, 900));
    }
}
