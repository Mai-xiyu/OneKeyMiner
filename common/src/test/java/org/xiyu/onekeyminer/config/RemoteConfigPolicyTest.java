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
        edited.previewHudEnabled = false;
        edited.previewHudX = 93;
        edited.previewHudY = 17;
        edited.previewHudScale = 1.5f;
        edited.previewHudBackgroundAlpha = 0;
        edited.previewHudShowShape = false;
        edited.previewHudShowCount = false;
        edited.previewHudShowMax = false;

        MinerConfig merged = RemoteConfigPolicy.mergeClientPreferences(current, edited);

        assertEquals(64, merged.maxBlocks);
        assertFalse(merged.allowClientTeleportDrops);
        assertEquals("onekeyminer:cube", merged.selectedShape);
        assertNull(merged.shapeMode);
        assertTrue(merged.teleportDrops);
        assertTrue(merged.teleportExp);
        assertFalse(merged.previewHudEnabled);
        assertEquals(93, merged.previewHudX);
        assertEquals(17, merged.previewHudY);
        assertEquals(1.5f, merged.previewHudScale);
        assertEquals(0, merged.previewHudBackgroundAlpha);
        assertFalse(merged.previewHudShowShape);
        assertFalse(merged.previewHudShowCount);
        assertFalse(merged.previewHudShowMax);
        assertTrue(current.previewHudEnabled);
    }
}
