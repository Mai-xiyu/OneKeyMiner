package org.xiyu.yee.onekeyminer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.entity.player.EntityPlayer;
import org.xiyu.yee.onekeyminer.Onekeyminer;
import org.xiyu.yee.onekeyminer.config.ClientConfig;
import org.xiyu.yee.onekeyminer.network.ChainActionPacket;

/**
 * 客户端数据包处理器 - 用于处理服务器发送到客户端的数据包
 */
public class ClientPacketHandler {
    
    /**
     * 处理连锁操作消息
     */
    public static void handleChainActionPacket(ChainActionPacket packet) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        
        // 安全检查
        if (player == null) {
            if(ClientConfig.INSTANCE.Debug.get()) {
                Onekeyminer.LOGGER.error("无法显示连锁消息: 客户端玩家为空");
            }
            return;
        }
        
        // 获取操作类型和计数
        final String actionType = packet.getActionType();
        final int count = packet.getCount();
        
        // 添加调试日志
        
        // 在游戏线程上显示消息
        minecraft. addScheduledTask(() -> {
            // 根据操作类型构建不同的消息
        ITextComponent message;
            switch (actionType) {
                case "mining":
            message = new TextComponentTranslation("message.onekeyminer.chain_mining", count);
                    break;
                case "interaction":
            message = new TextComponentTranslation("message.onekeyminer.chain_interaction", count);
                    break;
                case "planting":
                    // 添加调试日志
                    if(ClientConfig.INSTANCE.Debug.get()) {
                        Onekeyminer.LOGGER.debug("显示连锁种植消息，数量: {}", count);
                    }
            message = new TextComponentTranslation("message.onekeyminer.chain_planting", count);
                    break;
                case "shearing":
                    if(ClientConfig.INSTANCE.Debug.get()) {
                        Onekeyminer.LOGGER.debug("显示连锁剪羊毛消息，数量: {}", count);
                    }
            message = new TextComponentTranslation("message.onekeyminer.chain_shearing", count);
                    break;
                default:
            message = new TextComponentTranslation("message.onekeyminer.chain_action", actionType, count);
            }
            
            // 确保非空并显示消息
            if (message != null && player != null) {
                // 在操作栏显示消息
                player.sendStatusMessage(message, true);
                
                // 添加成功日志
            } else {
                if(ClientConfig.INSTANCE.Debug.get()) {
                Onekeyminer.LOGGER.warn("无法显示连锁消息: 消息或玩家为空");
            }}
        });
    }
} 