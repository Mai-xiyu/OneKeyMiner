package org.xiyu.onekeyminer.network;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.xiyu.onekeyminer.config.MinerConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientPreferenceSessionTest {

    @AfterEach
    void clearSession() {
        ClientPreferenceSession.clear();
    }

    @Test
    void previewUsesLocalValuesBeforeServerAcknowledgement() {
        MinerConfig local = new MinerConfig();
        local.selectedShape = "onekeyminer:cube";
        local.maxBlocks = 91;

        var policy = ClientPreferenceSession.resolvePreviewPolicy(local);

        assertFalse(policy.serverAcknowledged());
        assertEquals("onekeyminer:cube", policy.shapeId());
        assertEquals(91, policy.maxBlocks());
    }

    @Test
    void previewUsesServerAppliedValuesWithoutOverwritingLocalRequest() {
        MinerConfig local = new MinerConfig();
        local.selectedShape = "onekeyminer:cube";
        local.maxBlocks = 91;
        ClientPreferenceSession.accept(new ClientPreferenceAck(
                ClientPreferenceProtocol.WIRE_VERSION,
                7,
                false,
                "onekeyminer:amorphous",
                12,
                5,
                false,
                false,
                true,
                ClientPreferenceProtocol.SUPPORTED_CAPABILITIES
        ));

        var policy = ClientPreferenceSession.resolvePreviewPolicy(local);

        assertTrue(policy.serverAcknowledged());
        assertFalse(policy.enabled());
        assertEquals("onekeyminer:amorphous", policy.shapeId());
        assertEquals(12, policy.maxBlocks());
        assertEquals(5, policy.maxDistance());
        assertFalse(policy.allowDiagonal());
        assertEquals("onekeyminer:cube", local.selectedShape);
        assertEquals(91, local.maxBlocks);
    }
}
