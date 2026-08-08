package org.xiyu.onekeyminer.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

final class ClientPreferenceRequestTest {
    @Test
    void rejectsShapeIdBeyondWireLimit() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ClientPreferenceRequest(
                        true,
                        "x".repeat(129),
                        false,
                        false
                )
        );
    }
}
