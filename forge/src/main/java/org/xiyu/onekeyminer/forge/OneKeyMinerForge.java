package org.xiyu.onekeyminer.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.platform.PlatformServices;

/** Forge 1.20.1 entrypoint. */
@Mod(OneKeyMiner.MOD_ID)
public final class OneKeyMinerForge {

    public OneKeyMinerForge(FMLJavaModLoadingContext context) {
        PlatformServices.setInstance(new ForgePlatformServices());
        OneKeyMiner.init();

        context.getModEventBus().addListener(this::onCommonSetup);

        // A plain runtime if is not enough: the JVM may resolve client method
        // references while loading this class on a dedicated server.
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ForgeClientSetup::register);

        MinecraftForge.EVENT_BUS.register(new ForgeEventHandler());
        OneKeyMiner.LOGGER.info("OneKeyMiner Forge module initialized");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        ForgeNetworking.register();
    }
}
