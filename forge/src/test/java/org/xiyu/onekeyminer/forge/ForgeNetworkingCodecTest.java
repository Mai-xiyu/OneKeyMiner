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
                true,
                ClientPreferenceProtocol.SUPPORTED_CAPABILITIES
        );

        assertEquals(request, roundTripRequest(request));
        assertEquals(ack, roundTripAck(ack));
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
