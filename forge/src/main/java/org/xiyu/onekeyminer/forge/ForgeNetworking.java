package org.xiyu.onekeyminer.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.SimpleChannel;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.platform.PlatformServices;

/**
 * Forge 网络数据包处理
 * 
 * <p>用于客户端和服务端之间的按键状态同步。</p>
 * <p>使用 SimpleChannel API 实现网络通信。</p>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.0
 * @since Minecraft 1.21.9
 */
public class ForgeNetworking {
    
    /** 网络通道 */
    private static final SimpleChannel CHANNEL = ChannelBuilder
            .named(Identifier.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "main"))
            .optional()
            .simpleChannel();
    
    /** 包ID计数器 */
    private static int packetIndex = 0;
    
    /** 是否已注册 */
    private static boolean registered = false;
    
    /**
     * 按键状态数据包
     */
    public static class ChainKeyStatePacket {
        private final boolean pressed;
        
        public ChainKeyStatePacket(boolean pressed) {
            this.pressed = pressed;
        }
        
        public static ChainKeyStatePacket fromNetwork(FriendlyByteBuf buf) {
            return new ChainKeyStatePacket(buf.readBoolean());
        }
        
        public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(pressed);
        }
        
        public boolean isPressed() {
            return pressed;
        }
        
        /**
         * 服务端处理按键状态包
         */
        public static void handleOnServer(ChainKeyStatePacket packet, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    PlatformServices.getInstance().setChainModeActive(player, packet.isPressed());
                    OneKeyMiner.LOGGER.info("服务端收到按键状态包 - 玩家 {} 连锁模式: {}", 
                            player.getName().getString(), packet.isPressed() ? "激活" : "关闭");
                } else {
                    OneKeyMiner.LOGGER.warn("服务端收到按键状态包但玩家为空");
                }
            });
            context.setPacketHandled(true);
        }
    }
    
    /**
     * 注册网络数据包
     */
    public static void register() {
        if (registered) {
            return;
        }
        
        try {
            registered = true;
            
            CHANNEL.messageBuilder(ChainKeyStatePacket.class, packetIndex++, NetworkDirection.PLAY_TO_SERVER)
                    .encoder(ChainKeyStatePacket::write)
                    .decoder(ChainKeyStatePacket::fromNetwork)
                    .consumerNetworkThread(ChainKeyStatePacket::handleOnServer)
                    .add();
            
            OneKeyMiner.LOGGER.debug("已注册 Forge 网络通道");
        } catch (Exception e) {
            OneKeyMiner.LOGGER.error("注册 Forge 网络通道失败", e);
        }
    }
    
    /**
     * 从客户端发送按键状态到服务端
     */
    public static void sendKeyState(boolean pressed) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            try {
                CHANNEL.send(new ChainKeyStatePacket(pressed), connection.getConnection());
            } catch (Exception e) {
                OneKeyMiner.LOGGER.debug("发送按键状态包失败: {}", e.getMessage());
            }
        } else {
            OneKeyMiner.LOGGER.debug("无法发送到服务器：客户端连接为空");
        }
    }
}
