package org.xiyu.onekeyminer.network;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.xiyu.onekeyminer.config.MinerConfig;

import static org.junit.jupiter.api.Assertions.*;

class ClientPreferenceSessionTest {
    @AfterEach
    void clearSession() {
        ClientPreferenceSession.clear();
    }

    @Test
    void localPreviewValuesAreUsedBeforeAcknowledgement() {
        MinerConfig config = new MinerConfig();
        config.selectedShape = "onekeyminer:cube";
        config.maxBlocks = 123;

        var policy = ClientPreferenceSession.resolvePreviewPolicy(config);

        assertEquals("onekeyminer:cube", policy.shapeId());
        assertEquals(123, policy.maxBlocks());
        assertFalse(policy.serverAcknowledged());
    }

    @Test
    void acknowledgementControlsPreviewWithoutOverwritingLocalRequest() {
        MinerConfig config = new MinerConfig();
        config.selectedShape = "onekeyminer:cube";
        ClientPreferenceSession.accept(new ClientPreferenceAck(
                ClientPreferenceProtocol.WIRE_VERSION,
                7,
                true,
                "onekeyminer:column",
                32,
                8,
                false,
                false,
                false,
                ClientPreferenceProtocol.SUPPORTED_CAPABILITIES
        ));

        var policy = ClientPreferenceSession.resolvePreviewPolicy(config);

        assertEquals("onekeyminer:column", policy.shapeId());
        assertEquals(32, policy.maxBlocks());
        assertTrue(policy.serverAcknowledged());
        assertEquals("onekeyminer:cube", config.selectedShape);
    }
}
