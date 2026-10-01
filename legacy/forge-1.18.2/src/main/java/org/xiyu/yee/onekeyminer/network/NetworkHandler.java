package org.xiyu.yee.onekeyminer.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import org.xiyu.yee.onekeyminer.Onekeyminer;
import org.xiyu.yee.onekeyminer.config.CommonConfig;

import java.util.Optional;

/**
 * 网络处理类 - 用于处理客户端和服务器之间的通信
 */
@Mod.EventBusSubscriber(modid = Onekeyminer.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
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
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(Onekeyminer.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );


    private static int packetIndex = 0;
    private static boolean registered = false;

    /**
     * 注册网络包处理器
     */
    @SubscribeEvent
    public static void registerPayloads(FMLCommonSetupEvent event) {
        if (registered) {
            return;
        }
        event.enqueueWork(() -> {
            if (registered) {
                return;
            }
            registered = true;

    // 使用 SimpleChannel.registerMessage 的兼容方式注册（1.20.1 风格）
    CHANNEL.registerMessage(packetIndex++, ChainModePacket.class,
        (packet, buf) -> packet.write(buf),
        ChainModePacket::fromNetwork,
        (packet, ctxSupplier) -> ChainModePacket.handleOnServer((ChainModePacket) packet, ctxSupplier.get()), Optional.of(NetworkDirection.PLAY_TO_SERVER));

    CHANNEL.registerMessage(packetIndex++, BlocksMinedPacket.class,
        (packet, buf) -> packet.write(buf),
        BlocksMinedPacket::fromNetwork,
        (packet, ctxSupplier) -> BlocksMinedPacket.handleOnClient((BlocksMinedPacket) packet, ctxSupplier.get()), Optional.of(NetworkDirection.PLAY_TO_CLIENT));

    CHANNEL.registerMessage(packetIndex++, ChainActionPacket.class,
        (packet, buf) -> packet.write(buf),
        ChainActionPacket::fromNetwork,
        (packet, ctxSupplier) -> ChainActionPacket.handleOnClient((ChainActionPacket) packet, ctxSupplier.get()), Optional.of(NetworkDirection.PLAY_TO_CLIENT));

    CHANNEL.registerMessage(packetIndex++, ConfigSyncPacket.class,
        (packet, buf) -> packet.write(buf),
        ConfigSyncPacket::fromNetwork,
        (packet, ctxSupplier) -> ConfigSyncPacket.handleOnClient((ConfigSyncPacket) packet, ctxSupplier.get()), Optional.of(NetworkDirection.PLAY_TO_CLIENT));

    CHANNEL.registerMessage(packetIndex++, ChainPositionsPacket.class,
        (packet, buf) -> packet.write(buf),
        ChainPositionsPacket::fromNetwork,
        (packet, ctxSupplier) -> ChainPositionsPacket.handleOnClient((ChainPositionsPacket) packet, ctxSupplier.get()), Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        });
    }

    /**
     * 向指定玩家发送网络包
     */
    public static void sendToPlayer(IOneKeyPacket packet, ServerPlayer player) {
        try {
            if (player != null) {
                // 添加调试日志
                if(CommonConfig.INSTANCE.enableDebugMode.get()) Onekeyminer.LOGGER.debug("向玩家 {} 发送数据包: {}",
                        player.getScoreboardName(), packet.getClass().getSimpleName());

                // 使用 PacketDistributor 将封包发送到指定玩家
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            } else {
                if(CommonConfig.INSTANCE.enableDebugMode.get()) Onekeyminer.LOGGER.warn("无法发送数据包: 玩家或连接为空");
            }
        } catch (Exception e) {
            Onekeyminer.LOGGER.error("发送数据包时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 向服务器发送网络包
     */
    public static void sendToServer(IOneKeyPacket packet) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            // 客户端发送到服务器（使用 SimpleChannel 的 sendToServer 或 send 方法）
            CHANNEL.sendToServer(packet);
        } else {
            if (CommonConfig.INSTANCE.enableDebugMode.get()) {
                Onekeyminer.LOGGER.warn("无法发送到服务器：客户端连接为空");
            }
        }
    }

    /**
     * 初始化客户端数据包处理器
     */
    // public static void registerClientPacketHandlers() {
    //     // 删除内部调用
    //     registerClientPacketHandlers(ChainActionPacket.class, (packet, context) -> {
    //         return () -> ClientPacketHandler.handleChainActionPacket(packet);
    //     });
    // }
}