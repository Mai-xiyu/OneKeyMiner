package org.xiyu.onekeyminer.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.xiyu.onekeyminer.OneKeyMiner;

/**
 * Fabric 按键绑定注册
 * 
 * <p>注册模组使用的快捷键，如按住连锁挖矿激活的按键。</p>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.0
 * @since Minecraft 1.21.9
 */
@Environment(EnvType.CLIENT)
public class KeyBindings {
    
    /** 连锁挖矿激活按键（默认：波浪键/反引号键）- 需要按住 */
    public static KeyMapping CHAIN_MINING_KEY;
    
    /** 打开配置界面的按键（默认：无） */
    public static KeyMapping OPEN_CONFIG;
    
    /** 上一帧按键是否被按下 */
    private static boolean wasKeyDown = false;

    /** 按键状态网络包 ID */
    public static final ResourceLocation CHAIN_KEY_STATE_ID = new ResourceLocation(OneKeyMiner.MOD_ID, "chain_key_state");
    
    /**
     * 注册所有按键绑定
     */
    public static void register() {
        // 连锁挖矿激活按键（按住模式）
        CHAIN_MINING_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.onekeyminer.hold",             // 翻译键
                InputConstants.Type.KEYSYM,         // 输入类型
                GLFW.GLFW_KEY_GRAVE_ACCENT,        // 默认按键（`键）
            "key.categories.onekeyminer"       // 使用自定义分类
        ));
        
        // 打开配置界面（默认未绑定）
        OPEN_CONFIG = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.onekeyminer.config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,              // 默认未绑定
            "key.categories.onekeyminer"       // 使用自定义分类
        ));
        
        // 注册按键处理
        registerKeyHandler();
        
        OneKeyMiner.LOGGER.debug("已注册 Fabric 按键绑定");
    }
    
    /**
     * 注册按键事件处理器 - 按住模式
     */
    private static void registerKeyHandler() {
        // 使用 Fabric 的客户端 tick 事件来检查按键状态
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 检查按键当前状态
            boolean isKeyDown = CHAIN_MINING_KEY.isDown();
            
            // 只在状态变化时发送网络包
            if (isKeyDown != wasKeyDown) {
                wasKeyDown = isKeyDown;
                
                // 发送按键状态到服务端
                if (client.getConnection() != null) {
                    try {
                        FriendlyByteBuf buf = PacketByteBufs.create();
                        buf.writeBoolean(isKeyDown);
                        ClientPlayNetworking.send(CHAIN_KEY_STATE_ID, buf);
                    } catch (Exception e) {
                        OneKeyMiner.LOGGER.error("发送按键状态网络包失败: {}", e.getMessage());
                    }
                }
            }
            
            // 检查配置界面按键
            while (OPEN_CONFIG.consumeClick()) {
                // 打开配置界面
            }
        });
    }
    
}
