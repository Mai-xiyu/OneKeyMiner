package org.xiyu.onekeyminer.neoforge;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import org.xiyu.onekeyminer.network.ClientPreferenceProtocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class NeoForgeNetworkingCodecTest {

    @Test
    void requestCodecUsesCanonicalFieldOrder() {
        var expected = request();
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        try {
            NeoForgeNetworking.ClientPreferencesPayload.STREAM_CODEC.encode(
                    encoded,
                    expected
            );
            assertRequestFields(encoded);
        } finally {
            encoded.release();
        }

        FriendlyByteBuf canonical = new FriendlyByteBuf(Unpooled.buffer());
        try {
            writeRequestFields(canonical);
            assertEquals(
                    expected,
                    NeoForgeNetworking.ClientPreferencesPayload.STREAM_CODEC.decode(
                            canonical
                    )
            );
            assertEquals(0, canonical.readableBytes());
        } finally {
            canonical.release();
        }
    }

    @Test
    void acknowledgementCodecUsesCanonicalFieldOrder() {
        var expected = acknowledgement();
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        try {
            NeoForgeNetworking.ServerPreferencesAckPayload.STREAM_CODEC.encode(
                    encoded,
                    expected
            );
            assertAcknowledgementFields(encoded);
        } finally {
            encoded.release();
        }

        FriendlyByteBuf canonical = new FriendlyByteBuf(Unpooled.buffer());
        try {
            writeAcknowledgementFields(canonical);
            assertEquals(
                    expected,
                    NeoForgeNetworking.ServerPreferencesAckPayload.STREAM_CODEC.decode(
                            canonical
                    )
            );
            assertEquals(0, canonical.readableBytes());
        } finally {
            canonical.release();
        }
    }

    private static NeoForgeNetworking.ClientPreferencesPayload request() {
        return new NeoForgeNetworking.ClientPreferencesPayload(
                ClientPreferenceProtocol.WIRE_VERSION,
                29,
                true,
                "onekeyminer:cube",
                true,
                false
        );
    }

    private static NeoForgeNetworking.ServerPreferencesAckPayload acknowledgement() {
        return new NeoForgeNetworking.ServerPreferencesAckPayload(
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
    }

    private static void writeRequestFields(FriendlyByteBuf buffer) {
        buffer.writeVarInt(ClientPreferenceProtocol.WIRE_VERSION);
        buffer.writeVarInt(29);
        buffer.writeBoolean(true);
        buffer.writeUtf("onekeyminer:cube", NeoForgeNetworking.MAX_SHAPE_ID_LENGTH);
        buffer.writeBoolean(true);
        buffer.writeBoolean(false);
    }

    private static void assertRequestFields(FriendlyByteBuf buffer) {
        assertEquals(ClientPreferenceProtocol.WIRE_VERSION, buffer.readVarInt());
        assertEquals(29, buffer.readVarInt());
        assertEquals(true, buffer.readBoolean());
        assertEquals(
                "onekeyminer:cube",
                buffer.readUtf(NeoForgeNetworking.MAX_SHAPE_ID_LENGTH)
        );
        assertEquals(true, buffer.readBoolean());
        assertEquals(false, buffer.readBoolean());
        assertEquals(0, buffer.readableBytes());
    }

    private static void writeAcknowledgementFields(FriendlyByteBuf buffer) {
        buffer.writeVarInt(ClientPreferenceProtocol.WIRE_VERSION);
        buffer.writeVarInt(29);
        buffer.writeBoolean(true);
        buffer.writeUtf("onekeyminer:cube", NeoForgeNetworking.MAX_SHAPE_ID_LENGTH);
        buffer.writeVarInt(64);
        buffer.writeVarInt(16);
        buffer.writeBoolean(false);
        buffer.writeBoolean(false);
        buffer.writeBoolean(true);
        buffer.writeVarInt(ClientPreferenceProtocol.SUPPORTED_CAPABILITIES);
    }

    private static void assertAcknowledgementFields(FriendlyByteBuf buffer) {
        assertEquals(ClientPreferenceProtocol.WIRE_VERSION, buffer.readVarInt());
        assertEquals(29, buffer.readVarInt());
        assertEquals(true, buffer.readBoolean());
        assertEquals(
                "onekeyminer:cube",
                buffer.readUtf(NeoForgeNetworking.MAX_SHAPE_ID_LENGTH)
        );
        assertEquals(64, buffer.readVarInt());
        assertEquals(16, buffer.readVarInt());
        assertEquals(false, buffer.readBoolean());
        assertEquals(false, buffer.readBoolean());
        assertEquals(true, buffer.readBoolean());
        assertEquals(
                ClientPreferenceProtocol.SUPPORTED_CAPABILITIES,
                buffer.readVarInt()
        );
        assertEquals(0, buffer.readableBytes());
    }
}
