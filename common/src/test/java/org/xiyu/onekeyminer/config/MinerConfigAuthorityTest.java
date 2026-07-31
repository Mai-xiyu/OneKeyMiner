package org.xiyu.onekeyminer.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MinerConfigAuthorityTest {

    @Test
    void serverMustAllowRequestedDropTeleport() {
        MinerConfig config = new MinerConfig();

        config.allowClientTeleportDrops = false;
        assertFalse(config.isDropTeleportEnabled(true));

        config.allowClientTeleportDrops = true;
        assertFalse(config.isDropTeleportEnabled(false));
        assertTrue(config.isDropTeleportEnabled(true));
    }

    @Test
    void serverMustAllowRequestedExperienceTeleport() {
        MinerConfig config = new MinerConfig();

        config.allowClientTeleportExp = false;
        assertFalse(config.isExperienceTeleportEnabled(true));

        config.allowClientTeleportExp = true;
        assertFalse(config.isExperienceTeleportEnabled(false));
        assertTrue(config.isExperienceTeleportEnabled(true));
    }

    @Test
    void applyingRemotePreferencesMustPreserveServerPolicy() {
        MinerConfig serverConfig = new MinerConfig();
        serverConfig.allowClientTeleportDrops = false;
        serverConfig.allowClientTeleportExp = false;
        serverConfig.maxBlocks = 32;

        MinerConfig clientPreferences = new MinerConfig();
        clientPreferences.selectedShape = "onekeyminer:cube";
        clientPreferences.teleportDrops = true;
        clientPreferences.teleportExp = true;
        clientPreferences.allowClientTeleportDrops = true;
        clientPreferences.allowClientTeleportExp = true;
        clientPreferences.maxBlocks = 1024;

        serverConfig.applyClientPreferences(clientPreferences);

        assertTrue(serverConfig.teleportDrops);
        assertTrue(serverConfig.teleportExp);
        assertFalse(serverConfig.allowClientTeleportDrops);
        assertFalse(serverConfig.allowClientTeleportExp);
        assertFalse(serverConfig.isDropTeleportEnabled(true));
        assertFalse(serverConfig.isExperienceTeleportEnabled(true));
        assertEquals(32, serverConfig.maxBlocks);
    }
}
