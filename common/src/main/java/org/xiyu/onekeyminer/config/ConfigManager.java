package org.xiyu.onekeyminer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

/** Publishes immutable-by-convention config snapshots and persists them atomically. */
public final class ConfigManager {
    private static final String CONFIG_FILE_NAME = "onekeyminer.json";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final AtomicReference<MinerConfig> CONFIG =
            new AtomicReference<>(normalized(new MinerConfig()));
    private static final List<ConfigChangeListener> LISTENERS =
            new CopyOnWriteArrayList<>();

    private ConfigManager() {
    }

    public static synchronized void load() {
        Path configPath = getConfigPath();
        if (!Files.exists(configPath)) {
            save();
            return;
        }
        try {
            MinerConfig loaded = GSON.fromJson(Files.readString(configPath), MinerConfig.class);
            if (loaded == null) {
                throw new IllegalArgumentException("configuration document is null");
            }
            CONFIG.set(normalized(loaded));
            OneKeyMiner.LOGGER.info("Loaded config from {}", configPath);
        } catch (IOException | RuntimeException exception) {
            OneKeyMiner.LOGGER.error(
                    "Could not load config {}; retaining safe defaults",
                    configPath,
                    exception
            );
        }
    }

    public static synchronized void save() {
        Path configPath = getConfigPath();
        Path temporary = configPath.resolveSibling(configPath.getFileName() + ".tmp");
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(temporary, GSON.toJson(CONFIG.get()));
            try {
                Files.move(
                        temporary,
                        configPath,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, configPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            OneKeyMiner.LOGGER.error("Could not save config {}", configPath, exception);
        }
    }

    /** Reloads valid fields without exposing a half-parsed or mutable instance. */
    public static synchronized void reload() {
        Path configPath = getConfigPath();
        final MinerConfig diskConfig;
        try {
            diskConfig = GSON.fromJson(Files.readString(configPath), MinerConfig.class);
            if (diskConfig == null) {
                throw new IllegalArgumentException("configuration document is null");
            }
        } catch (IOException | RuntimeException exception) {
            OneKeyMiner.LOGGER.error("Could not reload config {}; retaining current values", configPath, exception);
            return;
        }

        MinerConfig oldConfig = CONFIG.get().copy();
        MinerConfig merged = oldConfig.copy();
        mergeDiskConfig(merged, diskConfig);
        MinerConfig committed = normalized(merged);
        CONFIG.set(committed);
        notifyListeners(oldConfig, committed.copy());
        ConfigSyncHelper.triggerSync();
    }

    /** Returns an isolated snapshot; callers cannot mutate the published config. */
    public static MinerConfig getConfig() {
        return CONFIG.get().copy();
    }

    public record ClientPreferencesSnapshot(
            String selectedShape,
            boolean teleportDrops,
            boolean teleportExp
    ) {
    }

    public static ClientPreferencesSnapshot getClientPreferencesSnapshot() {
        MinerConfig config = CONFIG.get();
        return new ClientPreferencesSnapshot(
                config.selectedShape,
                config.teleportDrops,
                config.teleportExp
        );
    }

    public record ServerPreferenceSnapshot(
            boolean enabled,
            int maxBlocks,
            int maxBlocksCreative,
            int maxDistance,
            boolean allowDiagonal,
            boolean allowClientTeleportDrops,
            boolean allowClientTeleportExp
    ) {
        public int maxBlocksFor(boolean creative) {
            return creative ? maxBlocksCreative : maxBlocks;
        }
    }

    public static ServerPreferenceSnapshot getServerPreferenceSnapshot() {
        MinerConfig config = CONFIG.get();
        return new ServerPreferenceSnapshot(
                config.enabled,
                config.maxBlocks,
                config.maxBlocksCreative,
                config.maxDistance,
                config.allowDiagonal,
                config.allowClientTeleportDrops,
                config.allowClientTeleportExp
        );
    }

    public static synchronized void updateConfig(MinerConfig newConfig) {
        updateConfig(newConfig, "all");
    }

    public static synchronized void updateConfig(MinerConfig newConfig, String changedKey) {
        Objects.requireNonNull(newConfig, "newConfig");
        Objects.requireNonNull(changedKey, "changedKey");
        MinerConfig oldConfig = CONFIG.get().copy();
        MinerConfig committed = normalized(newConfig);
        CONFIG.set(committed);
        save();
        notifyListeners(oldConfig, committed.copy());
        ConfigSyncHelper.notifyConfigChanged(changedKey);
    }

    /** Atomic copy-edit-publish helper for public API setters. */
    public static synchronized void editConfig(String changedKey, Consumer<MinerConfig> editor) {
        Objects.requireNonNull(changedKey, "changedKey");
        Objects.requireNonNull(editor, "editor");
        MinerConfig edited = CONFIG.get().copy();
        editor.accept(edited);
        updateConfig(edited, changedKey);
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

    private static MinerConfig normalized(MinerConfig source) {
        Objects.requireNonNull(source, "source");
        MinerConfig result = source.copy();
        normalizeCollections(result);
        result.maxBlocks = Math.max(1, Math.min(10_240, result.maxBlocks));
        result.maxBlocksCreative = Math.max(1, Math.min(10_240, result.maxBlocksCreative));
        result.maxDistance = Math.max(1, Math.min(128, result.maxDistance));
        result.preserveDurability = Math.max(0, result.preserveDurability);
        if (!Float.isFinite(result.hungerMultiplier)) {
            result.hungerMultiplier = 1.0f;
        }
        result.hungerMultiplier = Math.max(
                0.0f,
                Math.min(10.0f, result.hungerMultiplier)
        );
        result.minHungerLevel = Math.max(0, Math.min(20, result.minHungerLevel));
        if (!Float.isFinite(result.hungerPerBlock)) {
            result.hungerPerBlock = 0.025f;
        }
        result.hungerPerBlock = Math.max(0.0f, result.hungerPerBlock);
        if (!ShapeRegistry.isValidShapeId(result.selectedShape)) {
            result.selectedShape = ShapeRegistry.DEFAULT_SHAPE_ID.toString();
        }
        normalizeCollections(result);
        return result;
    }

    private static void normalizeCollections(MinerConfig config) {
        if (config.interactionToolWhitelist == null) config.interactionToolWhitelist = new ArrayList<>();
        if (config.interactionToolBlacklist == null) config.interactionToolBlacklist = new ArrayList<>();
        if (config.seedWhitelist == null) config.seedWhitelist = new ArrayList<>();
        if (config.seedBlacklist == null) config.seedBlacklist = new ArrayList<>();
        if (config.farmlandWhitelist == null) config.farmlandWhitelist = new ArrayList<>();
        if (config.interactiveItemWhitelist == null) config.interactiveItemWhitelist = new ArrayList<>();
        if (config.interactiveItemBlacklist == null) config.interactiveItemBlacklist = new ArrayList<>();
        if (config.customWhitelist == null) config.customWhitelist = new ArrayList<>();
        if (config.blacklist == null) config.blacklist = new ArrayList<>();
        if (config.toolWhitelist == null) config.toolWhitelist = new ArrayList<>();
        if (config.toolBlacklist == null) config.toolBlacklist = new ArrayList<>();
    }

    private static void mergeDiskConfig(MinerConfig target, MinerConfig disk) {
        target.enabled = disk.enabled;
        if (ShapeRegistry.isValidShapeId(disk.selectedShape)) target.selectedShape = disk.selectedShape;
        if (disk.maxBlocks >= 1 && disk.maxBlocks <= 10_240) target.maxBlocks = disk.maxBlocks;
        if (disk.maxBlocksCreative >= 1 && disk.maxBlocksCreative <= 10_240) target.maxBlocksCreative = disk.maxBlocksCreative;
        if (disk.maxDistance >= 1 && disk.maxDistance <= 128) target.maxDistance = disk.maxDistance;
        target.allowDiagonal = disk.allowDiagonal;
        target.consumeDurability = disk.consumeDurability;
        target.stopOnLowDurability = disk.stopOnLowDurability;
        if (disk.preserveDurability >= 0) target.preserveDurability = disk.preserveDurability;
        target.consumeHunger = disk.consumeHunger;
        if (Float.isFinite(disk.hungerMultiplier)
                && disk.hungerMultiplier >= 0
                && disk.hungerMultiplier <= 10) {
            target.hungerMultiplier = disk.hungerMultiplier;
        }
        if (disk.minHungerLevel >= 0 && disk.minHungerLevel <= 20) target.minHungerLevel = disk.minHungerLevel;
        if (Float.isFinite(disk.hungerPerBlock) && disk.hungerPerBlock >= 0) {
            target.hungerPerBlock = disk.hungerPerBlock;
        }
        target.enableInteraction = disk.enableInteraction;
        target.enablePlanting = disk.enablePlanting;
        target.enableHarvesting = disk.enableHarvesting;
        target.harvestReplant = disk.harvestReplant;
        target.strictBlockMatching = disk.strictBlockMatching;
        target.mineAllBlocks = disk.mineAllBlocks;
        target.allowBareHand = disk.allowBareHand;
        target.teleportDrops = disk.teleportDrops;
        target.teleportExp = disk.teleportExp;
        target.allowClientTeleportDrops = disk.allowClientTeleportDrops;
        target.allowClientTeleportExp = disk.allowClientTeleportExp;
        target.requireExactMatch = disk.requireExactMatch;
        target.playSound = disk.playSound;
        target.showStats = disk.showStats;
        copyCollections(target, disk);
    }

    private static void copyCollections(MinerConfig target, MinerConfig source) {
        if (source.interactionToolWhitelist != null) target.interactionToolWhitelist = new ArrayList<>(source.interactionToolWhitelist);
        if (source.interactionToolBlacklist != null) target.interactionToolBlacklist = new ArrayList<>(source.interactionToolBlacklist);
        if (source.seedWhitelist != null) target.seedWhitelist = new ArrayList<>(source.seedWhitelist);
        if (source.seedBlacklist != null) target.seedBlacklist = new ArrayList<>(source.seedBlacklist);
        if (source.farmlandWhitelist != null) target.farmlandWhitelist = new ArrayList<>(source.farmlandWhitelist);
        if (source.interactiveItemWhitelist != null) target.interactiveItemWhitelist = new ArrayList<>(source.interactiveItemWhitelist);
        if (source.interactiveItemBlacklist != null) target.interactiveItemBlacklist = new ArrayList<>(source.interactiveItemBlacklist);
        if (source.customWhitelist != null) target.customWhitelist = new ArrayList<>(source.customWhitelist);
        if (source.blacklist != null) target.blacklist = new ArrayList<>(source.blacklist);
        if (source.toolWhitelist != null) target.toolWhitelist = new ArrayList<>(source.toolWhitelist);
        if (source.toolBlacklist != null) target.toolBlacklist = new ArrayList<>(source.toolBlacklist);
    }

    private static void notifyListeners(MinerConfig oldConfig, MinerConfig newConfig) {
        for (ConfigChangeListener listener : LISTENERS) {
            try {
                listener.onConfigChanged(oldConfig.copy(), newConfig.copy());
            } catch (RuntimeException exception) {
                OneKeyMiner.LOGGER.error("Config change listener failed", exception);
            }
        }
    }

    @FunctionalInterface
    public interface ConfigChangeListener {
        void onConfigChanged(MinerConfig oldConfig, MinerConfig newConfig);
    }
}
