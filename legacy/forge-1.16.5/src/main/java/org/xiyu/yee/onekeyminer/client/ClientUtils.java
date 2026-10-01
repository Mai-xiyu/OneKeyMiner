package org.xiyu.yee.onekeyminer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.xiyu.yee.onekeyminer.Onekeyminer;

public class ClientUtils {
    public static void showStatusMessage(String translationKey) {
        Minecraft minecraft = Minecraft.getInstance();
        PlayerEntity player = minecraft.player;
        
        if (player != null) {
            TranslationTextComponent message = new TranslationTextComponent(translationKey);
            player.displayClientMessage(message, true); // true表示覆盖操作栏消息
        }
    }
    
    public static void showBlockCountMessage(int blockCount) {
        if (blockCount <= 1) return;
        
        Minecraft minecraft = Minecraft.getInstance();
        PlayerEntity player = minecraft.player;
        
        if (player != null) {
            player.displayClientMessage(new TranslationTextComponent("message.onekeyminer.chain_count", blockCount), true);
        }
    }
} 