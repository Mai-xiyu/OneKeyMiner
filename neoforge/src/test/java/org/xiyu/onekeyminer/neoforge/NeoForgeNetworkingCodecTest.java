package org.xiyu.onekeyminer.neoforge;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import org.xiyu.onekeyminer.network.ClientPreferenceProtocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class NeoForgeNetworkingCodecTest {

    @Test
    void requestAndAcknowledgementCodecsRoundTripEveryField() {
        var request = new NeoForgeNetworking.ClientPreferencesPayload(
                ClientPreferenceProtocol.WIRE_VERSION,
                29,
                true,
                "onekeyminer:cube",
                true,
                false
        );
        var ack = new NeoForgeNetworking.ServerPreferencesAckPayload(
                ClientPreferenceProtocol.WIRE_VERSION,
                29,
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

    private static NeoForgeNetworking.ClientPreferencesPayload roundTripRequest(
            NeoForgeNetworking.ClientPreferencesPayload payload
    ) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            NeoForgeNetworking.ClientPreferencesPayload.STREAM_CODEC.encode(buffer, payload);
            return NeoForgeNetworking.ClientPreferencesPayload.STREAM_CODEC.decode(buffer);
        } finally {
            buffer.release();
        }
    }

    private static NeoForgeNetworking.ServerPreferencesAckPayload roundTripAck(
            NeoForgeNetworking.ServerPreferencesAckPayload payload
    ) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            NeoForgeNetworking.ServerPreferencesAckPayload.STREAM_CODEC.encode(buffer, payload);
            return NeoForgeNetworking.ServerPreferencesAckPayload.STREAM_CODEC.decode(buffer);
        } finally {
            buffer.release();
        }
    }
}
