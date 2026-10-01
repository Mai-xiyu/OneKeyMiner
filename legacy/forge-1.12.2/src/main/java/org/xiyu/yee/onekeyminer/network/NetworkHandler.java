package org.xiyu.yee.onekeyminer.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import org.xiyu.yee.onekeyminer.Onekeyminer;

/**
 * 网络处理类 - 用于处理客户端和服务器之间的通信
 */
public class NetworkHandler {

    // 定义网络包ID
    public static final ResourceLocation CHAIN_MODE_ID = new ResourceLocation(Onekeyminer.MODID, "chain_mode");
    public static final ResourceLocation BLOCKS_MINED_ID = new ResourceLocation(Onekeyminer.MODID, "blocks_mined");
    public static final ResourceLocation CHAIN_ACTION_ID = new ResourceLocation(Onekeyminer.MODID, "chain_action");
    // 配置同步数据包ID
    public static final ResourceLocation CONFIG_SYNC_ID = new ResourceLocation(Onekeyminer.MODID, "config_sync");
    public static final ResourceLocation CHAIN_POSITIONS_ID = new ResourceLocation(Onekeyminer.MODID, "chain_positions");

    // 创建类型对象
    public static final Object CHAIN_MODE_TYPE = CHAIN_MODE_ID;
    public static final Object BLOCKS_MINED_TYPE = BLOCKS_MINED_ID;
    public static final Object CHAIN_ACTION_TYPE = CHAIN_ACTION_ID;
    // 配置同步数据包类型
    public static final Object CONFIG_SYNC_TYPE = CONFIG_SYNC_ID;
    public static final Object CHAIN_POSITIONS_TYPE = CHAIN_POSITIONS_ID;

    private static final String PROTOCOL_VERSION = "1.0.0";
    // 延迟初始化 CHANNEL，避免在类加载期调用 NetworkRegistry 导致的 NPE
    public static SimpleNetworkWrapper CHANNEL = null;

    private static int packetIndex = 0;
    private static boolean registered = false;

    /**
     * 注册网络包处理器
     */
    public static void registerPayloads(FMLPreInitializationEvent event) {
        if (registered) {
            return;
        }
        registered = true;

        // 在 preInit 时创建 channel（此时 NetworkRegistry 已可用）
        if (CHANNEL == null) {
            try {
                if (NetworkRegistry.INSTANCE == null) {
                    Onekeyminer.LOGGER.error("NetworkRegistry.INSTANCE is null during registerPayloads - stack:\n", new RuntimeException("NetworkRegistry.INSTANCE is null"));
                }
                CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(Onekeyminer.MODID);
            } catch (Throwable t) {
                // 记录更多诊断信息，帮助定位为何 FML 在 modConstruction 阶段调用 newChannel 并导致 NPE
                Onekeyminer.LOGGER.error("Failed to create SimpleNetworkWrapper channel for {}: {}", Onekeyminer.MODID, t.getMessage(), t);
                // 继续抛出以保持原有失败行为，但先记录堆栈方便调试
                throw t;
            }
        }
        // 1.12.2 使用 registerMessage 注册数据包
        CHANNEL.registerMessage(ChainModePacket.Handler.class, ChainModePacket.class, packetIndex++, Side.SERVER);
        CHANNEL.registerMessage(BlocksMinedPacket.Handler.class, BlocksMinedPacket.class, packetIndex++, Side.CLIENT);
        CHANNEL.registerMessage(ChainActionPacket.Handler.class, ChainActionPacket.class, packetIndex++, Side.CLIENT);
        CHANNEL.registerMessage(ConfigSyncPacket.Handler.class, ConfigSyncPacket.class, packetIndex++, Side.CLIENT);
        CHANNEL.registerMessage(ChainPositionsPacket.Handler.class, ChainPositionsPacket.class, packetIndex++, Side.CLIENT);
    }

    /**
     * 向指定玩家发送网络包
     */
    public static void sendToPlayer(IOneKeyPacket packet, EntityPlayerMP player) {
        try {
            if (player != null && packet instanceof IMessage) {
                CHANNEL.sendTo((IMessage) packet, player);
            }
        } catch (Exception e) {
            Onekeyminer.LOGGER.error("发送数据包时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 向服务器发送网络包
     */
    public static void sendToServer(IOneKeyPacket packet) {
        if (packet instanceof IMessage) {
            CHANNEL.sendToServer((IMessage) packet);
        }
    }
}