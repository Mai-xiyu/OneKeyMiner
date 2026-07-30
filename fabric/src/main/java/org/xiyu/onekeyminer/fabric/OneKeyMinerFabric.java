package org.xiyu.onekeyminer.fabric;

import net.fabricmc.api.ModInitializer;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.platform.PlatformServices;

/**
 * Fabric common entry point.
 */
public class OneKeyMinerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PlatformServices.setInstance(new FabricPlatformServices());
        OneKeyMiner.init();
        FabricEventHandler.register();
        FabricNetworking.register();
        OneKeyMiner.LOGGER.info("OneKeyMiner Fabric initialized");
    }
}
