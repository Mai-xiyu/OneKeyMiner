package org.xiyu.onekeyminer.fabric;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import org.xiyu.onekeyminer.network.ClientPreferenceProtocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class FabricPayloadsCodecTest {
    private static final int SEQUENCE = 19;
    private static final String SHAPE = "onekeyminer:cube";

    @Test
    void requestCodecUsesCanonicalFieldOrder() {
        var expected = new FabricPayloads.ClientPreferencesPayload(
                ClientPreferenceProtocol.WIRE_VERSION,
                SEQUENCE,
                true,
                SHAPE,
                true,
                false
        );
        FriendlyByteBuf encoded = buffer();
        try {
            FabricPayloads.ClientPreferencesPayload.STREAM_CODEC.encode(encoded, expected);
            assertRequestFields(encoded);
        } finally {
            encoded.release();
        }

        FriendlyByteBuf canonical = buffer();
        try {
            writeRequestFields(canonical);
            assertEquals(
                    expected,
                    FabricPayloads.ClientPreferencesPayload.STREAM_CODEC.decode(canonical)
            );
            assertEquals(0, canonical.readableBytes());
        } finally {
            canonical.release();
        }
    }

    @Test
    void acknowledgementCodecUsesCanonicalFieldOrder() {
        var expected = new FabricPayloads.ServerPreferencesAckPayload(
                ClientPreferenceProtocol.WIRE_VERSION,
                SEQUENCE,
                true,
                SHAPE,
                64,
                16,
                false,
                false,
                true,
                ClientPreferenceProtocol.SUPPORTED_CAPABILITIES
        );
        FriendlyByteBuf encoded = buffer();
        try {
            FabricPayloads.ServerPreferencesAckPayload.STREAM_CODEC.encode(encoded, expected);
            assertAcknowledgementFields(encoded);
        } finally {
            encoded.release();
        }

        FriendlyByteBuf canonical = buffer();
        try {
            writeAcknowledgementFields(canonical);
            assertEquals(
                    expected,
                    FabricPayloads.ServerPreferencesAckPayload.STREAM_CODEC.decode(canonical)
            );
            assertEquals(0, canonical.readableBytes());
        } finally {
            canonical.release();
        }
    }

    @Test
    void codecsRejectTrailingBytes() {
        FriendlyByteBuf request = buffer();
        try {
            writeRequestFields(request);
            request.writeByte(0);
            assertThrows(
                    IllegalArgumentException.class,
                    () -> FabricPayloads.ClientPreferencesPayload.STREAM_CODEC.decode(request)
            );
        } finally {
            request.release();
        }

        FriendlyByteBuf acknowledgement = buffer();
        try {
            writeAcknowledgementFields(acknowledgement);
            acknowledgement.writeByte(0);
            assertThrows(
                    IllegalArgumentException.class,
                    () -> FabricPayloads.ServerPreferencesAckPayload.STREAM_CODEC.decode(
                            acknowledgement
                    )
            );
        } finally {
            acknowledgement.release();
        }
    }

    @Test
    void acknowledgementCodecRejectsOutOfRangeServerPolicy() {
        FriendlyByteBuf acknowledgement = buffer();
        try {
            writeAcknowledgementFields(
                    acknowledgement,
                    ClientPreferenceProtocol.MAX_APPLIED_BLOCKS + 1
            );
            assertThrows(
                    IllegalArgumentException.class,
                    () -> FabricPayloads.ServerPreferencesAckPayload.STREAM_CODEC.decode(
                            acknowledgement
                    )
            );
        } finally {
            acknowledgement.release();
        }
    }

    private static FriendlyByteBuf buffer() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    private static void writeRequestFields(FriendlyByteBuf buffer) {
        buffer.writeVarInt(ClientPreferenceProtocol.WIRE_VERSION);
        buffer.writeVarInt(SEQUENCE);
        buffer.writeBoolean(true);
        buffer.writeUtf(SHAPE, FabricPayloads.MAX_SHAPE_ID_LENGTH);
        buffer.writeBoolean(true);
        buffer.writeBoolean(false);
    }

    private static void assertRequestFields(FriendlyByteBuf buffer) {
        assertEquals(ClientPreferenceProtocol.WIRE_VERSION, buffer.readVarInt());
        assertEquals(SEQUENCE, buffer.readVarInt());
        assertEquals(true, buffer.readBoolean());
        assertEquals(SHAPE, buffer.readUtf(FabricPayloads.MAX_SHAPE_ID_LENGTH));
        assertEquals(true, buffer.readBoolean());
        assertEquals(false, buffer.readBoolean());
        assertEquals(0, buffer.readableBytes());
    }

    private static void writeAcknowledgementFields(FriendlyByteBuf buffer) {
        writeAcknowledgementFields(buffer, 64);
    }

    private static void writeAcknowledgementFields(
            FriendlyByteBuf buffer,
            int maxBlocks
    ) {
        buffer.writeVarInt(ClientPreferenceProtocol.WIRE_VERSION);
        buffer.writeVarInt(SEQUENCE);
        buffer.writeBoolean(true);
        buffer.writeUtf(SHAPE, FabricPayloads.MAX_SHAPE_ID_LENGTH);
        buffer.writeVarInt(maxBlocks);
        buffer.writeVarInt(16);
        buffer.writeBoolean(false);
        buffer.writeBoolean(false);
        buffer.writeBoolean(true);
        buffer.writeVarInt(ClientPreferenceProtocol.SUPPORTED_CAPABILITIES);
    }

    private static void assertAcknowledgementFields(FriendlyByteBuf buffer) {
        assertEquals(ClientPreferenceProtocol.WIRE_VERSION, buffer.readVarInt());
        assertEquals(SEQUENCE, buffer.readVarInt());
        assertEquals(true, buffer.readBoolean());
        assertEquals(SHAPE, buffer.readUtf(FabricPayloads.MAX_SHAPE_ID_LENGTH));
        assertEquals(64, buffer.readVarInt());
        assertEquals(16, buffer.readVarInt());
        assertEquals(false, buffer.readBoolean());
        assertEquals(false, buffer.readBoolean());
        assertEquals(true, buffer.readBoolean());
        assertEquals(ClientPreferenceProtocol.SUPPORTED_CAPABILITIES, buffer.readVarInt());
        assertEquals(0, buffer.readableBytes());
    }
}
