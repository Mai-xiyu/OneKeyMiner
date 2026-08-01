package org.xiyu.onekeyminer.forge;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import org.xiyu.onekeyminer.network.ClientPreferenceProtocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForgeNetworkingCodecTest {
    @Test
    void clientPreferencesRoundTripInCanonicalOrder() {
        var expected = new ForgeNetworking.ClientPreferencesPacket(
                ClientPreferenceProtocol.WIRE_VERSION,
                8,
                true,
                "onekeyminer:cube",
                false,
                true
        );
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        expected.write(buf);

        assertEquals(expected, ForgeNetworking.ClientPreferencesPacket.fromNetwork(buf));
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void acknowledgementRoundTripsAllServerPolicyFields() {
        var expected = new ForgeNetworking.ServerPreferencesAckPacket(
                ClientPreferenceProtocol.WIRE_VERSION,
                8,
                true,
                "onekeyminer:column",
                96,
                24,
                false,
                true,
                false,
                ClientPreferenceProtocol.SUPPORTED_CAPABILITIES
        );
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        expected.write(buf);

        assertEquals(expected, ForgeNetworking.ServerPreferencesAckPacket.fromNetwork(buf));
        assertEquals(0, buf.readableBytes());
    }
}
