package org.xiyu.yee.onekeyminer.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
// import net.minecraft.network.chat.Component; // Removed unused import
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.xiyu.yee.onekeyminer.network.ChainModePacket;
import org.xiyu.yee.onekeyminer.network.NetworkHandler;
import org.xiyu.yee.onekeyminer.capability.ChainModeCapability;

@Mod.EventBusSubscriber
public class ChainModeToggleKey {
    public static KeyBinding chainModeKey;
    private static boolean currentState = false;
    
    @SubscribeEvent
    public static void onKeyInput(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        if (chainModeKey != null && chainModeKey.isPressed()) {
            // 切换连锁模式
            currentState = !currentState;
            
            // 发送到服务器
            NetworkHandler.sendToServer(new ChainModePacket(currentState));
            
            // 显示消息 - 消息现在会通过服务器的回复显示
        }
    }
} 