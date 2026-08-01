
package org.xiyu.onekeyminer.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.ConfigSyncHelper;
import org.xiyu.onekeyminer.network.ClientPreferenceAckDispatcher;
import org.xiyu.onekeyminer.platform.PlatformServices;

@Mod(OneKeyMiner.MOD_ID)
public class OneKeyMinerForge {
    public OneKeyMinerForge(FMLJavaModLoadingContext context) {
        PlatformServices.setInstance(new ForgePlatformServices());
        OneKeyMiner.init();

        var modEventBus = context.getModEventBus();
        modEventBus.addListener(this::onCommonSetup);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::onClientSetup);
            modEventBus.addListener(ForgeKeyBindings::registerKeyMappings);
            modEventBus.addListener(ForgeKeyBindings::registerGuiOverlay);
            ForgeConfigScreen.register(context);
        }

        MinecraftForge.EVENT_BUS.register(new ForgeEventHandler());
        OneKeyMiner.LOGGER.info("OneKeyMiner Forge initialized");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        ForgeNetworking.register();
        OneKeyMiner.LOGGER.debug("Forge common setup complete");
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        ConfigSyncHelper.registerSyncCallback(ForgeKeyBindings::sendCurrentPreferences);
        ClientPreferenceAckDispatcher.register(ForgeClientNetworking::handlePreferencesAck);
        ForgeKeyBindings.register();
        OneKeyMiner.LOGGER.debug("Forge client setup complete");
    }
}
