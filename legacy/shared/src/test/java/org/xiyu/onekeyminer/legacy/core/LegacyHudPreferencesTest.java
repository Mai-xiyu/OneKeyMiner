package org.xiyu.onekeyminer.legacy.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.io.Reader;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LegacyHudPreferencesTest {
    @TempDir Path directory;

    @Test
    void persistsClientOnlySettingsAndBoundsInvalidApiInput() throws Exception {
        Path config = directory.resolve("hud.properties");
        LegacyHudSettings settings = new LegacyHudSettings(false, -2, 42, Float.NaN, 900, false, true, false);
        LegacyHudPreferences.set(config, settings);
        Properties values = new Properties();
        try (Reader reader = Files.newBufferedReader(config)) { values.load(reader); }
        assertEquals("0", values.getProperty("x"));
        assertEquals("42", values.getProperty("y"));
        assertEquals("1.0", values.getProperty("scale"));
        assertEquals("255", values.getProperty("backgroundAlpha"));
        assertFalse(LegacyHudPreferences.get(config).enabled);
        assertTrue(LegacyHudPreferences.get(config).showCount);
    }
}
