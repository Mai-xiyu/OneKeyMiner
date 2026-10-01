package org.xiyu.yee.onekeyminer.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
// import net.minecraft.network.chat.Component; // Removed unused import
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.xiyu.yee.onekeyminer.network.ChainModePacket;
import org.xiyu.yee.onekeyminer.network.NetworkHandler;
import org.xiyu.yee.onekeyminer.capability.ChainModeCapability;

@Mod.EventBusSubscriber
public class ChainModeToggleKey {
    public static KeyBinding chainModeKey;
    private static boolean currentState = false;
    
    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
    if (chainModeKey != null && chainModeKey.consumeClick()) {
            // 切换连锁模式
            currentState = !currentState;
            
            // 发送到服务器
            NetworkHandler.sendToServer(new ChainModePacket(currentState));
            
            // 显示消息 - 消息现在会通过服务器的回复显示
        }
    }
} 