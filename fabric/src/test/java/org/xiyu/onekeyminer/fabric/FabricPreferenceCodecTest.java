package org.xiyu.onekeyminer.fabric;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;
import org.xiyu.onekeyminer.network.ClientPreferenceProtocol;
import org.xiyu.onekeyminer.network.ClientPreferenceRequest;

import static org.junit.jupiter.api.Assertions.*;

final class FabricPreferenceCodecTest {
    @Test
    void requestAndAcknowledgementRoundTripInCanonicalOrder() {
        FriendlyByteBuf requestBuffer = new FriendlyByteBuf(Unpooled.buffer());
        ClientPreferenceRequest request = new ClientPreferenceRequest(
                true,
                "onekeyminer:small_tunnel",
                true,
                false
        );
        FabricPreferenceCodec.writeRequest(requestBuffer, 17, request);
        FabricPreferenceCodec.WireRequest decoded =
                FabricPreferenceCodec.readRequest(requestBuffer);
        assertEquals(ClientPreferenceProtocol.WIRE_VERSION, decoded.wireVersion());
        assertEquals(17, decoded.sequence());
        assertTrue(decoded.holding());
        assertEquals("onekeyminer:small_tunnel", decoded.shapeId());
        assertTrue(decoded.teleportDrops());
        assertFalse(decoded.teleportExp());

        ClientPreferenceAck acknowledgement = new ClientPreferenceAck(
                ClientPreferenceProtocol.WIRE_VERSION,
                17,
                true,
                "onekeyminer:small_tunnel",
                64,
                16,
                true,
                true,
                false,
                ClientPreferenceProtocol.SUPPORTED_CAPABILITIES
        );
        FriendlyByteBuf ackBuffer = new FriendlyByteBuf(Unpooled.buffer());
        FabricPreferenceCodec.writeAck(ackBuffer, acknowledgement);
        assertEquals(acknowledgement, FabricPreferenceCodec.readAck(ackBuffer));
    }

    @Test
    void trailingBytesAreRejected() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        FabricPreferenceCodec.writeRequest(
                buffer,
                1,
                new ClientPreferenceRequest(false, "onekeyminer:amorphous", false, false)
        );
        buffer.writeByte(1);

        assertThrows(
                IllegalArgumentException.class,
                () -> FabricPreferenceCodec.readRequest(buffer)
        );
    }
}
