package org.xiyu.onekeyminer.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MinerConfigClientPreferencesTest {
    @Test
    void applyingClientPreferencesCannotOverwriteServerLimitsOrPolicy() {
        MinerConfig server = new MinerConfig();
        server.maxBlocks = 17;
        server.allowClientTeleportDrops = false;
        MinerConfig client = new MinerConfig();
        client.selectedShape = "onekeyminer:cube";
        client.teleportDrops = true;
        client.maxBlocks = 9999;
        client.allowClientTeleportDrops = true;

        server.applyClientPreferences(client);

        assertEquals("onekeyminer:cube", server.selectedShape);
        assertTrue(server.teleportDrops);
        assertEquals(17, server.maxBlocks);
        assertFalse(server.allowClientTeleportDrops);
        assertFalse(server.isDropTeleportEnabled(server.teleportDrops));
    }

    @Test
    void copyNormalizesNullCollections() {
        MinerConfig config = new MinerConfig();
        config.blacklist = null;
        config.seedWhitelist = null;

        MinerConfig copy = config.copy();

        assertNotNull(copy.blacklist);
        assertNotNull(copy.seedWhitelist);
    }
}
