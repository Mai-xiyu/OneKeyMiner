package org.xiyu.yee.onekeyminer.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.xiyu.yee.onekeyminer.Onekeyminer;
import org.xiyu.yee.onekeyminer.config.ClientConfig;

/**
 * 配置同步数据包 - 将服务器配置更改应用到客户端
 */
public class ConfigSyncPacket implements IOneKeyPacket {
    private final String configKey;
    private final String configValue;
    
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
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(configKey);
        buffer.writeUtf(configValue);
    }
    
    public static ConfigSyncPacket fromNetwork(FriendlyByteBuf buffer) {
        String key = buffer.readUtf();
        String value = buffer.readUtf();
        return new ConfigSyncPacket(key, value);
    }
    
    @OnlyIn(Dist.CLIENT)
    public static void handleOnClient(ConfigSyncPacket packet, NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            String key = packet.configKey;
            String value = packet.configValue;
            
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
        context.setPacketHandled(true);
    }
} 