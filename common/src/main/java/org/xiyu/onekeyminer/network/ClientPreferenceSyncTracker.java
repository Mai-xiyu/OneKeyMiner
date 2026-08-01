package org.xiyu.onekeyminer.network;

/** Tracks acknowledgement state for one logical client connection. */
public final class ClientPreferenceSyncTracker {
    private int nextSequence;
    private int pendingSequence;
    private ClientPreferenceAck lastAck;
    private boolean synchronizedWithServer;

    public synchronized int beginAttempt() {
        nextSequence = nextSequence == Integer.MAX_VALUE ? 1 : nextSequence + 1;
        pendingSequence = nextSequence;
        synchronizedWithServer = false;
        return pendingSequence;
    }

    public synchronized void cancelAttempt(int sequence) {
        if (pendingSequence == sequence) {
            pendingSequence = 0;
        }
    }

    public synchronized void invalidatePendingAttempt() {
        pendingSequence = 0;
        synchronizedWithServer = false;
    }

    public synchronized int pendingSequence() {
        return pendingSequence;
    }

    public synchronized boolean hasPendingAttempt() {
        return pendingSequence > 0;
    }

    public synchronized boolean confirm(ClientPreferenceAck ack) {
        if (ack == null
                || ack.wireVersion() != ClientPreferenceProtocol.WIRE_VERSION
                || ack.sequence() <= 0
                || ack.sequence() != pendingSequence) {
            return false;
        }
        lastAck = ack;
        pendingSequence = 0;
        synchronizedWithServer = true;
        return true;
    }

    public synchronized boolean isPending() {
        return !synchronizedWithServer;
    }

    public synchronized ClientPreferenceAck lastAck() {
        return lastAck;
    }

    public synchronized void reset() {
        pendingSequence = 0;
        lastAck = null;
        synchronizedWithServer = false;
        nextSequence = nextSequence == Integer.MAX_VALUE ? 1 : nextSequence + 1;
    }
}
