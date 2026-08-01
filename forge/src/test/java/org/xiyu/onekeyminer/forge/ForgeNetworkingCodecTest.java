package org.xiyu.onekeyminer.forge;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import org.xiyu.onekeyminer.network.ClientPreferenceProtocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ForgeNetworkingCodecTest {

    @Test
    void requestAndAcknowledgementCodecsRoundTripEveryField() {
        var request = new ForgeNetworking.ClientPreferencesPacket(
                ClientPreferenceProtocol.WIRE_VERSION,
                23,
                true,
                "onekeyminer:cube",
                true,
                false
        );
        var ack = new ForgeNetworking.ServerPreferencesAckPacket(
                ClientPreferenceProtocol.WIRE_VERSION,
                23,
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
    void acknowledgementCodecUsesCanonicalWireOrder() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            acknowledgement().write(buffer);
            assertEquals(3, buffer.readVarInt());
            assertEquals(41, buffer.readVarInt());
            assertEquals(false, buffer.readBoolean());
            assertEquals("onekeyminer:column", buffer.readUtf(128));
            assertEquals(79, buffer.readVarInt());
            assertEquals(23, buffer.readVarInt());
            assertEquals(true, buffer.readBoolean());
            assertEquals(true, buffer.readBoolean());
            assertEquals(false, buffer.readBoolean());
            assertEquals(11, buffer.readVarInt());
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
                    ForgeNetworking.ServerPreferencesAckPacket.fromNetwork(buffer)
            );
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    private static ForgeNetworking.ServerPreferencesAckPacket acknowledgement() {
        return new ForgeNetworking.ServerPreferencesAckPacket(
                3, 41, false, "onekeyminer:column", 79, 23,
                true, true, false, 11
        );
    }

    private static void writeCanonicalAcknowledgement(FriendlyByteBuf buffer) {
        buffer.writeVarInt(3);
        buffer.writeVarInt(41);
        buffer.writeBoolean(false);
        buffer.writeUtf("onekeyminer:column", 128);
        buffer.writeVarInt(79);
        buffer.writeVarInt(23);
        buffer.writeBoolean(true);
        buffer.writeBoolean(true);
        buffer.writeBoolean(false);
        buffer.writeVarInt(11);
    }

    private static ForgeNetworking.ClientPreferencesPacket roundTripRequest(
            ForgeNetworking.ClientPreferencesPacket packet
    ) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.write(buffer);
            return ForgeNetworking.ClientPreferencesPacket.fromNetwork(buffer);
        } finally {
            buffer.release();
        }
    }

    private static ForgeNetworking.ServerPreferencesAckPacket roundTripAck(
            ForgeNetworking.ServerPreferencesAckPacket packet
    ) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.write(buffer);
            return ForgeNetworking.ServerPreferencesAckPacket.fromNetwork(buffer);
        } finally {
            buffer.release();
        }
    }
}
