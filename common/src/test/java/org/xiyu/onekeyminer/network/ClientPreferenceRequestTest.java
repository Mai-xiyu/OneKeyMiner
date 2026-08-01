package org.xiyu.onekeyminer.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

final class ClientPreferenceRequestTest {
    @Test
    void rejectsShapeIdentifierThatCannotFitOnTheWire() {
        String oversized = "x".repeat(
                ClientPreferenceProtocol.MAX_SHAPE_ID_LENGTH + 1
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ClientPreferenceRequest(
                        true,
                        oversized,
                        false,
                        false
                )
        );
    }
}
