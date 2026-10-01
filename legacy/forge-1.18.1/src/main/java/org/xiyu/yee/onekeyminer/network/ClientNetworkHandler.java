package org.xiyu.yee.onekeyminer.network;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.xiyu.yee.onekeyminer.client.ClientUtils;
import org.xiyu.yee.onekeyminer.config.ClientConfig;

/**
 * 客户端专用网络处理器
 * 处理只能在客户端执行的网络包
 */
@OnlyIn(Dist.CLIENT)
public class ClientNetworkHandler {
      /**
     * 处理挖掘方块数量消息
     */
    public static final java.util.function.BiConsumer<BlocksMinedPacket, NetworkEvent.Context> BLOCKS_MINED_HANDLER = (packet, context) -> {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && ClientConfig.INSTANCE.showBlockCount.get()) {
                ClientUtils.showBlockCountMessage(packet.getBlockCount());
            }
        });
        context.setPacketHandled(true);
    };
}
