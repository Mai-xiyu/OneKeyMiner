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
 * Forge 模组入口。
 *
 * <p>负责初始化平台服务、网络、游戏事件与客户端专用组件。</p>
 *
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.21.7
 */
@Mod(OneKeyMiner.MOD_ID)
public class OneKeyMinerForge {

    public OneKeyMinerForge(FMLJavaModLoadingContext context) {
        // 平台服务必须先于通用初始化注册。
        PlatformServices.setInstance(new ForgePlatformServices());

        // 初始化通用模块。
        OneKeyMiner.init();

        var modBusGroup = context.getModBusGroup();

        // 注册生命周期事件。
        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::onCommonSetup);

        // 客户端专用事件。
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgeClientBootstrap.register(context);
        }

        // 注册游戏事件处理器。
        MinecraftForge.EVENT_BUS.register(new ForgeEventHandler());

        OneKeyMiner.LOGGER.info("OneKeyMiner Forge initialized");
    }

    /**
     * 通用设置事件处理。
     */
    private void onCommonSetup(FMLCommonSetupEvent event) {
        // 注册网络通道。
        ForgeNetworking.register();
        OneKeyMiner.LOGGER.debug("Forge common setup complete");
    }

}
