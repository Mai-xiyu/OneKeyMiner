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
 * Forge 平台模组入口点
 * 
 * <p>负责在 Forge 平台上初始化 OneKeyMiner 模组，
 * 包括注册事件监听器和配置界面。</p>
 * 
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.21.9
 */
@Mod(OneKeyMiner.MOD_ID)
public class OneKeyMinerForge {
    
    public OneKeyMinerForge(FMLJavaModLoadingContext context) {
        // 首先初始化平台服务（必须在 OneKeyMiner.init() 之前）
        PlatformServices.setInstance(new ForgePlatformServices());
        
        // 初始化通用模块
        OneKeyMiner.init();
        
        var modBusGroup = context.getModBusGroup();
        
        // 注册生命周期事件
        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::onCommonSetup);
        
        // 客户端专用事件
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgeClientBootstrap.register(context);
        }
        
        // 注册游戏事件处理器到 Forge 事件总线
        MinecraftForge.EVENT_BUS.register(new ForgeEventHandler());
        
        OneKeyMiner.LOGGER.info("OneKeyMiner Forge 模块初始化完成");
    }
    
    /**
     * 通用设置事件处理
     */
    private void onCommonSetup(FMLCommonSetupEvent event) {
        // 注册网络包
        ForgeNetworking.register();
        OneKeyMiner.LOGGER.debug("Forge 通用设置完成");
    }
    
}
