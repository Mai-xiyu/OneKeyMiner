package org.xiyu.onekeyminer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.platform.PlatformServices;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Loads, validates, saves and hot-reloads the JSON config.
 */
public class ConfigManager {
    private static final String CONFIG_FILE_NAME = "onekeyminer.json";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final AtomicReference<MinerConfig> CONFIG = new AtomicReference<>(new MinerConfig());
    private static final List<ConfigChangeListener> LISTENERS = new CopyOnWriteArrayList<>();

    public static synchronized void load() {
        Path configPath = getConfigPath();

        try {
            if (Files.exists(configPath)) {
                String json = Files.readString(configPath);
                MinerConfig loaded = GSON.fromJson(json, MinerConfig.class);
                if (loaded != null) {
                    normalizeCollections(loaded);
                    CONFIG.set(loaded);
                    OneKeyMiner.LOGGER.info("Config loaded: {}", configPath);
                }
            } else {
                save();
                OneKeyMiner.LOGGER.info("Created default config: {}", configPath);
            }
        } catch (Exception e) {
            OneKeyMiner.LOGGER.error("Failed to load config; keeping safe defaults: {}", e.getMessage());
        }

        if (validateConfig()) {
            save();
        }
    }

    public static synchronized void save() {
        Path configPath = getConfigPath();

        try {
            Path configDirectory = configPath.getParent();
            Files.createDirectories(configDirectory);
            Path temporaryFile = Files.createTempFile(configDirectory, "onekeyminer-", ".tmp");
            try {
                Files.writeString(temporaryFile, GSON.toJson(CONFIG.get()));
                try {
                    Files.move(
                            temporaryFile,
                            configPath,
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING
                    );
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temporaryFile, configPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporaryFile);
            }
            OneKeyMiner.LOGGER.debug("Config saved: {}", configPath);
        } catch (IOException e) {
            OneKeyMiner.LOGGER.error("Failed to save config: {}", e.getMessage());
        }
    }

    /**
     * Reloads the config with per-field validation. Invalid fields keep their old value.
     */
    public static synchronized void reload() {
        MinerConfig oldConfig = CONFIG.get().copy();
        Path configPath = getConfigPath();
        MinerConfig diskConfig;

        try {
            if (!Files.exists(configPath)) {
                save();
                return;
            }
            diskConfig = GSON.fromJson(Files.readString(configPath), MinerConfig.class);
        } catch (Exception e) {
            OneKeyMiner.LOGGER.error("Failed to reload config: {}", e.getMessage());
            return;
        }

        if (diskConfig == null) {
            OneKeyMiner.LOGGER.warn("Reloaded config was null; keeping current config");
            return;
        }
        normalizeCollections(diskConfig);

        MinerConfig merged = oldConfig.copy();
        List<String> rejected = new ArrayList<>();

        merged.enabled = diskConfig.enabled;

        String migratedShape = migrateLegacyShape(diskConfig);
        if (isValidShapeSyntax(migratedShape)) {
            merged.selectedShape = migratedShape;
        } else if (isValidShapeSyntax(diskConfig.selectedShape)) {
            merged.selectedShape = diskConfig.selectedShape;
        } else if (diskConfig.selectedShape != null) {
            rejected.add("selectedShape=" + diskConfig.selectedShape);
        }
        merged.shapeMode = null;

        if (diskConfig.maxBlocks >= 1 && diskConfig.maxBlocks <= 10240) {
            merged.maxBlocks = diskConfig.maxBlocks;
        } else {
            rejected.add("maxBlocks=" + diskConfig.maxBlocks);
        }
        if (diskConfig.maxDistance >= 1 && diskConfig.maxDistance <= 128) {
            merged.maxDistance = diskConfig.maxDistance;
        } else {
            rejected.add("maxDistance=" + diskConfig.maxDistance);
        }

        merged.allowDiagonal = diskConfig.allowDiagonal;
        merged.consumeDurability = diskConfig.consumeDurability;
        merged.stopOnLowDurability = diskConfig.stopOnLowDurability;
        merged.consumeHunger = diskConfig.consumeHunger;
        merged.enableInteraction = diskConfig.enableInteraction;
        merged.enablePlanting = diskConfig.enablePlanting;
        merged.enableHarvesting = diskConfig.enableHarvesting;
        merged.harvestReplant = diskConfig.harvestReplant;
        merged.strictBlockMatching = diskConfig.strictBlockMatching;
        merged.mineAllBlocks = diskConfig.mineAllBlocks;
        merged.allowBareHand = diskConfig.allowBareHand;
        merged.teleportDrops = diskConfig.teleportDrops;
        merged.teleportExp = diskConfig.teleportExp;
        merged.requireExactMatch = diskConfig.requireExactMatch;
        merged.playSound = diskConfig.playSound;
        merged.showStats = diskConfig.showStats;

        if (diskConfig.preserveDurability >= 0) {
            merged.preserveDurability = diskConfig.preserveDurability;
        } else {
            rejected.add("preserveDurability=" + diskConfig.preserveDurability);
        }
        if (Float.isFinite(diskConfig.hungerMultiplier)
                && diskConfig.hungerMultiplier >= 0
                && diskConfig.hungerMultiplier <= 10) {
            merged.hungerMultiplier = diskConfig.hungerMultiplier;
        } else {
            rejected.add("hungerMultiplier=" + diskConfig.hungerMultiplier);
        }
        if (diskConfig.minHungerLevel >= 0 && diskConfig.minHungerLevel <= 20) {
            merged.minHungerLevel = diskConfig.minHungerLevel;
        } else {
            rejected.add("minHungerLevel=" + diskConfig.minHungerLevel);
        }
        if (Float.isFinite(diskConfig.hungerPerBlock) && diskConfig.hungerPerBlock >= 0) {
            merged.hungerPerBlock = diskConfig.hungerPerBlock;
        } else {
            rejected.add("hungerPerBlock=" + diskConfig.hungerPerBlock);
        }
        if (diskConfig.maxBlocksCreative >= 1 && diskConfig.maxBlocksCreative <= 10240) {
            merged.maxBlocksCreative = diskConfig.maxBlocksCreative;
        } else {
            rejected.add("maxBlocksCreative=" + diskConfig.maxBlocksCreative);
        }

        copyLists(merged, diskConfig);

        CONFIG.set(merged);
        if (validateConfig()) {
            save();
        }

        if (!rejected.isEmpty()) {
            OneKeyMiner.LOGGER.warn("Rejected invalid config fields on reload: {}", String.join(", ", rejected));
        }
        MinerConfig newConfig = CONFIG.get().copy();
        notifyListeners(oldConfig, newConfig);
    }

