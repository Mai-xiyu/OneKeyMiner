package org.xiyu.onekeyminer.network;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.xiyu.onekeyminer.config.MinerConfig;

import static org.junit.jupiter.api.Assertions.*;

final class ClientPreferenceSessionTest {
    @AfterEach
    void clearSession() {
        ClientPreferenceSession.clear();
    }

    @Test
    void acknowledgedPolicyOverridesLocalPreviewLimits() {
        MinerConfig local = new MinerConfig();
        local.maxBlocks = 999;
        ClientPreferenceSession.accept(new ClientPreferenceAck(
                ClientPreferenceProtocol.WIRE_VERSION,
                1,
                true,
                "onekeyminer:small_tunnel",
                32,
                8,
                false,
                false,
                false,
                ClientPreferenceProtocol.SUPPORTED_CAPABILITIES
        ));

        ClientPreferenceSession.PreviewPolicy policy =
                ClientPreferenceSession.resolvePreviewPolicy(local);
        assertTrue(policy.serverAcknowledged());
        assertEquals(32, policy.maxBlocks());
        assertEquals(8, policy.maxDistance());
        assertFalse(policy.allowDiagonal());
    }
}
