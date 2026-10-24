package org.xiyu.onekeyminer.legacy.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.xiyu.onekeyminer.api.OneKeyMinerAPI.PreviewHudSettings;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class LegacyFabricConfig {
    public boolean enabled = true;
    public int maxBlocks = 64;
    public int maxDistance = 16;
    public boolean allowDiagonal = true;
    public boolean enableInteraction = true;
    public boolean enablePlanting = true;
    public boolean allowBareHand = true;
    public boolean consumeHunger = true;
    public int minHungerLevel = 1;
    public int preserveDurability = 1;
    public List<String> blacklist = new ArrayList<>();
    public volatile PreviewHudSettings previewHud = PreviewHudSettings.defaults();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("onekeyminer.json");
    private static volatile LegacyFabricConfig current = new LegacyFabricConfig();

    public static LegacyFabricConfig get() {
        return current;
    }

    public static synchronized void load() {
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                LegacyFabricConfig loaded = GSON.fromJson(reader, LegacyFabricConfig.class);
                if (loaded != null) current = loaded;
            } catch (IOException | RuntimeException exception) {
                LegacyFabricMiner.LOGGER.error("Unable to read OneKeyMiner config", exception);
            }
        }
        current.maxBlocks = Math.max(1, Math.min(10240, current.maxBlocks));
        current.maxDistance = Math.max(1, Math.min(128, current.maxDistance));
        current.minHungerLevel = Math.max(0, Math.min(20, current.minHungerLevel));
        current.preserveDurability = Math.max(0, current.preserveDurability);
        if (current.blacklist == null) current.blacklist = new ArrayList<>();
        PreviewHudSettings settings = current.previewHud;
        current.previewHud = settings == null ? PreviewHudSettings.defaults() : new PreviewHudSettings(
                settings.enabled(), settings.x(), settings.y(), settings.scale(), settings.backgroundAlpha(),
                settings.showShape(), settings.showCount(), settings.showMax());
        save();
    }

    public static synchronized void setHud(PreviewHudSettings settings) {
        current.previewHud = settings;
        save();
    }

    public static synchronized void save() {
        Path temporary = null;
        try {
            Files.createDirectories(FILE.getParent());
            temporary = Files.createTempFile(FILE.getParent(), "onekeyminer-", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(current, writer);
            }
            try {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LegacyFabricMiner.LOGGER.error("Unable to save OneKeyMiner config", exception);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException exception) {
                    LegacyFabricMiner.LOGGER.debug("Unable to remove temporary config", exception);
                }
            }
        }
    }
}