    public static MinerConfig getConfig() {
        return CONFIG.get().copy();
    }

    public static MinerConfig getConfigSnapshot() {
        return getConfig();
    }

    public static void updateConfig(MinerConfig newConfig) {
        updateConfig(newConfig, "*");
    }

    public static synchronized void updateConfig(MinerConfig newConfig, String changedKey) {
        Objects.requireNonNull(newConfig, "newConfig");
        Objects.requireNonNull(changedKey, "changedKey");
        MinerConfig oldConfig = CONFIG.get().copy();
        MinerConfig normalized = newConfig.copy();
        normalizeCollections(normalized);
        CONFIG.set(normalized);
        validateConfig();
        save();
        notifyListeners(oldConfig, CONFIG.get().copy());
        ConfigSyncHelper.triggerSync();
        ConfigSyncHelper.notifyConfigChanged(changedKey);
    }

    /**
     * Atomically edits the latest configuration snapshot. Prefer this method
     * for single-setting API changes so concurrent callers cannot overwrite
     * unrelated fields using stale copies returned by {@link #getConfig()}.
     */
    public static synchronized void editConfig(
            String changedKey,
            Consumer<MinerConfig> editor
    ) {
        Objects.requireNonNull(changedKey, "changedKey");
        Objects.requireNonNull(editor, "editor");

        MinerConfig oldConfig = CONFIG.get().copy();
        MinerConfig edited = oldConfig.copy();
        editor.accept(edited);
        normalizeCollections(edited);
        CONFIG.set(edited);
        validateConfig();
        save();

        MinerConfig committed = CONFIG.get().copy();
        notifyListeners(oldConfig, committed);
        ConfigSyncHelper.triggerSync();
        ConfigSyncHelper.notifyConfigChanged(changedKey);
    }

    public static void addListener(ConfigChangeListener listener) {
        LISTENERS.add(Objects.requireNonNull(listener, "listener"));
    }

    public static void removeListener(ConfigChangeListener listener) {
        LISTENERS.remove(listener);
    }

    private static Path getConfigPath() {
        return PlatformServices.getInstance().getConfigDirectory().resolve(CONFIG_FILE_NAME);
    }

