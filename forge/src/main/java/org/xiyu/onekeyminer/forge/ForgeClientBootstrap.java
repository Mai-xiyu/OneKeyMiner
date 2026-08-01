package org.xiyu.onekeyminer.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigSyncHelper;
import org.xiyu.onekeyminer.network.ClientPreferenceAckDispatcher;
import org.xiyu.onekeyminer.preview.ChainPreviewHud;

/**
 * Registers Forge client-only hooks without exposing client event types to the common entrypoint.
 */
@OnlyIn(Dist.CLIENT)
public final class ForgeClientBootstrap {
    private static final Identifier PREVIEW_HUD =
            Identifier.fromNamespaceAndPath(
                    OneKeyMiner.MOD_ID,
                    "chain_preview"
            );

    private ForgeClientBootstrap() {
    }

    public static void register(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();
        FMLClientSetupEvent.getBus(modBusGroup).addListener(ForgeClientBootstrap::onClientSetup);
        AddGuiOverlayLayersEvent.BUS.addListener(ForgeClientBootstrap::registerPreviewHud);
        ForgeConfigScreen.register(context);
    }

    private static void registerPreviewHud(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().addAbove(
                ForgeLayeredDraw.POST_SLEEP_STACK,
                ForgeLayeredDraw.CHAT_OVERLAY,
                PREVIEW_HUD,
                (guiGraphics, deltaTracker) ->
                        ChainPreviewHud.render(guiGraphics)
        );
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        ConfigSyncHelper.registerSyncCallback(ForgeKeyBindings::sendCurrentPreferences);
        ClientPreferenceAckDispatcher.register(ForgeClientNetworking::handlePreferencesAck);
        ForgeKeyBindings.register();
        OneKeyMiner.LOGGER.debug("Forge client setup complete");
    }
}
