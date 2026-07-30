package org.xiyu.onekeyminer.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigSyncHelper;

/**
 * Registers Forge client-only hooks without exposing client event types to the common entrypoint.
 */
@OnlyIn(Dist.CLIENT)
public final class ForgeClientBootstrap {
    private ForgeClientBootstrap() {
    }

    public static void register(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();
        FMLClientSetupEvent.getBus(modBusGroup).addListener(ForgeClientBootstrap::onClientSetup);
        RegisterKeyMappingsEvent.getBus(modBusGroup)
                .addListener(ForgeKeyBindings::registerKeyMappings);
        CustomizeGuiOverlayEvent.Chat.BUS.addListener(ForgeKeyBindings::renderPreviewHud);
        ForgeConfigScreen.register(context);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        ConfigSyncHelper.registerSyncCallback(ForgeKeyBindings::sendCurrentPreferences);
        ForgeKeyBindings.register();
        OneKeyMiner.LOGGER.debug("Forge client setup complete");
    }
}
