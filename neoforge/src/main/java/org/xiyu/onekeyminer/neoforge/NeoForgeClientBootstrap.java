package org.xiyu.onekeyminer.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigSyncHelper;
import org.xiyu.onekeyminer.network.ClientPreferenceAckDispatcher;

/**
 * Registers NeoForge client-only hooks without exposing client event types to the common entrypoint.
 */
@OnlyIn(Dist.CLIENT)
public final class NeoForgeClientBootstrap {
    private NeoForgeClientBootstrap() {
    }

    public static void register(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(NeoForgeClientBootstrap::onClientSetup);
        modEventBus.addListener(NeoForgeKeyBindings::registerKeyMappings);
        modEventBus.addListener(NeoForgeKeyBindings::registerGuiLayer);
        NeoForgeConfigScreen.register(modContainer);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        ConfigSyncHelper.registerSyncCallback(NeoForgeKeyBindings::sendCurrentPreferences);
        ClientPreferenceAckDispatcher.register(NeoForgeKeyBindings::handlePreferencesAck);
        NeoForgeKeyBindings.register();
        OneKeyMiner.LOGGER.debug("NeoForge client setup complete");
    }
}
