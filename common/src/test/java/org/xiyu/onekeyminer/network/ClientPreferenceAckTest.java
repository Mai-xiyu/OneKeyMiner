package org.xiyu.onekeyminer.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

final class ClientPreferenceAckTest {
    private static final String SHAPE = "onekeyminer:amorphous";

    @Test
    void rejectsNonPositiveSequence() {
        assertThrows(IllegalArgumentException.class, () -> ack(0, SHAPE, 64, 16, 0));
    }

    @Test
    void rejectsShapeIdBeyondWireLimit() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ack(1, "x".repeat(129), 64, 16, 0)
        );
    }

    @Test
    void rejectsAppliedLimitsOutsideProtocolBounds() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ack(1, SHAPE, 10_241, 16, 0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ack(1, SHAPE, 64, 129, 0)
        );
    }

    @Test
    void rejectsUnknownCapabilityBits() {
        int unknownBit = 1 << 20;
        assertThrows(
                IllegalArgumentException.class,
                () -> ack(1, SHAPE, 64, 16, unknownBit)
        );
    }

    private static ClientPreferenceAck ack(
            int sequence,
            String shape,
            int maxBlocks,
            int maxDistance,
            int capabilities
    ) {
        return new ClientPreferenceAck(
                ClientPreferenceProtocol.WIRE_VERSION,
                sequence,
                true,
                shape,
                maxBlocks,
                maxDistance,
                true,
                false,
                false,
                capabilities
        );
    }
}
