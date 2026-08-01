package org.xiyu.onekeyminer.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

final class ClientPreferenceAckTest {

    @Test
    void rejectsValuesOutsideCanonicalProtocolBounds() {
        assertThrows(IllegalArgumentException.class, () -> ack(0, 64, 16, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> ack(1, ClientPreferenceProtocol.MAX_APPLIED_BLOCKS + 1, 16, 0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ack(1, 64, ClientPreferenceProtocol.MAX_APPLIED_DISTANCE + 1, 0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new ClientPreferenceAck(
                        ClientPreferenceProtocol.WIRE_VERSION,
                        1,
                        true,
                        "x".repeat(ClientPreferenceProtocol.MAX_SHAPE_ID_LENGTH + 1),
                        64,
                        16,
                        true,
                        false,
                        false,
                        0
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ack(
                        1,
                        64,
                        16,
                        ClientPreferenceProtocol.SUPPORTED_CAPABILITIES | (1 << 30)
                )
        );
    }

    private static ClientPreferenceAck ack(
            int sequence,
            int maxBlocks,
            int maxDistance,
            int capabilities
    ) {
        return new ClientPreferenceAck(
                ClientPreferenceProtocol.WIRE_VERSION,
                sequence,
                true,
                "onekeyminer:amorphous",
                maxBlocks,
                maxDistance,
                true,
                false,
                false,
                capabilities
        );
    }
}
