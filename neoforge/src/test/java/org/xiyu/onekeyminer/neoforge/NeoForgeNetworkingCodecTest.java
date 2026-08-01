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
                false,
                ClientPreferenceProtocol.SUPPORTED_CAPABILITIES
        );

        assertEquals(request, roundTripRequest(request));
        assertEquals(ack, roundTripAck(ack));
    }

    @Test
    void requestCodecUsesCanonicalWireOrder() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            NeoForgeNetworking.ClientPreferencesPayload.STREAM_CODEC.encode(
                    buffer,
                    request()
            );
            assertEquals(3, buffer.readVarInt());
            assertEquals(39, buffer.readVarInt());
            assertEquals(true, buffer.readBoolean());
            assertEquals(
                    "onekeyminer:cube",
                    buffer.readUtf(NeoForgeNetworking.MAX_SHAPE_ID_LENGTH)
            );
            assertEquals(true, buffer.readBoolean());
            assertEquals(false, buffer.readBoolean());
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void requestDecoderAcceptsCanonicalWireOrder() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            writeCanonicalRequest(buffer);
            assertEquals(
                    request(),
                    NeoForgeNetworking.ClientPreferencesPayload.STREAM_CODEC.decode(buffer)
            );
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void acknowledgementCodecUsesCanonicalWireOrder() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            NeoForgeNetworking.ServerPreferencesAckPayload.STREAM_CODEC.encode(
                    buffer,
                    acknowledgement()
            );
            assertEquals(3, buffer.readVarInt());
            assertEquals(43, buffer.readVarInt());
            assertEquals(true, buffer.readBoolean());
            assertEquals("onekeyminer:amorphous", buffer.readUtf(128));
            assertEquals(83, buffer.readVarInt());
            assertEquals(29, buffer.readVarInt());
            assertEquals(false, buffer.readBoolean());
            assertEquals(true, buffer.readBoolean());
            assertEquals(false, buffer.readBoolean());
            assertEquals(7, buffer.readVarInt());
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void acknowledgementDecoderAcceptsCanonicalWireOrder() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            writeCanonicalAcknowledgement(buffer);
            assertEquals(
                    acknowledgement(),
                    NeoForgeNetworking.ServerPreferencesAckPayload.STREAM_CODEC.decode(buffer)
            );
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    private static NeoForgeNetworking.ServerPreferencesAckPayload acknowledgement() {
        return new NeoForgeNetworking.ServerPreferencesAckPayload(
                3, 43, true, "onekeyminer:amorphous", 83, 29,
                false, true, false, 7
        );
    }

    private static NeoForgeNetworking.ClientPreferencesPayload request() {
        return new NeoForgeNetworking.ClientPreferencesPayload(
                3, 39, true, "onekeyminer:cube", true, false
        );
    }

    private static void writeCanonicalRequest(FriendlyByteBuf buffer) {
        buffer.writeVarInt(3);
        buffer.writeVarInt(39);
        buffer.writeBoolean(true);
        buffer.writeUtf(
                "onekeyminer:cube",
                NeoForgeNetworking.MAX_SHAPE_ID_LENGTH
        );
        buffer.writeBoolean(true);
        buffer.writeBoolean(false);
    }

    private static void writeCanonicalAcknowledgement(FriendlyByteBuf buffer) {
        buffer.writeVarInt(3);
        buffer.writeVarInt(43);
        buffer.writeBoolean(true);
        buffer.writeUtf("onekeyminer:amorphous", 128);
        buffer.writeVarInt(83);
        buffer.writeVarInt(29);
        buffer.writeBoolean(false);
        buffer.writeBoolean(true);
        buffer.writeBoolean(false);
        buffer.writeVarInt(7);
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
