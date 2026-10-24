package org.xiyu.onekeyminer.config;

import org.junit.jupiter.api.Test;
import org.xiyu.onekeyminer.api.OneKeyMinerAPI.PreviewHudSettings;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PreviewHudSettingsTest {
    @Test
    void apiBoundsInvalidValuesAndUsesFiniteScale() {
        PreviewHudSettings settings = new PreviewHudSettings(true, -1, -8, Float.NaN, 800, true, true, true);
        assertEquals(0, settings.x());
        assertEquals(0, settings.y());
        assertEquals(1.0f, settings.scale());
        assertEquals(255, settings.backgroundAlpha());
        assertEquals(0.5f, new PreviewHudSettings(true, 0, 0, -2, 0, true, true, true).scale());
        assertEquals(2.0f, new PreviewHudSettings(true, 0, 0, 80, 0, true, true, true).scale());
    }

    @Test
    void defensiveCopiesKeepPreferencesIndependent() {
        MinerConfig original = new MinerConfig();
        original.previewHudX = 123;
        original.previewHudScale = 1.75f;
        MinerConfig snapshot = original.copy();
        snapshot.previewHudX = 456;
        assertEquals(123, original.previewHudX);
        assertEquals(1.75f, snapshot.previewHudScale);
        snapshot.copyPreviewHudFrom(new MinerConfig());
        assertEquals(6, snapshot.previewHudX);
        assertEquals(1.0f, snapshot.previewHudScale);
    }
}
