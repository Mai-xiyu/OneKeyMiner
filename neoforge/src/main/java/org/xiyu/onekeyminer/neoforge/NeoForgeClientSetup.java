package org.xiyu.onekeyminer.neoforge;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.ConfigSyncHelper;

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
        ConfigSyncHelper.registerSyncCallback(NeoForgeClientSetup::sendCurrentState);
        NeoForgeKeyBindings.register();
    }

    public static void sendCurrentState() {
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        var config = ConfigManager.getConfig();
        NeoForgeClientNetworking.sendClientState(
                NeoForgeKeyBindings.isChainKeyDown(),
                config.selectedShape,
                config.teleportDrops,
                config.teleportExp
        );
    }
}
