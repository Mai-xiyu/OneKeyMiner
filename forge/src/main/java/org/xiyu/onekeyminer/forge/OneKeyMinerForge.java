package org.xiyu.onekeyminer.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.platform.PlatformServices;

/**
 * Forge entry point. Client-only classes are isolated behind the physical-side
 * bootstrap so a dedicated server never loads Minecraft client types.
 */
@Mod(OneKeyMiner.MOD_ID)
public final class OneKeyMinerForge {
    public OneKeyMinerForge(FMLJavaModLoadingContext context) {
        PlatformServices.setInstance(new ForgePlatformServices());
        OneKeyMiner.init();

        var modBusGroup = context.getModBusGroup();
        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::onCommonSetup);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgeClientBootstrap.register(context);
        }

        MinecraftForge.EVENT_BUS.register(new ForgeEventHandler());
        OneKeyMiner.LOGGER.info("OneKeyMiner Forge initialized");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        ForgeNetworking.register();
        OneKeyMiner.LOGGER.debug("Forge common setup complete");
    }
}