package org.xiyu.onekeyminer.network;

import org.junit.jupiter.api.Test;
import org.xiyu.onekeyminer.config.MinerConfig;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientPreferenceProtocolTest {

    @Test
    void keepsRawRequestsWhileAcknowledgingServerAppliedValues() {
        MinerConfig serverConfig = new MinerConfig();
        serverConfig.allowClientTeleportDrops = false;
        serverConfig.allowClientTeleportExp = false;

        ClientPreferenceProtocol.TeleportDecision denied =
                ClientPreferenceProtocol.decideTeleport(serverConfig, true, true);

        assertTrue(denied.requestedDrops());
        assertTrue(denied.requestedExperience());
        assertFalse(denied.appliedDrops());
        assertFalse(denied.appliedExperience());

        serverConfig.allowClientTeleportDrops = true;
        serverConfig.allowClientTeleportExp = true;
        ClientPreferenceProtocol.TeleportDecision allowed =
                ClientPreferenceProtocol.decideTeleport(serverConfig, true, true);

        assertTrue(allowed.requestedDrops());
        assertTrue(allowed.requestedExperience());
        assertTrue(allowed.appliedDrops());
        assertTrue(allowed.appliedExperience());
    }
}
