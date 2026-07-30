package org.xiyu.onekeyminer.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigSyncHelper;

/**
 * Keeps every physical-client class reference out of the common mod entrypoint.
 */
public final class NeoForgeClientBootstrap {
    private NeoForgeClientBootstrap() {
    }

    public static void register(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(NeoForgeClientBootstrap::onClientSetup);
        modEventBus.addListener(NeoForgeKeyBindings::registerKeyMappings);
        modEventBus.addListener(NeoForgeKeyBindings::registerGuiLayer);
        NeoForge.EVENT_BUS.addListener(NeoForgeClientBootstrap::onLoggingIn);
        NeoForge.EVENT_BUS.addListener(NeoForgeClientBootstrap::onLoggingOut);
        NeoForgeConfigScreen.register(modContainer);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        ConfigSyncHelper.registerSyncCallback(NeoForgeKeyBindings::syncCurrentState);
        NeoForgeKeyBindings.register();
        OneKeyMiner.LOGGER.debug("NeoForge client setup complete");
    }

    private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        NeoForgeKeyBindings.syncCurrentState();
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        NeoForgeKeyBindings.resetConnectionState();
    }
}
