package org.xiyu.onekeyminer.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigSyncHelper;

/**
 * Registers NeoForge client-only hooks without exposing client event types to the common entrypoint.
 */
@Mod(value = OneKeyMiner.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeClientBootstrap {
    public NeoForgeClientBootstrap(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(NeoForgeClientBootstrap::onClientSetup);
        modEventBus.addListener(NeoForgeKeyBindings::registerKeyMappings);
        modEventBus.addListener(NeoForgeKeyBindings::registerGuiLayer);
        modEventBus.addListener(NeoForgeClientNetworking::registerPayloadHandlers);
        NeoForgeConfigScreen.register(modContainer);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        ConfigSyncHelper.registerSyncCallback(NeoForgeKeyBindings::sendCurrentPreferences);
        NeoForgeKeyBindings.register();
        OneKeyMiner.LOGGER.debug("NeoForge client setup complete");
    }
}
