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
import org.xiyu.yee.onekeyminer.client.ClientPacketHandler;
import org.xiyu.yee.onekeyminer.Onekeyminer;
import org.xiyu.yee.onekeyminer.config.ClientConfig;

/**
 * 连锁操作数据包 - 用于向客户端发送连锁操作信息
 */
public class ChainActionPacket implements IOneKeyPacket, IMessage {
    private String actionType; // 操作类型: "mining", "interaction", "planting"
    private int count;         // 操作的方块数量
    
    public ChainActionPacket() {
        // 无参构造函数必须存在
    }
    
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
    
    @Override
    public void fromBytes(ByteBuf buf) {
        actionType = ByteBufUtils.readUTF8String(buf);
        count = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, actionType);
        buf.writeInt(count);
    }
    
    /**
     * 写入网络数据
     */
    @Override
    public void write(ByteBuf buffer) {
        toBytes(buffer);
    }
    
    public static class Handler implements IMessageHandler<ChainActionPacket, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(ChainActionPacket message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                try {
                    if(ClientConfig.INSTANCE.Debug.get()) Onekeyminer.LOGGER.debug("ChainActionPacket.handleOnClient 被调用");
                    ClientPacketHandler.handleChainActionPacket(message);
                } catch (Exception e) {
                    Onekeyminer.LOGGER.error("处理连锁消息数据包时出错: {}", e.getMessage(), e);
                }
            });
            return null;
        }
    }
} 