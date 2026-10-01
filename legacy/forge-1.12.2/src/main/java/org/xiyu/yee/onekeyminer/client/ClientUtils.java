package org.xiyu.yee.onekeyminer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.xiyu.yee.onekeyminer.Onekeyminer;

@SideOnly(Side.CLIENT)
public class ClientUtils {
    public static void showStatusMessage(String translationKey) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        
        if (player != null) {
            TextComponentTranslation message = new TextComponentTranslation(translationKey);
            player.sendStatusMessage(message, true); // true表示覆盖操作栏消息
        }
    }
    
    public static void showBlockCountMessage(int blockCount) {
        if (blockCount <= 1) return;
        
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        
        if (player != null) {
            player.sendStatusMessage(new TextComponentTranslation("message.onekeyminer.chain_count", blockCount), true);
        }
    }
} 