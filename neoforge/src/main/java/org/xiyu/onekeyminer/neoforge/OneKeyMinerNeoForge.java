package org.xiyu.onekeyminer.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.platform.PlatformServices;

@Mod(OneKeyMiner.MOD_ID)
public class OneKeyMinerNeoForge {
    public OneKeyMinerNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        PlatformServices.setInstance(new NeoForgePlatformServices());
        OneKeyMiner.init();

        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(NeoForgeNetworking::registerPayloadHandlers);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            NeoForgeClientBootstrap.register(modEventBus, modContainer);
        }

        NeoForge.EVENT_BUS.register(NeoForgeEventHandler.class);
        OneKeyMiner.LOGGER.info("OneKeyMiner NeoForge initialized");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        OneKeyMiner.LOGGER.debug("NeoForge common setup complete");
    }

}
