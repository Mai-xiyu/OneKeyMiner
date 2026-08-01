package org.xiyu.onekeyminer.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ServerPreferencePolicyTest {
    @Test
    void serverGatesClientTeleportRequests() {
        ServerPreferencePolicy.Result result = ServerPreferencePolicy.apply(
                true,
                true,
                false,
                true
        );

        assertFalse(result.teleportDropsApplied());
        assertTrue(result.teleportExpApplied());
        assertEquals(ClientPreferenceProtocol.SUPPORTED_CAPABILITIES, result.capabilities());
    }
}
