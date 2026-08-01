package org.xiyu.onekeyminer.neoforge;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import org.xiyu.onekeyminer.network.ClientPreferenceProtocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NeoForgeNetworkingCodecTest {
    @Test
    void clientPreferencesRoundTripInCanonicalOrder() {
        var expected = new NeoForgeNetworking.ClientPreferencesPayload(
                ClientPreferenceProtocol.WIRE_VERSION,
                5,
                true,
                "onekeyminer:cube",
                true,
                true
        );
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        expected.write(buf);

        assertEquals(expected, NeoForgeNetworking.ClientPreferencesPayload.READER.apply(buf));
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void acknowledgementRoundTripsAllServerPolicyFields() {
        var expected = new NeoForgeNetworking.ServerPreferencesAckPayload(
                ClientPreferenceProtocol.WIRE_VERSION,
                5,
                false,
                "onekeyminer:amorphous",
                12,
                6,
                false,
                false,
                true,
                ClientPreferenceProtocol.SUPPORTED_CAPABILITIES
        );
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        expected.write(buf);

        assertEquals(expected, NeoForgeNetworking.ServerPreferencesAckPayload.READER.apply(buf));
        assertEquals(0, buf.readableBytes());
    }
}
