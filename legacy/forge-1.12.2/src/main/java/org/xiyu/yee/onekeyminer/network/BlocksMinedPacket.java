package org.xiyu.yee.onekeyminer.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.xiyu.yee.onekeyminer.client.ClientUtils;
import org.xiyu.yee.onekeyminer.config.ClientConfig;

/**
 * 挖掘方块数量消息数据包
 */
public class BlocksMinedPacket implements IOneKeyPacket, IMessage {
    private int blockCount;
    
    public BlocksMinedPacket() {
        // 无参构造函数必须存在
    }
    
    public BlocksMinedPacket(int blockCount) {
        this.blockCount = blockCount;
    }
    
    /**
     * 获取方块数量
     */
    public int getBlockCount() {
        return blockCount;
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        blockCount = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(blockCount);
    }

    @Override
    public void write(ByteBuf buf) {
        toBytes(buf);
    }
    
    @Override
    public ResourceLocation id() {
        return NetworkHandler.BLOCKS_MINED_ID;
    }
    
    @Override
    public Object type() {
        return NetworkHandler.BLOCKS_MINED_TYPE;
    }
    
    public static class Handler implements IMessageHandler<BlocksMinedPacket, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(BlocksMinedPacket message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                Minecraft minecraft = Minecraft.getMinecraft();
                if (minecraft.player != null && ClientConfig.INSTANCE.showBlockCount.get()) {
                    ClientUtils.showBlockCountMessage(message.blockCount);
                }
            });
            return null;
        }
    }
} 