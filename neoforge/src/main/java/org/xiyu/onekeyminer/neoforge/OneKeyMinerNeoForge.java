package org.xiyu.onekeyminer.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.DistExecutor;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.platform.PlatformServices;

/** NeoForge 20.4 entrypoint. */
@Mod(OneKeyMiner.MOD_ID)
public final class OneKeyMinerNeoForge {

    private static IEventBus modEventBus;
    private static ModContainer modContainer;

    public OneKeyMinerNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        OneKeyMinerNeoForge.modEventBus = modEventBus;
        OneKeyMinerNeoForge.modContainer = modContainer;

        PlatformServices.setInstance(new NeoForgePlatformServices());
        OneKeyMiner.init();

        modEventBus.addListener(NeoForgeNetworking::registerPayloadHandlers);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> NeoForgeClientSetup::register);

        NeoForge.EVENT_BUS.register(new NeoForgeEventHandler());
        OneKeyMiner.LOGGER.info("OneKeyMiner NeoForge initialized");
    }

    static IEventBus getModEventBus() {
        return modEventBus;
    }

    static ModContainer getModContainer() {
        return modContainer;
    }
}
