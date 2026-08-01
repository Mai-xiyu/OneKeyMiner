package org.xiyu.onekeyminer.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ServerPreferencePolicyTest {

    @Test
    void serverPolicyGatesClientTeleportRequests() {
        ServerPreferencePolicy.Result result = ServerPreferencePolicy.apply(
                true,
                true,
                false,
                true
        );

        assertTrue(result.teleportDropsRequested());
        assertTrue(result.teleportExpRequested());
        assertFalse(result.teleportDropsApplied());
        assertTrue(result.teleportExpApplied());
        assertTrue(result.supports(ClientPreferenceProtocol.CAP_SHAPE_SELECTION));
        assertTrue(result.supports(ClientPreferenceProtocol.CAP_TELEPORT_DROPS));
        assertTrue(result.supports(ClientPreferenceProtocol.CAP_TELEPORT_EXP));
        assertTrue(result.supports(ClientPreferenceProtocol.CAP_SERVER_PREVIEW_POLICY));
    }
}
