package org.xiyu.yee.onekeyminer.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.xiyu.yee.onekeyminer.client.ClientUtils;
import org.xiyu.yee.onekeyminer.config.ClientConfig;

/**
 * 客户端专用网络处理器
 * 处理只能在客户端执行的网络包
 */
@SideOnly(Side.CLIENT)
public class ClientNetworkHandler {
      /**
     * 处理挖掘方块数量消息
     */
    public static final java.util.function.BiConsumer<BlocksMinedPacket, net.minecraftforge.fml.common.network.simpleimpl.MessageContext> BLOCKS_MINED_HANDLER = (packet, context) -> {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft.player != null && ClientConfig.INSTANCE.showBlockCount.get()) {
                ClientUtils.showBlockCountMessage(packet.getBlockCount());
            }
        });
    };
}
