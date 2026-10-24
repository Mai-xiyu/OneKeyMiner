package org.xiyu.onekeyminer.legacy.core;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Properties;

public final class LegacyHudPreferences {
    private static LegacyHudSettings current;

    private LegacyHudPreferences() {
    }

    public static synchronized LegacyHudSettings get(Path file) {
        if (current != null) return current;
        Properties values = new Properties();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                values.load(reader);
            } catch (IOException exception) {
                java.util.logging.Logger.getLogger("OneKeyMiner").warning("Unable to read HUD preferences: " + file);
                values.clear();
            }
        }
        current = new LegacyHudSettings(bool(values, "enabled", true), number(values, "x", 6), number(values, "y", 6),
                decimal(values, "scale", 1), number(values, "backgroundAlpha", 128), bool(values, "showShape", true),
                bool(values, "showCount", true), bool(values, "showMax", true));
        if (!Files.exists(file)) set(file, current);
        return current;
    }

    public static synchronized void set(Path file, LegacyHudSettings settings) {
        Objects.requireNonNull(settings, "settings");
        Properties values = new Properties();
        values.setProperty("enabled", Boolean.toString(settings.enabled));
        values.setProperty("x", Integer.toString(settings.x));
        values.setProperty("y", Integer.toString(settings.y));
        values.setProperty("scale", Float.toString(settings.scale));
        values.setProperty("backgroundAlpha", Integer.toString(settings.backgroundAlpha));
        values.setProperty("showShape", Boolean.toString(settings.showShape));
        values.setProperty("showCount", Boolean.toString(settings.showCount));
        values.setProperty("showMax", Boolean.toString(settings.showMax));
        Path temporary = null;
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            temporary = Files.createTempFile(file.toAbsolutePath().getParent(), "onekeyminer-hud-", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                values.store(writer, null);
            }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            current = settings;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save HUD preferences: " + file, exception);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private static int number(Properties values, String key, int fallback) {
        try { return Integer.parseInt(values.getProperty(key, Integer.toString(fallback))); }
        catch (NumberFormatException exception) { return fallback; }
    }

    private static float decimal(Properties values, String key, float fallback) {
        try { return Float.parseFloat(values.getProperty(key, Float.toString(fallback))); }
        catch (NumberFormatException exception) { return fallback; }
    }

    private static boolean bool(Properties values, String key, boolean fallback) {
        String value = values.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }
}
