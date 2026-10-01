package org.xiyu.yee.onekeyminer.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
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
    
    public static BlocksMinedPacket fromNetwork(FriendlyByteBuf buf) {
        return new BlocksMinedPacket(buf.readInt());
    }
    

    @Override
    public void write(FriendlyByteBuf buf) {
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
    public static void handleOnClient(BlocksMinedPacket packet, NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && ClientConfig.INSTANCE.showBlockCount.get()) {
                ClientUtils.showBlockCountMessage(packet.blockCount);
            }
        });
        context.setPacketHandled(true);
    }
} 