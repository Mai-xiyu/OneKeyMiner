package org.xiyu.yee.onekeyminer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.xiyu.yee.onekeyminer.Onekeyminer;

public class ClientUtils {
    public static void showStatusMessage(String translationKey) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        
        if (player != null) {
            net.minecraft.network.chat.TranslatableComponent message = new net.minecraft.network.chat.TranslatableComponent(translationKey);
            player.displayClientMessage(message, true); // true表示覆盖操作栏消息
        }
    }
    
    public static void showBlockCountMessage(int blockCount) {
        if (blockCount <= 1) return;
        
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        
        if (player != null) {
            player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent("message.onekeyminer.chain_count", blockCount), true);
        }
    }
} 