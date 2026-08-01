package org.xiyu.onekeyminer.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.xiyu.onekeyminer.config.ConfigSyncHelper;
import org.xiyu.onekeyminer.network.ClientPreferenceAckDispatcher;

/** Physical-client-only NeoForge bootstrap. */
public final class NeoForgeClientSetup {

    private NeoForgeClientSetup() {
    }

    public static void register() {
        IEventBus modEventBus = OneKeyMinerNeoForge.getModEventBus();
        ModContainer modContainer = OneKeyMinerNeoForge.getModContainer();
        modEventBus.addListener(NeoForgeClientSetup::onClientSetup);
        modEventBus.addListener(NeoForgeKeyBindings::registerKeyMappings);
        NeoForgeConfigScreen.register(modContainer);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        ClientPreferenceAckDispatcher.register(
                NeoForgeClientNetworking::handlePreferencesAck
        );
        ConfigSyncHelper.registerSyncCallback(
                NeoForgeKeyBindings::sendCurrentPreferences
        );
        NeoForgeKeyBindings.register();
    }
}
