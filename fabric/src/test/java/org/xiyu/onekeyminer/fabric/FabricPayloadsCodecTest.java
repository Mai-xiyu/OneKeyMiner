package org.xiyu.onekeyminer.fabric;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import org.xiyu.onekeyminer.network.ClientPreferenceProtocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FabricPayloadsCodecTest {
    @Test
    void clientPreferencesRoundTripInCanonicalOrder() {
        var expected = new FabricPayloads.ClientPreferences(
                ClientPreferenceProtocol.WIRE_VERSION,
                42,
                true,
                "onekeyminer:cube",
                true,
                false
        );
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        expected.write(buf);

        assertEquals(expected, FabricPayloads.ClientPreferences.read(buf));
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void acknowledgementRoundTripsAllServerPolicyFields() {
        var expected = new FabricPayloads.ServerPreferencesAck(
                ClientPreferenceProtocol.WIRE_VERSION,
                42,
                true,
                "onekeyminer:column",
                64,
                16,
                false,
                true,
                false,
                ClientPreferenceProtocol.SUPPORTED_CAPABILITIES
        );
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        expected.write(buf);

        assertEquals(expected, FabricPayloads.ServerPreferencesAck.read(buf));
        assertEquals(0, buf.readableBytes());
    }
}
