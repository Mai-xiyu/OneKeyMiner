package org.xiyu.onekeyminer.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientPreferenceSyncTrackerTest {
    @Test
    void onlyNewestPendingSequenceCanConfirmSynchronization() {
        ClientPreferenceSyncTracker tracker = new ClientPreferenceSyncTracker();
        int first = tracker.beginAttempt();
        int second = tracker.beginAttempt();

        assertFalse(tracker.confirm(ack(first)));
        assertTrue(tracker.isPending());
        assertTrue(tracker.confirm(ack(second)));
        assertFalse(tracker.isPending());
        assertSame(tracker.lastAck(), tracker.lastAck());
    }

    @Test
    void invalidatedLateAcknowledgementCannotRestoreSynchronization() {
        ClientPreferenceSyncTracker tracker = new ClientPreferenceSyncTracker();
        int sequence = tracker.beginAttempt();
        tracker.invalidatePendingAttempt();

        assertFalse(tracker.confirm(ack(sequence)));
        assertTrue(tracker.isPending());
        assertFalse(tracker.hasPendingAttempt());
    }

    @Test
    void resetRejectsPreviousConnectionAcknowledgement() {
        ClientPreferenceSyncTracker tracker = new ClientPreferenceSyncTracker();
        int sequence = tracker.beginAttempt();
        tracker.reset();

        assertFalse(tracker.confirm(ack(sequence)));
        assertNull(tracker.lastAck());
    }

    @Test
    void mismatchedWireVersionIsRejected() {
        ClientPreferenceSyncTracker tracker = new ClientPreferenceSyncTracker();
        int sequence = tracker.beginAttempt();
        ClientPreferenceAck wrong = new ClientPreferenceAck(
                ClientPreferenceProtocol.WIRE_VERSION - 1,
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

        assertFalse(tracker.confirm(wrong));
        assertTrue(tracker.isPending());
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
