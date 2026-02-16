package org.xiyu.onekeyminer.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.ConfigSyncHelper;

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
        // 注册配置同步回调
        ConfigSyncHelper.registerSyncCallback(() -> {
            // 配置变更后的回调
        });
        
        // 注册按键绑定（这会注册 ClientTickEvents）
        KeyBindings.register();
        
        // 注册配置界面（如果 Mod Menu 存在）
        // 配置界面通过 fabric.mod.json 的 entrypoints 注册
        
        OneKeyMiner.LOGGER.info("OneKeyMiner Fabric 客户端模块初始化完成");
    }
}
