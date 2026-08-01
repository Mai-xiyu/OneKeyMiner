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
            FabricPayloads.ClientPreferencesPayload.STREAM_CODEC.encode(
                    buffer,
                    request()
            );
            assertEquals(3, buffer.readVarInt());
            assertEquals(31, buffer.readVarInt());
            assertEquals(true, buffer.readBoolean());
            assertEquals(
                    "onekeyminer:column",
                    buffer.readUtf(FabricPayloads.MAX_SHAPE_ID_LENGTH)
            );
            assertEquals(false, buffer.readBoolean());
            assertEquals(true, buffer.readBoolean());
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
                    FabricPayloads.ClientPreferencesPayload.STREAM_CODEC.decode(buffer)
            );
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void acknowledgementCodecUsesCanonicalWireOrder() {
        var ack = acknowledgement();
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            FabricPayloads.ServerPreferencesAckPayload.STREAM_CODEC.encode(buffer, ack);
            assertEquals(3, buffer.readVarInt());
            assertEquals(37, buffer.readVarInt());
            assertEquals(false, buffer.readBoolean());
            assertEquals("onekeyminer:cube", buffer.readUtf(128));
            assertEquals(73, buffer.readVarInt());
            assertEquals(19, buffer.readVarInt());
            assertEquals(true, buffer.readBoolean());
            assertEquals(false, buffer.readBoolean());
            assertEquals(true, buffer.readBoolean());
            assertEquals(13, buffer.readVarInt());
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
                    FabricPayloads.ServerPreferencesAckPayload.STREAM_CODEC.decode(buffer)
            );
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    private static FabricPayloads.ServerPreferencesAckPayload acknowledgement() {
        return new FabricPayloads.ServerPreferencesAckPayload(
                3, 37, false, "onekeyminer:cube", 73, 19,
                true, false, true, 13
        );
    }

    private static FabricPayloads.ClientPreferencesPayload request() {
        return new FabricPayloads.ClientPreferencesPayload(
                3, 31, true, "onekeyminer:column", false, true
        );
    }

    private static void writeCanonicalRequest(FriendlyByteBuf buffer) {
        buffer.writeVarInt(3);
        buffer.writeVarInt(31);
        buffer.writeBoolean(true);
        buffer.writeUtf("onekeyminer:column", FabricPayloads.MAX_SHAPE_ID_LENGTH);
        buffer.writeBoolean(false);
        buffer.writeBoolean(true);
    }

    private static void writeCanonicalAcknowledgement(FriendlyByteBuf buffer) {
        buffer.writeVarInt(3);
        buffer.writeVarInt(37);
        buffer.writeBoolean(false);
        buffer.writeUtf("onekeyminer:cube", 128);
        buffer.writeVarInt(73);
        buffer.writeVarInt(19);
        buffer.writeBoolean(true);
        buffer.writeBoolean(false);
        buffer.writeBoolean(true);
        buffer.writeVarInt(13);
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
