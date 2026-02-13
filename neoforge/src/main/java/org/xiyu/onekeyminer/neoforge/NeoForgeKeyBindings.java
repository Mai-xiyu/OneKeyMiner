package org.xiyu.onekeyminer.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import org.xiyu.onekeyminer.OneKeyMiner;

/**
 * NeoForge 按键绑定注册 (修复版)
 * <p>修复了配置按键不响应的问题。</p>
 * @author OneKeyMiner Team
 * @version 1.0.1
 * @since Minecraft 1.21.9
 */
@OnlyIn(Dist.CLIENT)
public class NeoForgeKeyBindings {
    
    /** 连锁挖矿激活按键 */
    public static KeyMapping CHAIN_MINING_KEY;
    
    /** 打开配置界面的按键 */
    public static KeyMapping OPEN_CONFIG;
    
    /** 上一帧按键是否被按下 */
    private static boolean wasKeyDown = false;
    
    public static void register() {
        // 防止重复创建 KeyMapping 对象（RegisterKeyMappingsEvent 和 FMLClientSetupEvent 都会调用）
        // 重复创建会导致游戏内重绑定按键无效：游戏 UI 控制旧对象，tick 检测读取新对象
        if (CHAIN_MINING_KEY != null) return;
        
        // 创建按键映射
        CHAIN_MINING_KEY = new KeyMapping("key.onekeyminer.hold", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT, "key.categories.onekeyminer");
        OPEN_CONFIG = new KeyMapping("key.onekeyminer.config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.onekeyminer");
        
        // 注册 Tick 监听
        NeoForge.EVENT_BUS.addListener(NeoForgeKeyBindings::onClientTick);
        OneKeyMiner.LOGGER.debug("已注册 NeoForge 按键绑定");
    }
    
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        if (CHAIN_MINING_KEY == null) register(); // 确保初始化
        event.register(CHAIN_MINING_KEY);
        event.register(OPEN_CONFIG);
        OneKeyMiner.LOGGER.debug("已通过事件注册 NeoForge 按键映射");
    }
    
    /**
     * 客户端 tick 事件处理
     */
    private static void onClientTick(ClientTickEvent.Post event) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) return;
        
        // 1. 处理配置按键 (这是之前遗漏的逻辑！)
        if (OPEN_CONFIG != null && OPEN_CONFIG.consumeClick()) {
            // 直接调用配置界面的静态创建方法，绕过 ModListScreen
            mc.setScreen(NeoForgeConfigScreen.createConfigScreen(mc.screen));
        }

        // 2. 处理连锁挖矿按键
        if (CHAIN_MINING_KEY == null) return;
        boolean isKeyDown = CHAIN_MINING_KEY.isDown();
        
        if (isKeyDown != wasKeyDown) {
            wasKeyDown = isKeyDown;
            if (mc.getConnection() != null) {
                try {
                    NeoForgeNetworking.sendKeyState(isKeyDown);
                    OneKeyMiner.LOGGER.debug("连锁按键状态变化: {}", isKeyDown ? "按下" : "释放");
                } catch (Exception e) {
                    OneKeyMiner.LOGGER.debug("发送按键状态失败: {}", e.getMessage());
                }
            }
        }
    }
}