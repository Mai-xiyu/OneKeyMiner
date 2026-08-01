package org.xiyu.onekeyminer.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
    }

    @Test
    void cancelledAttemptCannotBeConfirmed() {
        ClientPreferenceSyncTracker tracker = new ClientPreferenceSyncTracker();
        int sequence = tracker.beginAttempt();

        tracker.cancelAttempt(sequence);

        assertFalse(tracker.confirm(ack(sequence)));
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
    void resetInvalidatesAcknowledgementsFromPreviousConnection() {
        ClientPreferenceSyncTracker tracker = new ClientPreferenceSyncTracker();
        int sequence = tracker.beginAttempt();

        tracker.reset();

        assertFalse(tracker.confirm(ack(sequence)));
        assertTrue(tracker.isPending());
    }

    private static ClientPreferenceAck ack(int sequence) {
        return new ClientPreferenceAck(
                ClientPreferenceProtocol.WIRE_VERSION,
                sequence,
                "onekeyminer:amorphous",
                false,
                false,
                ClientPreferenceProtocol.SUPPORTED_CAPABILITIES
        );
    }
}
