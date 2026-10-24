package org.xiyu.onekeyminer.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class PreviewHudSettingsTest {
    @Test
    void boundsSettingsForInvalidConfigAndApiInput() {
        OneKeyMinerAPI.PreviewHudSettings settings = new OneKeyMinerAPI.PreviewHudSettings(
                true, -1, -8, Float.NaN, 800, true, true, true);
        assertEquals(0, settings.x());
        assertEquals(0, settings.y());
        assertEquals(1.0f, settings.scale());
        assertEquals(255, settings.backgroundAlpha());
    }
}
