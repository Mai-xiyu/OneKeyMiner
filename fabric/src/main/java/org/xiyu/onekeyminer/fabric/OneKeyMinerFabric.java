package org.xiyu.onekeyminer.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.platform.PlatformServices;

/**
 * Fabric 平台模组入口点
 * 
 * <p>负责在 Fabric 平台上初始化 OneKeyMiner 模组，
 * 包括注册事件监听器和配置界面。</p>
 * 
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.21.9
 */
public class OneKeyMinerFabric implements ModInitializer {
    
    @Override
    public void onInitialize() {
        // 首先初始化平台服务（必须在 OneKeyMiner.init() 之前）
        PlatformServices.setInstance(new FabricPlatformServices());
        
        // 初始化通用模块
        OneKeyMiner.init();
        
        // 注册事件处理器
        FabricEventHandler.register();
        
        // 注册网络包
        registerNetworking();
        
        OneKeyMiner.LOGGER.info("OneKeyMiner Fabric 模块初始化完成");
    }
    
    /**
     * 注册网络包处理
     */
    private void registerNetworking() {
        // 注册按键状态包（客户端到服务端）
        PayloadTypeRegistry.playC2S().register(
                KeyBindings.ChainKeyStatePayload.TYPE,
                KeyBindings.ChainKeyStatePayload.STREAM_CODEC
        );
        
        // 注册服务端接收处理
        ServerPlayNetworking.registerGlobalReceiver(
                KeyBindings.ChainKeyStatePayload.TYPE,
                (payload, context) -> {
                    // 更新玩家的按键状态
                    context.server().execute(() -> {
                        if (context.player() != null) {
                            PlatformServices.getInstance().setChainModeActive(context.player(), payload.holding());
                            OneKeyMiner.LOGGER.info("服务端收到按键状态包 - 玩家 {} 连锁模式: {}",
                                    context.player().getName().getString(),
                                    payload.holding() ? "激活" : "关闭");
                        }
                    });
                }
        );
        
        OneKeyMiner.LOGGER.debug("已注册 Fabric 网络包处理");
    }
}
