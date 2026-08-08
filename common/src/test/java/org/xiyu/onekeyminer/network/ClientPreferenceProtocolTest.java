package org.xiyu.onekeyminer.network;

import org.junit.jupiter.api.Test;
import org.xiyu.onekeyminer.config.ConfigManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientPreferenceProtocolTest {

    @Test
    void extendedAcknowledgementUsesWireVersionFour() {
        assertEquals(4, ClientPreferenceProtocol.WIRE_VERSION);
        assertTrue((ClientPreferenceProtocol.SUPPORTED_CAPABILITIES
                & ClientPreferenceProtocol.CAP_SERVER_PREVIEW_POLICY) != 0);
    }

    @Test
    void keepsRawRequestsWhileAcknowledgingServerAppliedValues() {
        ConfigManager.ServerPreferenceSnapshot deniedPolicy = policy(false, false);

        ClientPreferenceProtocol.TeleportDecision denied =
                ClientPreferenceProtocol.decideTeleport(deniedPolicy, true, true);

        assertTrue(denied.requestedDrops());
        assertTrue(denied.requestedExperience());
        assertFalse(denied.appliedDrops());
        assertFalse(denied.appliedExperience());

        ClientPreferenceProtocol.TeleportDecision allowed =
                ClientPreferenceProtocol.decideTeleport(policy(true, true), true, true);

        assertTrue(allowed.requestedDrops());
        assertTrue(allowed.requestedExperience());
        assertTrue(allowed.appliedDrops());
        assertTrue(allowed.appliedExperience());
    }

    @Test
    void serverSnapshotSelectsCreativeLimitWithoutCopyingFullConfig() {
        ConfigManager.ServerPreferenceSnapshot policy = new ConfigManager.ServerPreferenceSnapshot(
                true,
                64,
                512,
                16,
                true,
                true,
                true
        );

        assertEquals(64, policy.maxBlocksFor(false));
        assertEquals(512, policy.maxBlocksFor(true));
    }

    private static ConfigManager.ServerPreferenceSnapshot policy(
            boolean allowDrops,
            boolean allowExperience
    ) {
        return new ConfigManager.ServerPreferenceSnapshot(
                true,
                64,
                512,
                16,
                true,
                allowDrops,
                allowExperience
        );
    }
}
