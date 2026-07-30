package org.xiyu.onekeyminer.forge;

import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigSyncHelper;

/**
 * Keeps every physical-client class reference out of the common mod entrypoint.
 */
public final class ForgeClientBootstrap {
    private ForgeClientBootstrap() {
    }

    public static void register(FMLJavaModLoadingContext context) {
        FMLClientSetupEvent.getBus(context.getModBusGroup())
                .addListener(ForgeClientBootstrap::onClientSetup);
        RegisterKeyMappingsEvent.BUS.addListener(
                ForgeKeyBindings.Events::onRegisterKeyMappings
        );
        TickEvent.ClientTickEvent.Post.BUS.addListener(
                ForgeKeyBindings.Events::onClientTick
        );
        CustomizeGuiOverlayEvent.Chat.BUS.addListener(ForgeKeyBindings::renderPreviewHud);
        ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(
                event -> ForgeKeyBindings.syncCurrentState()
        );
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(
                event -> ForgeKeyBindings.resetConnectionState()
        );
        ForgeConfigScreen.register(context);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        ConfigSyncHelper.registerSyncCallback(ForgeKeyBindings::syncCurrentState);
        ForgeKeyBindings.register();
        OneKeyMiner.LOGGER.debug("Forge client setup complete");
    }
}