    private static boolean validateConfig() {
        MinerConfig config = CONFIG.get().copy();
        normalizeCollections(config);
        boolean changed = false;

        String migrated = migrateLegacyShape(config);
        if (migrated != null) {
            config.selectedShape = migrated;
            config.shapeMode = null;
            changed = true;
        }

        if (!isValidShapeSyntax(config.selectedShape)) {
            config.selectedShape = ShapeRegistry.DEFAULT_SHAPE_ID.toString();
            changed = true;
        }
        if (config.maxBlocks < 1) {
            config.maxBlocks = 1;
            changed = true;
        } else if (config.maxBlocks > 10240) {
            config.maxBlocks = 10240;
            changed = true;
        }
        if (config.maxDistance < 1) {
            config.maxDistance = 1;
            changed = true;
        } else if (config.maxDistance > 128) {
            config.maxDistance = 128;
            changed = true;
        }
        if (config.maxBlocksCreative < 1) {
            config.maxBlocksCreative = 1;
            changed = true;
        } else if (config.maxBlocksCreative > 10240) {
            config.maxBlocksCreative = 10240;
            changed = true;
        }
        if (!Float.isFinite(config.hungerMultiplier) || config.hungerMultiplier < 0) {
            config.hungerMultiplier = 0;
            changed = true;
        } else if (config.hungerMultiplier > 10) {
            config.hungerMultiplier = 10;
            changed = true;
        }
        if (config.preserveDurability < 0) {
            config.preserveDurability = 0;
            changed = true;
        } else if (config.preserveDurability > 100_000) {
            config.preserveDurability = 100_000;
            changed = true;
        }
        if (config.minHungerLevel < 0) {
            config.minHungerLevel = 0;
            changed = true;
        } else if (config.minHungerLevel > 20) {
            config.minHungerLevel = 20;
            changed = true;
        }
        if (!Float.isFinite(config.hungerPerBlock) || config.hungerPerBlock < 0) {
            config.hungerPerBlock = 0;
            changed = true;
        } else if (config.hungerPerBlock > 10) {
            config.hungerPerBlock = 10;
            changed = true;
        }
        if (changed) {
            CONFIG.set(config);
        }
        return changed;
    }

    private static String migrateLegacyShape(MinerConfig config) {
        if (config.shapeMode == null) {
            return null;
        }
        return switch (config.shapeMode) {
            case CONNECTED -> "onekeyminer:amorphous";
            case CUBE -> "onekeyminer:cube";
            case COLUMN -> "onekeyminer:column";
        };
    }

    private static void copyLists(MinerConfig merged, MinerConfig diskConfig) {
        if (diskConfig.customWhitelist != null) merged.customWhitelist = new ArrayList<>(diskConfig.customWhitelist);
        if (diskConfig.blacklist != null) merged.blacklist = new ArrayList<>(diskConfig.blacklist);
        if (diskConfig.toolWhitelist != null) merged.toolWhitelist = new ArrayList<>(diskConfig.toolWhitelist);
        if (diskConfig.toolBlacklist != null) merged.toolBlacklist = new ArrayList<>(diskConfig.toolBlacklist);
        if (diskConfig.interactionToolWhitelist != null) merged.interactionToolWhitelist = new ArrayList<>(diskConfig.interactionToolWhitelist);
        if (diskConfig.interactionToolBlacklist != null) merged.interactionToolBlacklist = new ArrayList<>(diskConfig.interactionToolBlacklist);
        if (diskConfig.interactiveItemWhitelist != null) merged.interactiveItemWhitelist = new ArrayList<>(diskConfig.interactiveItemWhitelist);
        if (diskConfig.interactiveItemBlacklist != null) merged.interactiveItemBlacklist = new ArrayList<>(diskConfig.interactiveItemBlacklist);
        if (diskConfig.seedWhitelist != null) merged.seedWhitelist = new ArrayList<>(diskConfig.seedWhitelist);
        if (diskConfig.seedBlacklist != null) merged.seedBlacklist = new ArrayList<>(diskConfig.seedBlacklist);
        if (diskConfig.farmlandWhitelist != null) merged.farmlandWhitelist = new ArrayList<>(diskConfig.farmlandWhitelist);
    }

    private static boolean isValidShapeSyntax(String shapeId) {
        return shapeId != null
                && !shapeId.isBlank()
                && shapeId.length() <= ShapeRegistry.MAX_SHAPE_ID_LENGTH
                && ResourceLocation.tryParse(shapeId) != null;
    }

    private static void normalizeCollections(MinerConfig config) {
        config.customWhitelist = sanitizeStrings(config.customWhitelist);
        config.blacklist = sanitizeStrings(config.blacklist);
        config.toolWhitelist = sanitizeStrings(config.toolWhitelist);
        config.toolBlacklist = sanitizeStrings(config.toolBlacklist);
        config.interactionToolWhitelist = sanitizeStrings(config.interactionToolWhitelist);
        config.interactionToolBlacklist = sanitizeStrings(config.interactionToolBlacklist);
        config.interactiveItemWhitelist = sanitizeStrings(config.interactiveItemWhitelist);
        config.interactiveItemBlacklist = sanitizeStrings(config.interactiveItemBlacklist);
        config.seedWhitelist = sanitizeStrings(config.seedWhitelist);
        config.seedBlacklist = sanitizeStrings(config.seedBlacklist);
        config.farmlandWhitelist = sanitizeStrings(config.farmlandWhitelist);
    }

    private static List<String> sanitizeStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private static void notifyListeners(MinerConfig oldConfig, MinerConfig newConfig) {
        for (ConfigChangeListener listener : LISTENERS) {
            try {
                listener.onConfigChanged(oldConfig.copy(), newConfig.copy());
            } catch (RuntimeException e) {
                OneKeyMiner.LOGGER.error("Config change listener failed", e);
            }
        }
    }

    @FunctionalInterface
    public interface ConfigChangeListener {
        void onConfigChanged(MinerConfig oldConfig, MinerConfig newConfig);
    }
}
