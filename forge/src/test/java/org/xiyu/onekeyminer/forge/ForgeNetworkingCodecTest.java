package org.xiyu.onekeyminer.forge;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import org.xiyu.onekeyminer.network.ClientPreferenceProtocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ForgeNetworkingCodecTest {

    @Test
    void requestCodecUsesCanonicalFieldOrder() {
        var expected = request();
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        try {
            expected.write(encoded);
            assertRequestFields(encoded);
        } finally {
            encoded.release();
        }

        FriendlyByteBuf canonical = new FriendlyByteBuf(Unpooled.buffer());
        try {
            writeRequestFields(canonical);
            assertEquals(
                    expected,
                    ForgeNetworking.ClientPreferencesPacket.fromNetwork(canonical)
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
            expected.write(encoded);
            assertAcknowledgementFields(encoded);
        } finally {
            encoded.release();
        }

        FriendlyByteBuf canonical = new FriendlyByteBuf(Unpooled.buffer());
        try {
            writeAcknowledgementFields(canonical);
            assertEquals(
                    expected,
                    ForgeNetworking.ServerPreferencesAckPacket.fromNetwork(canonical)
            );
            assertEquals(0, canonical.readableBytes());
        } finally {
            canonical.release();
        }
    }

    @Test
    void codecsRejectTrailingBytes() {
        FriendlyByteBuf request = new FriendlyByteBuf(Unpooled.buffer());
        try {
            writeRequestFields(request);
            request.writeByte(0);
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ForgeNetworking.ClientPreferencesPacket.fromNetwork(request)
            );
        } finally {
            request.release();
        }

        FriendlyByteBuf acknowledgement = new FriendlyByteBuf(Unpooled.buffer());
        try {
            writeAcknowledgementFields(acknowledgement);
            acknowledgement.writeByte(0);
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ForgeNetworking.ServerPreferencesAckPacket.fromNetwork(
                            acknowledgement
                    )
            );
        } finally {
            acknowledgement.release();
        }
    }

    private static ForgeNetworking.ClientPreferencesPacket request() {
        return new ForgeNetworking.ClientPreferencesPacket(
                ClientPreferenceProtocol.WIRE_VERSION,
                23,
                true,
                "onekeyminer:cube",
                true,
                false
        );
    }

    private static ForgeNetworking.ServerPreferencesAckPacket acknowledgement() {
        return new ForgeNetworking.ServerPreferencesAckPacket(
                ClientPreferenceProtocol.WIRE_VERSION,
                23,
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
        buffer.writeVarInt(23);
        buffer.writeBoolean(true);
        buffer.writeUtf("onekeyminer:cube", ForgeNetworking.MAX_SHAPE_ID_LENGTH);
        buffer.writeBoolean(true);
        buffer.writeBoolean(false);
    }

    private static void assertRequestFields(FriendlyByteBuf buffer) {
        assertEquals(ClientPreferenceProtocol.WIRE_VERSION, buffer.readVarInt());
        assertEquals(23, buffer.readVarInt());
        assertEquals(true, buffer.readBoolean());
        assertEquals(
                "onekeyminer:cube",
                buffer.readUtf(ForgeNetworking.MAX_SHAPE_ID_LENGTH)
        );
        assertEquals(true, buffer.readBoolean());
        assertEquals(false, buffer.readBoolean());
        assertEquals(0, buffer.readableBytes());
    }

    private static void writeAcknowledgementFields(FriendlyByteBuf buffer) {
        buffer.writeVarInt(ClientPreferenceProtocol.WIRE_VERSION);
        buffer.writeVarInt(23);
        buffer.writeBoolean(true);
        buffer.writeUtf("onekeyminer:cube", ForgeNetworking.MAX_SHAPE_ID_LENGTH);
        buffer.writeVarInt(64);
        buffer.writeVarInt(16);
        buffer.writeBoolean(false);
        buffer.writeBoolean(false);
        buffer.writeBoolean(true);
        buffer.writeVarInt(ClientPreferenceProtocol.SUPPORTED_CAPABILITIES);
    }

    private static void assertAcknowledgementFields(FriendlyByteBuf buffer) {
        assertEquals(ClientPreferenceProtocol.WIRE_VERSION, buffer.readVarInt());
        assertEquals(23, buffer.readVarInt());
        assertEquals(true, buffer.readBoolean());
        assertEquals(
                "onekeyminer:cube",
                buffer.readUtf(ForgeNetworking.MAX_SHAPE_ID_LENGTH)
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
