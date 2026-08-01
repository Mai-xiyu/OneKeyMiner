package org.xiyu.onekeyminer.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MinerConfigClientPreferencesTest {

    @Test
    void remoteClientPreferencesDoNotOverwriteServerOwnedSettings() {
        MinerConfig serverOwned = new MinerConfig();
        serverOwned.enabled = true;
        serverOwned.maxBlocks = 64;
        serverOwned.allowClientTeleportDrops = false;
        serverOwned.allowClientTeleportExp = true;

        MinerConfig remoteClient = serverOwned.copy();
        remoteClient.enabled = false;
        remoteClient.maxBlocks = 512;
        remoteClient.selectedShape = "onekeyminer:column";
        remoteClient.shapeMode = MinerConfig.ShapeMode.CUBE;
        remoteClient.teleportDrops = true;
        remoteClient.teleportExp = false;
        remoteClient.allowClientTeleportDrops = true;
        remoteClient.allowClientTeleportExp = false;

        serverOwned.applyClientPreferences(remoteClient);

        assertTrue(serverOwned.enabled);
        assertEquals(64, serverOwned.maxBlocks);
        assertFalse(serverOwned.allowClientTeleportDrops);
        assertTrue(serverOwned.allowClientTeleportExp);
        assertEquals("onekeyminer:column", serverOwned.selectedShape);
        assertNull(serverOwned.shapeMode);
        assertTrue(serverOwned.teleportDrops);
        assertFalse(serverOwned.teleportExp);
    }
}
