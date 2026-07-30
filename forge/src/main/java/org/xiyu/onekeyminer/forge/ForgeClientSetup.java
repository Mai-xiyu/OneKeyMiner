package org.xiyu.onekeyminer.forge;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.ConfigSyncHelper;

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
        ConfigSyncHelper.registerSyncCallback(ForgeClientSetup::sendCurrentState);
        ForgeKeyBindings.register();
    }

    public static void sendCurrentState() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            return;
        }
        var config = ConfigManager.getConfig();
        ForgeClientNetworking.sendClientState(
                ForgeKeyBindings.isChainKeyDown(),
                config.selectedShape,
                config.teleportDrops,
                config.teleportExp
        );
    }
}
