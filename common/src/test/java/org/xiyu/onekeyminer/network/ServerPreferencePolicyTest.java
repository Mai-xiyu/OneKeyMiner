package org.xiyu.onekeyminer.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerPreferencePolicyTest {
    @Test
    void serverPolicyCanDenyClientTeleportRequests() {
        var result = ServerPreferencePolicy.apply(true, true, false, true);

        assertFalse(result.teleportDropsApplied());
        assertTrue(result.teleportExpApplied());
        assertTrue(result.teleportDropsRequested());
        assertTrue(result.supports(ClientPreferenceProtocol.CAP_SERVER_PREVIEW_POLICY));
    }
}
