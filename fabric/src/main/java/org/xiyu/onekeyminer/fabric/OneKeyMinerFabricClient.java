package org.xiyu.onekeyminer.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.xiyu.onekeyminer.OneKeyMiner;

/**
 * Fabric 平台客户端入口点
 * 
 * <p>负责客户端专用功能的初始化，如配置界面、按键绑定等。</p>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.0
 * @since Minecraft 1.21.9
 */
@Environment(EnvType.CLIENT)
public class OneKeyMinerFabricClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        // 注册按键绑定（这会注册 ClientTickEvents）
        KeyBindings.register();
        
        // 注册配置界面按键处理（原生实现，不依赖 ModMenu/Cloth Config）
        registerConfigKeyHandler();
        
        OneKeyMiner.LOGGER.info("OneKeyMiner Fabric 客户端模块初始化完成");
    }
    
    /**
     * 注册配置界面快捷键处理器
     */
    private void registerConfigKeyHandler() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KeyBindings.OPEN_CONFIG.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new FabricConfigScreen(null));
                }
            }
        });
    }
}
