package org.xiyu.onekeyminer.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.ConfigSyncHelper;
import org.xiyu.onekeyminer.platform.PlatformServices;

/**
 * NeoForge 平台模组入口点
 * 
 * <p>负责在 NeoForge 平台上初始化 OneKeyMiner 模组，
 * 包括注册事件监听器和配置界面。</p>
 * 
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.21.9
 */
@Mod(OneKeyMiner.MOD_ID)
public class OneKeyMinerNeoForge {
    
    public OneKeyMinerNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        // 首先初始化平台服务（必须在 OneKeyMiner.init() 之前）
        PlatformServices.setInstance(new NeoForgePlatformServices());
        
        // 初始化通用模块
        OneKeyMiner.init();
        
        // 注册模组生命周期事件
        modEventBus.addListener(this::onCommonSetup);
        
        // 注册网络包处理器（MOD 事件总线）
        modEventBus.addListener(NeoForgeNetworking::registerPayloadHandlers);
        
        // 客户端专用事件
        if (FMLLoader.getDist() == Dist.CLIENT) {
            modEventBus.addListener(this::onClientSetup);
            modEventBus.addListener(NeoForgeKeyBindings::registerKeyMappings);
        }
        
        // 注册游戏事件处理器到 NeoForge 事件总线
        NeoForge.EVENT_BUS.register(NeoForgeEventHandler.class);
        
        // 注册配置界面（仅客户端）
        if (FMLLoader.getDist() == Dist.CLIENT) {
            NeoForgeConfigScreen.register(modContainer);
        }
        
        OneKeyMiner.LOGGER.info("OneKeyMiner NeoForge 模块初始化完成");
    }
    
    /**
     * 通用设置事件处理
     */
    private void onCommonSetup(FMLCommonSetupEvent event) {
        OneKeyMiner.LOGGER.debug("NeoForge 通用设置完成");
    }
    
    /**
     * 客户端设置事件处理
     */
    private void onClientSetup(FMLClientSetupEvent event) {
        // 注册配置同步回调
        ConfigSyncHelper.registerSyncCallback(() -> {
            // 配置变更后的回调
        });
        // 注册按键绑定
        NeoForgeKeyBindings.register();
        OneKeyMiner.LOGGER.debug("NeoForge 客户端设置完成");
    }
}
