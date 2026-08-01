package org.xiyu.onekeyminer.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ClientPreferenceSyncTrackerTest {
    @Test
    void onlyNewestAcknowledgementConfirmsSynchronization() {
        ClientPreferenceSyncTracker tracker = new ClientPreferenceSyncTracker();
        int first = tracker.beginAttempt();
        int second = tracker.beginAttempt();

        assertFalse(tracker.confirm(ack(first)));
        assertTrue(tracker.confirm(ack(second)));
        assertFalse(tracker.isPending());
        assertEquals(second, tracker.lastAck().sequence());
    }

    @Test
    void invalidatingDirtySnapshotRejectsLateAcknowledgement() {
        ClientPreferenceSyncTracker tracker = new ClientPreferenceSyncTracker();
        int stale = tracker.beginAttempt();

        tracker.invalidatePendingAttempt();

        assertFalse(tracker.confirm(ack(stale)));
        assertTrue(tracker.isPending());
        assertFalse(tracker.hasPendingAttempt());
    }

    @Test
    void staleAckCannotReplacePriorAckAfterPreferencesBecomeDirty() {
        ClientPreferenceSyncTracker tracker = new ClientPreferenceSyncTracker();
        int confirmed = tracker.beginAttempt();
        assertTrue(tracker.confirm(ack(confirmed)));

        int stale = tracker.beginAttempt();
        tracker.invalidatePendingAttempt();
        assertFalse(tracker.confirm(ack(stale)));
        assertEquals(confirmed, tracker.lastAck().sequence());

        int latest = tracker.beginAttempt();
        assertTrue(tracker.confirm(ack(latest)));
        assertEquals(latest, tracker.lastAck().sequence());
    }

    @Test
    void resetRejectsPreviousConnectionAcknowledgement() {
        ClientPreferenceSyncTracker tracker = new ClientPreferenceSyncTracker();
        int previousConnection = tracker.beginAttempt();
        tracker.reset();

        assertFalse(tracker.confirm(ack(previousConnection)));
        assertNull(tracker.lastAck());
    }

    private static ClientPreferenceAck ack(int sequence) {
        return new ClientPreferenceAck(
                ClientPreferenceProtocol.WIRE_VERSION,
                sequence,
                true,
                "onekeyminer:amorphous",
                64,
                16,
                true,
                false,
                false,
                ClientPreferenceProtocol.SUPPORTED_CAPABILITIES
        );
    }
}
