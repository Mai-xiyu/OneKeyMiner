package org.xiyu.onekeyminer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xiyu.onekeyminer.api.OneKeyMinerAPI;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.shape.ShapeRegistry;
import org.xiyu.onekeyminer.shape.builtin.*;

/**
 * Common OneKeyMiner entry point.
 */
public class OneKeyMiner {
    public static final String MOD_ID = "onekeyminer";
    public static final String MOD_NAME = "OneKeyMiner";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private static boolean initialized = false;

    public static void init() {
        if (initialized) {
            LOGGER.warn("OneKeyMiner is already initialized; skipping duplicate init");
            return;
        }

        LOGGER.info("Initializing {} v{}", MOD_NAME, VERSION);
        registerBuiltinShapes();
        ConfigManager.load();
        OneKeyMinerAPI.init();
        registerDefaultBlocks();

        initialized = true;
        LOGGER.info("{} initialized", MOD_NAME);
    }

    private static void registerBuiltinShapes() {
        ShapeRegistry.register(new AmorphousShape());
        ShapeRegistry.register(new CubeShape());
        ShapeRegistry.register(new ColumnShape());
        ShapeRegistry.register(new SmallTunnelShape());
        ShapeRegistry.register(new LargeTunnelShape());
        ShapeRegistry.register(new SmallSquareShape());
        ShapeRegistry.register(new MiningTunnelShape());
        ShapeRegistry.register(new EscapeTunnelShape());
        LOGGER.debug("Registered {} chain shapes", ShapeRegistry.getShapeCount());
    }

    private static void registerDefaultBlocks() {
        OneKeyMinerAPI.registerBlockTag("minecraft:coal_ores");
        OneKeyMinerAPI.registerBlockTag("minecraft:iron_ores");
        OneKeyMinerAPI.registerBlockTag("minecraft:gold_ores");
        OneKeyMinerAPI.registerBlockTag("minecraft:diamond_ores");
        OneKeyMinerAPI.registerBlockTag("minecraft:emerald_ores");
        OneKeyMinerAPI.registerBlockTag("minecraft:lapis_ores");
        OneKeyMinerAPI.registerBlockTag("minecraft:redstone_ores");
        OneKeyMinerAPI.registerBlockTag("minecraft:copper_ores");
        OneKeyMinerAPI.registerBlockTag("minecraft:logs");
        OneKeyMinerAPI.registerBlockTag("minecraft:leaves");
        OneKeyMinerAPI.registerBlock("minecraft:nether_gold_ore");
        OneKeyMinerAPI.registerBlock("minecraft:ancient_debris");
        OneKeyMinerAPI.registerBlock("minecraft:nether_quartz_ore");
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
