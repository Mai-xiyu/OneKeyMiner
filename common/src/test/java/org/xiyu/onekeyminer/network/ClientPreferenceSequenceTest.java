package org.xiyu.onekeyminer.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ClientPreferenceSequenceTest {
    @Test
    void distinguishesNewRetryStaleAndWraparound() {
        assertEquals(
                ClientPreferenceSequence.Admission.NEW,
                ClientPreferenceSequence.classify(0, 1)
        );
        assertEquals(
                ClientPreferenceSequence.Admission.RETRY,
                ClientPreferenceSequence.classify(10, 10)
        );
        assertEquals(
                ClientPreferenceSequence.Admission.STALE,
                ClientPreferenceSequence.classify(10, 9)
        );
        assertEquals(
                ClientPreferenceSequence.Admission.NEW,
                ClientPreferenceSequence.classify(Integer.MAX_VALUE, 1)
        );
        assertEquals(
                ClientPreferenceSequence.Admission.STALE,
                ClientPreferenceSequence.classify(1, Integer.MAX_VALUE)
        );
    }
}
