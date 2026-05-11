package org.xiyu.onekeyminer.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.ConfigSyncHelper;
import org.xiyu.onekeyminer.platform.PlatformServices;

@Mod(OneKeyMiner.MOD_ID)
public class OneKeyMinerNeoForge {
    public OneKeyMinerNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        PlatformServices.setInstance(new NeoForgePlatformServices());
        OneKeyMiner.init();

        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(NeoForgeNetworking::registerPayloadHandlers);

        if (FMLLoader.getDist() == Dist.CLIENT) {
            modEventBus.addListener(this::onClientSetup);
            modEventBus.addListener(NeoForgeKeyBindings::registerKeyMappings);
            modEventBus.addListener(NeoForgeKeyBindings::registerGuiLayer);
            NeoForgeConfigScreen.register(modContainer);
        }

        NeoForge.EVENT_BUS.register(NeoForgeEventHandler.class);
        OneKeyMiner.LOGGER.info("OneKeyMiner NeoForge initialized");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        OneKeyMiner.LOGGER.debug("NeoForge common setup complete");
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        ConfigSyncHelper.registerSyncCallback(() -> {
            var config = ConfigManager.getConfig();
            NeoForgeNetworking.sendTeleportSettings(config.teleportDrops, config.teleportExp);
        });
        NeoForgeKeyBindings.register();
        OneKeyMiner.LOGGER.debug("NeoForge client setup complete");
    }
}
