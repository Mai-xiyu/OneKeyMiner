package org.xiyu.onekeyminer.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ClientPreferenceSequenceTest {
    @Test
    void firstPositiveSequenceIsNew() {
        assertEquals(
                ClientPreferenceSequence.Admission.NEW,
                ClientPreferenceSequence.classify(0, 1)
        );
    }

    @Test
    void identicalSequenceIsRetry() {
        assertEquals(
                ClientPreferenceSequence.Admission.RETRY,
                ClientPreferenceSequence.classify(10, 10)
        );
    }

    @Test
    void lowerSequenceIsStaleWithoutWraparound() {
        assertEquals(
                ClientPreferenceSequence.Admission.STALE,
                ClientPreferenceSequence.classify(10, 9)
        );
    }

    @Test
    void maximumSequenceWrapsForwardToOne() {
        assertEquals(
                ClientPreferenceSequence.Admission.NEW,
                ClientPreferenceSequence.classify(Integer.MAX_VALUE, 1)
        );
    }

    @Test
    void maximumSequenceIsStaleAfterOne() {
        assertEquals(
                ClientPreferenceSequence.Admission.STALE,
                ClientPreferenceSequence.classify(1, Integer.MAX_VALUE)
        );
    }

    @Test
    void nonPositiveCandidateIsStale() {
        assertEquals(
                ClientPreferenceSequence.Admission.STALE,
                ClientPreferenceSequence.classify(10, 0)
        );
    }
}
