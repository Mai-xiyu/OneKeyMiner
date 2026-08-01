package org.xiyu.onekeyminer.forge;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;
import org.xiyu.onekeyminer.network.ClientPreferenceProtocol;

import static org.junit.jupiter.api.Assertions.*;

final class ForgePreferenceCodecTest {
    @Test
    void bothDirectionsRoundTripInCanonicalOrder() {
        ForgeNetworking.ClientPreferencesPacket request =
                new ForgeNetworking.ClientPreferencesPacket(
                        ClientPreferenceProtocol.WIRE_VERSION,
                        9,
                        true,
                        "onekeyminer:large_tunnel",
                        false,
                        true
                );
        FriendlyByteBuf requestBuffer = new FriendlyByteBuf(Unpooled.buffer());
        request.write(requestBuffer);
        assertEquals(request, ForgeNetworking.ClientPreferencesPacket.fromNetwork(requestBuffer));

        ClientPreferenceAck commonAck = new ClientPreferenceAck(
                ClientPreferenceProtocol.WIRE_VERSION,
                9,
                true,
                "onekeyminer:large_tunnel",
                32,
                12,
                false,
                false,
                true,
                ClientPreferenceProtocol.SUPPORTED_CAPABILITIES
        );
        ForgeNetworking.ServerPreferencesAckPacket acknowledgement =
                ForgeNetworking.ServerPreferencesAckPacket.fromCommon(commonAck);
        FriendlyByteBuf ackBuffer = new FriendlyByteBuf(Unpooled.buffer());
        acknowledgement.write(ackBuffer);
        ForgeNetworking.ServerPreferencesAckPacket decoded =
                ForgeNetworking.ServerPreferencesAckPacket.fromNetwork(ackBuffer);
        assertEquals(acknowledgement, decoded);
        assertEquals(commonAck, decoded.toCommon());
    }

    @Test
    void trailingBytesAreRejected() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        new ForgeNetworking.ClientPreferencesPacket(
                ClientPreferenceProtocol.WIRE_VERSION,
                1,
                false,
                "onekeyminer:amorphous",
                false,
                false
        ).write(buffer);
        buffer.writeByte(1);

        assertThrows(
                IllegalArgumentException.class,
                () -> ForgeNetworking.ClientPreferencesPacket.fromNetwork(buffer)
        );
    }
}
