package org.xiyu.onekeyminer.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.platform.PlatformServices;

/** Forge 1.20.4 entrypoint. */
@Mod(OneKeyMiner.MOD_ID)
public final class OneKeyMinerForge {

    public OneKeyMinerForge(FMLJavaModLoadingContext context) {
        PlatformServices.setInstance(new ForgePlatformServices());
        OneKeyMiner.init();

        context.getModEventBus().addListener(this::onCommonSetup);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ForgeClientSetup::register);

        MinecraftForge.EVENT_BUS.register(new ForgeEventHandler());
        OneKeyMiner.LOGGER.info("OneKeyMiner Forge initialized");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        ForgeNetworking.register();
    }
}
