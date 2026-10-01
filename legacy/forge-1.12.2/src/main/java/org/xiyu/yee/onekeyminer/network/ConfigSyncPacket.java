package org.xiyu.yee.onekeyminer.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.xiyu.yee.onekeyminer.Onekeyminer;
import org.xiyu.yee.onekeyminer.config.ClientConfig;

/**
 * 配置同步数据包 - 将服务器配置更改应用到客户端
 */
public class ConfigSyncPacket implements IOneKeyPacket, IMessage {
    private String configKey;
    private String configValue;
    
    public ConfigSyncPacket() {
        // 无参构造函数必须存在
    }
    
    public ConfigSyncPacket(String configKey, String configValue) {
        this.configKey = configKey;
        this.configValue = configValue;
    }
    
    @Override
    public ResourceLocation id() {
        return NetworkHandler.CONFIG_SYNC_ID;
    }
    
    @Override
    public Object type() {
        return NetworkHandler.CONFIG_SYNC_TYPE;
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        configKey = ByteBufUtils.readUTF8String(buf);
        configValue = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, configKey);
        ByteBufUtils.writeUTF8String(buf, configValue);
    }
    
    @Override
    public void write(ByteBuf buffer) {
        toBytes(buffer);
    }
    
    public static class Handler implements IMessageHandler<ConfigSyncPacket, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(ConfigSyncPacket message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                String key = message.configKey;
                String value = message.configValue;
                
                // 根据配置键应用相应的配置值
                switch (key) {
                    case "keyhold":
                        ClientConfig.INSTANCE.requireKeyHold.set(Boolean.parseBoolean(value));
                        break;
                    case "showcount":
                        ClientConfig.INSTANCE.showBlockCount.set(Boolean.parseBoolean(value));
                        break;
                    case "messagestyle":
                        ClientConfig.INSTANCE.messageStyle.set(value);
                        break;
                    default:
                        Onekeyminer.LOGGER.warn("收到未知配置键: {}", key);
                }
                
                Onekeyminer.LOGGER.debug("客户端配置已更新: {} = {}", key, value);
            });
            return null;
        }
    }
} 