package org.xiyu.onekeyminer.forge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.platform.PlatformServices;

import java.util.Optional;
import java.util.function.Supplier;

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
        private static final String PROTOCOL_VERSION = "1";
        private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(OneKeyMiner.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
        );
    
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
        public static void handleOnServer(ChainKeyStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    PlatformServices.getInstance().setChainModeActive(player, packet.isPressed());
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
            
                CHANNEL.registerMessage(
                    packetIndex++,
                    ChainKeyStatePacket.class,
                    ChainKeyStatePacket::write,
                    ChainKeyStatePacket::fromNetwork,
                    ChainKeyStatePacket::handleOnServer,
                    Optional.of(NetworkDirection.PLAY_TO_SERVER)
                );
            
            OneKeyMiner.LOGGER.debug("已注册 Forge 网络通道");
        } catch (Exception e) {
            OneKeyMiner.LOGGER.error("注册 Forge 网络通道失败", e);
        }
    }
    
    /**
     * 从客户端发送按键状态到服务端
     */
    public static void sendKeyState(boolean pressed) {
        try {
            CHANNEL.sendToServer(new ChainKeyStatePacket(pressed));
        } catch (Exception e) {
            OneKeyMiner.LOGGER.debug("发送按键状态包失败: {}", e.getMessage());
        }
    }
}
