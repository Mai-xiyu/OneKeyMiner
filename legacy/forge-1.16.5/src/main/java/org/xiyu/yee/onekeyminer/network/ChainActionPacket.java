package org.xiyu.yee.onekeyminer.network;
import net.minecraft.network.PacketBuffer;

import net.minecraft.client.Minecraft;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;
import org.xiyu.yee.onekeyminer.client.ClientPacketHandler;
import org.xiyu.yee.onekeyminer.Onekeyminer;
import org.xiyu.yee.onekeyminer.config.ClientConfig;

/**
 * 连锁操作数据包 - 用于向客户端发送连锁操作信息
 */
public class ChainActionPacket implements IOneKeyPacket {
    private final String actionType; // 操作类型: "mining", "interaction", "planting"
    private final int count;         // 操作的方块数量
    
    public ChainActionPacket(String actionType, int count) {
        this.actionType = actionType;
        this.count = count;
    }
    
    /**
     * 获取操作类型
     */
    public String getActionType() {
        return actionType;
    }
    
    /**
     * 获取操作数量
     */
    public int getCount() {
        return count;
    }
    
    @Override
    public ResourceLocation id() {
        return NetworkHandler.CHAIN_ACTION_ID;
    }
    
    /**
     * 写入网络数据
     */
    @Override
    public void write(net.minecraft.network.PacketBuffer buffer) {
        buffer.writeUtf(actionType);
        buffer.writeInt(count);
    }
    
    /**
     * 从网络数据创建数据包
     */
    public static ChainActionPacket fromNetwork(net.minecraft.network.PacketBuffer buffer) {
        String actionType = buffer.readUtf();
        int count = buffer.readInt();
        return new ChainActionPacket(actionType, count);
    }
    
    /**
     * 在客户端处理数据包
     */
    @OnlyIn(Dist.CLIENT)
    public static void handleOnClient(ChainActionPacket packet, net.minecraftforge.fml.network.NetworkEvent.Context context) {
        if(ClientConfig.INSTANCE.Debug.get()) Onekeyminer.LOGGER.debug("ChainActionPacket.handleOnClient 被调用");
        context.enqueueWork(() -> {
            try {
                ClientPacketHandler.handleChainActionPacket(packet);
            } catch (Exception e) {
                Onekeyminer.LOGGER.error("处理连锁消息数据包时出错: {}", e.getMessage(), e);
            }
        });
        context.setPacketHandled(true);
    }
} 