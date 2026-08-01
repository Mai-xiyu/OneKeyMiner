package org.xiyu.onekeyminer.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientPreferenceSyncTrackerTest {

    @Test
    void onlyLatestAcknowledgementConfirmsSynchronization() {
        ClientPreferenceSyncTracker tracker = new ClientPreferenceSyncTracker();
        int first = tracker.beginAttempt();
        int second = tracker.beginAttempt();

        assertFalse(tracker.confirm(ack(first)));
        assertTrue(tracker.isPending());
        assertTrue(tracker.confirm(ack(second)));
        assertFalse(tracker.isPending());
        assertEquals(second, tracker.lastAck().sequence());
    }

    @Test
    void cancelledAndPreviousConnectionAttemptsCannotBeConfirmed() {
        ClientPreferenceSyncTracker tracker = new ClientPreferenceSyncTracker();
        int cancelled = tracker.beginAttempt();
        tracker.cancelAttempt(cancelled);

        assertFalse(tracker.confirm(ack(cancelled)));
        assertNull(tracker.lastAck());

        int previousConnection = tracker.beginAttempt();
        tracker.reset();

        assertFalse(tracker.confirm(ack(previousConnection)));
        assertTrue(tracker.isPending());
        assertNull(tracker.lastAck());
    }

    @Test
    void rejectsAcknowledgementsFromAnotherWireVersion() {
        ClientPreferenceSyncTracker tracker = new ClientPreferenceSyncTracker();
        int sequence = tracker.beginAttempt();
        ClientPreferenceAck wrongVersion = new ClientPreferenceAck(
                ClientPreferenceProtocol.WIRE_VERSION - 1,
                sequence,
                true,
                "onekeyminer:amorphous",
                64,
                16,
                true,
                false,
                false,
                0
        );

        assertFalse(tracker.confirm(wrongVersion));
        assertTrue(tracker.isPending());
    }

    @Test
    void failedNewAttemptDoesNotReuseAnOlderConfirmation() {
        ClientPreferenceSyncTracker tracker = new ClientPreferenceSyncTracker();
        int confirmed = tracker.beginAttempt();
        assertTrue(tracker.confirm(ack(confirmed)));

        int failed = tracker.beginAttempt();
        tracker.cancelAttempt(failed);

        assertTrue(tracker.isPending());
    }

    @Test
    void oneSequenceCanBeRetriedUntilItsAcknowledgementArrives() {
        ClientPreferenceSyncTracker tracker = new ClientPreferenceSyncTracker();
        int sequence = tracker.beginAttempt();

        assertTrue(tracker.hasPendingAttempt());
        assertEquals(sequence, tracker.pendingSequence());
        assertTrue(tracker.confirm(ack(sequence)));
        assertFalse(tracker.hasPendingAttempt());
        assertEquals(0, tracker.pendingSequence());
    }

    @Test
    void invalidatingDirtySnapshotRejectsItsLateAcknowledgement() {
        ClientPreferenceSyncTracker tracker = new ClientPreferenceSyncTracker();
        int staleSequence = tracker.beginAttempt();

        tracker.invalidatePendingAttempt();

        assertFalse(tracker.hasPendingAttempt());
        assertEquals(0, tracker.pendingSequence());
        assertTrue(tracker.isPending());
        assertFalse(tracker.confirm(ack(staleSequence)));
        assertNull(tracker.lastAck());
    }

    @Test
    void dirtySnapshotCannotReplacePriorAckAndNextAttemptCanConfirm() {
        ClientPreferenceSyncTracker tracker = new ClientPreferenceSyncTracker();
        int confirmedSequence = tracker.beginAttempt();
        assertTrue(tracker.confirm(ack(confirmedSequence)));

        int staleSequence = tracker.beginAttempt();
        tracker.invalidatePendingAttempt();

        assertFalse(tracker.confirm(ack(staleSequence)));
        assertTrue(tracker.isPending());
        assertEquals(confirmedSequence, tracker.lastAck().sequence());

        int latestSequence = tracker.beginAttempt();
        assertTrue(tracker.confirm(ack(latestSequence)));
        assertFalse(tracker.isPending());
        assertEquals(latestSequence, tracker.lastAck().sequence());
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
