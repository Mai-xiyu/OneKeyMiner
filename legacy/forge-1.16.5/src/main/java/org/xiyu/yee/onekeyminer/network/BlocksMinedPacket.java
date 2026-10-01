package org.xiyu.yee.onekeyminer.network;
import net.minecraft.network.PacketBuffer;

import net.minecraft.client.Minecraft;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;
import org.xiyu.yee.onekeyminer.client.ClientUtils;
import org.xiyu.yee.onekeyminer.config.ClientConfig;


/**
 * 挖掘方块数量消息数据包
 */
public class BlocksMinedPacket implements IOneKeyPacket {
    private final int blockCount;
    
    public BlocksMinedPacket(int blockCount) {
        this.blockCount = blockCount;
    }
    
    /**
     * 获取方块数量
     */
    public int getBlockCount() {
        return blockCount;
    }
    
    public static BlocksMinedPacket fromNetwork(net.minecraft.network.PacketBuffer buf) {
        return new BlocksMinedPacket(buf.readInt());
    }
    

    @Override
    public void write(net.minecraft.network.PacketBuffer buf) {
        buf.writeInt(blockCount);
    }
    
    @Override
    public ResourceLocation id() {
        return NetworkHandler.BLOCKS_MINED_ID;
    }
    
    @Override
    public Object type() {
        return NetworkHandler.BLOCKS_MINED_TYPE;
    }
    
    @OnlyIn(Dist.CLIENT)
    public static void handleOnClient(BlocksMinedPacket packet, net.minecraftforge.fml.network.NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && ClientConfig.INSTANCE.showBlockCount.get()) {
                ClientUtils.showBlockCountMessage(packet.blockCount);
            }
        });
        context.setPacketHandled(true);
    }
} 