package org.xiyu.onekeyminer.fabric;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import org.xiyu.onekeyminer.network.ClientPreferenceProtocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FabricPayloadsCodecTest {

    @Test
    void requestAndAcknowledgementCodecsRoundTripEveryField() {
        var request = new FabricPayloads.ClientPreferencesPayload(
                ClientPreferenceProtocol.WIRE_VERSION,
                19,
                true,
                "onekeyminer:cube",
                true,
                false
        );
        var ack = new FabricPayloads.ServerPreferencesAckPayload(
                ClientPreferenceProtocol.WIRE_VERSION,
                19,
                true,
                "onekeyminer:cube",
                64,
                16,
                false,
                false,
                true,
                ClientPreferenceProtocol.SUPPORTED_CAPABILITIES
        );

        assertEquals(request, roundTripRequest(request));
        assertEquals(ack, roundTripAck(ack));
    }

    private static FabricPayloads.ClientPreferencesPayload roundTripRequest(
            FabricPayloads.ClientPreferencesPayload payload
    ) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            FabricPayloads.ClientPreferencesPayload.STREAM_CODEC.encode(buffer, payload);
            return FabricPayloads.ClientPreferencesPayload.STREAM_CODEC.decode(buffer);
        } finally {
            buffer.release();
        }
    }

    private static FabricPayloads.ServerPreferencesAckPayload roundTripAck(
            FabricPayloads.ServerPreferencesAckPayload payload
    ) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            FabricPayloads.ServerPreferencesAckPayload.STREAM_CODEC.encode(buffer, payload);
            return FabricPayloads.ServerPreferencesAckPayload.STREAM_CODEC.decode(buffer);
        } finally {
            buffer.release();
        }
    }
}
