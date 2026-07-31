package org.xiyu.onekeyminer.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializable mod configuration.
 */
public class MinerConfig {
    public boolean enabled = true;

    public int maxBlocks = 64;
    public int maxDistance = 16;
    public boolean allowDiagonal = true;

    /** Selected chain shape ID. */
    public String selectedShape = "onekeyminer:amorphous";

    /**
     * Legacy field kept only so old configs can migrate to selectedShape.
     * Gson skips null values by default, so validation clears it after migration.
     */
    @Deprecated
    public ShapeMode shapeMode = null;

    public boolean consumeDurability = true;
    public boolean stopOnLowDurability = true;
    public int preserveDurability = 1;

    public boolean consumeHunger = true;
    public float hungerMultiplier = 1.0f;
    public int minHungerLevel = 1;
    public float hungerPerBlock = 0.025f;

    public boolean enableInteraction = true;
    public List<String> interactionToolWhitelist = new ArrayList<>();
    public List<String> interactionToolBlacklist = new ArrayList<>();
    public List<String> interactiveItemWhitelist = new ArrayList<>();
    public List<String> interactiveItemBlacklist = new ArrayList<>();

    public boolean enablePlanting = true;
    public List<String> seedWhitelist = new ArrayList<>();
    public List<String> seedBlacklist = new ArrayList<>();
    public List<String> farmlandWhitelist = new ArrayList<>();

    public boolean enableHarvesting = true;
    public boolean harvestReplant = true;

    public int maxBlocksCreative = 256;
    public boolean strictBlockMatching = false;
    public boolean mineAllBlocks = true;
    public boolean allowBareHand = true;
    public boolean teleportDrops = false;
    public boolean teleportExp = false;
    /** Dedicated-server policy gates for client-requested teleport features. */
    public boolean allowClientTeleportDrops = true;
    public boolean allowClientTeleportExp = true;
    public boolean requireExactMatch = false;
    public boolean playSound = true;
    public boolean showStats = true;

    public List<String> customWhitelist = new ArrayList<>();
    public List<String> blacklist = new ArrayList<>();
    public List<String> toolWhitelist = new ArrayList<>();
    public List<String> toolBlacklist = new ArrayList<>();

    @Deprecated
    public enum ShapeMode {
        CONNECTED("connected"),
        CUBE("cube"),
        COLUMN("column");

        private final String id;

        ShapeMode(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public MinerConfig copy() {
        MinerConfig copy = new MinerConfig();
        copy.enabled = this.enabled;
        copy.maxBlocks = this.maxBlocks;
        copy.maxDistance = this.maxDistance;
        copy.allowDiagonal = this.allowDiagonal;
        copy.selectedShape = this.selectedShape;
        copy.shapeMode = this.shapeMode;
        copy.consumeDurability = this.consumeDurability;
        copy.stopOnLowDurability = this.stopOnLowDurability;
        copy.preserveDurability = this.preserveDurability;
        copy.consumeHunger = this.consumeHunger;
        copy.hungerMultiplier = this.hungerMultiplier;
        copy.minHungerLevel = this.minHungerLevel;
        copy.hungerPerBlock = this.hungerPerBlock;
        copy.enableInteraction = this.enableInteraction;
        copy.interactionToolWhitelist = copyList(this.interactionToolWhitelist);
        copy.interactionToolBlacklist = copyList(this.interactionToolBlacklist);
        copy.interactiveItemWhitelist = copyList(this.interactiveItemWhitelist);
        copy.interactiveItemBlacklist = copyList(this.interactiveItemBlacklist);
        copy.enablePlanting = this.enablePlanting;
        copy.seedWhitelist = copyList(this.seedWhitelist);
        copy.seedBlacklist = copyList(this.seedBlacklist);
        copy.farmlandWhitelist = copyList(this.farmlandWhitelist);
        copy.enableHarvesting = this.enableHarvesting;
        copy.harvestReplant = this.harvestReplant;
        copy.maxBlocksCreative = this.maxBlocksCreative;
        copy.strictBlockMatching = this.strictBlockMatching;
        copy.mineAllBlocks = this.mineAllBlocks;
        copy.allowBareHand = this.allowBareHand;
        copy.teleportDrops = this.teleportDrops;
        copy.teleportExp = this.teleportExp;
        copy.allowClientTeleportDrops = this.allowClientTeleportDrops;
        copy.allowClientTeleportExp = this.allowClientTeleportExp;
        copy.requireExactMatch = this.requireExactMatch;
        copy.playSound = this.playSound;
        copy.showStats = this.showStats;
        copy.customWhitelist = copyList(this.customWhitelist);
        copy.blacklist = copyList(this.blacklist);
        copy.toolWhitelist = copyList(this.toolWhitelist);
        copy.toolBlacklist = copyList(this.toolBlacklist);
        return copy;
    }

    public boolean isDropTeleportEnabled(boolean clientRequested) {
        return allowClientTeleportDrops && clientRequested;
    }

    public boolean isExperienceTeleportEnabled(boolean clientRequested) {
        return allowClientTeleportExp && clientRequested;
    }

    private static List<String> copyList(List<String> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }
}
