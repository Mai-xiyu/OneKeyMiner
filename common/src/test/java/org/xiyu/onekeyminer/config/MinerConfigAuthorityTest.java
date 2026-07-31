package org.xiyu.onekeyminer.config;

import org.junit.jupiter.api.Test;

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
}
