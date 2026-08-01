package org.xiyu.onekeyminer.forge;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.xiyu.onekeyminer.config.ConfigSyncHelper;
import org.xiyu.onekeyminer.network.ClientPreferenceAckDispatcher;

/** Physical-client-only Forge bootstrap. */
public final class ForgeClientSetup {

    private ForgeClientSetup() {
    }

    public static void register() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(ForgeClientSetup::onClientSetup);
        modEventBus.addListener(ForgeKeyBindings::registerKeyMappings);
        ForgeConfigScreen.register(ModLoadingContext.get());
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        ClientPreferenceAckDispatcher.register(
                ForgeClientNetworking::handlePreferencesAck
        );
        ConfigSyncHelper.registerSyncCallback(
                ForgeKeyBindings::sendCurrentPreferences
        );
        ForgeKeyBindings.register();
    }
}
