package org.xiyu.onekeyminer.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RemoteConfigPolicyTest {

    @Test
    void onlyDedicatedRemoteConnectionsRestrictServerSettings() {
        assertTrue(RemoteConfigPolicy.canEditServerSettings(false, false));
        assertTrue(RemoteConfigPolicy.canEditServerSettings(true, true));
        assertFalse(RemoteConfigPolicy.canEditServerSettings(true, false));
    }

    @Test
    void remoteMergeKeepsServerAuthorityAndAppliesOnlyClientPreferences() {
        MinerConfig current = new MinerConfig();
        current.maxBlocks = 64;
        current.allowClientTeleportDrops = false;

        MinerConfig edited = current.copy();
        edited.maxBlocks = 1024;
        edited.allowClientTeleportDrops = true;
        edited.selectedShape = "onekeyminer:cube";
        edited.shapeMode = MinerConfig.ShapeMode.CUBE;
        edited.teleportDrops = true;
        edited.teleportExp = true;

        MinerConfig merged = RemoteConfigPolicy.mergeClientPreferences(current, edited);

        assertEquals(64, merged.maxBlocks);
        assertFalse(merged.allowClientTeleportDrops);
        assertEquals("onekeyminer:cube", merged.selectedShape);
        assertNull(merged.shapeMode);
        assertTrue(merged.teleportDrops);
        assertTrue(merged.teleportExp);
    }
}
