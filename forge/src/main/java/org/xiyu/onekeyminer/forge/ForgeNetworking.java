package org.xiyu.onekeyminer.forge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.mining.MiningStateManager;
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
     * 按键状态数据包（含形状 ID）
     */
    public static class ChainKeyStatePacket {
        private final boolean pressed;
        private final String shapeId;
        
        public ChainKeyStatePacket(boolean pressed, String shapeId) {
            this.pressed = pressed;
            this.shapeId = shapeId != null ? shapeId : "onekeyminer:amorphous";
        }
        
        public static ChainKeyStatePacket fromNetwork(FriendlyByteBuf buf) {
            boolean pressed = buf.readBoolean();
            String shapeId = buf.readUtf(256);
            return new ChainKeyStatePacket(pressed, shapeId);
        }
        
        public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(pressed);
            buf.writeUtf(shapeId);
        }
        
        public boolean isPressed() {
            return pressed;
        }
        
        public String getShapeId() {
            return shapeId;
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
                    try {
                        ResourceLocation shapeRL = new ResourceLocation(packet.getShapeId());
                        MiningStateManager.setPlayerShape(player, shapeRL);
                    } catch (Exception e) {
                        OneKeyMiner.LOGGER.debug("无效的形状 ID: {}", packet.getShapeId());
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }
    
    /**
     * 传送设置数据包
     */
    public static class TeleportSettingsPacket {
        private final boolean teleportDrops;
        private final boolean teleportExp;
        
        public TeleportSettingsPacket(boolean teleportDrops, boolean teleportExp) {
            this.teleportDrops = teleportDrops;
            this.teleportExp = teleportExp;
        }
        
        public static TeleportSettingsPacket fromNetwork(FriendlyByteBuf buf) {
            boolean drops = buf.readBoolean();
            boolean exp = buf.readBoolean();
            return new TeleportSettingsPacket(drops, exp);
        }
        
        public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(teleportDrops);
            buf.writeBoolean(teleportExp);
        }
        
        /**
         * 服务端处理传送设置包
         */
        public static void handleOnServer(TeleportSettingsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    MiningStateManager.setTeleportDrops(player, packet.teleportDrops);
                    MiningStateManager.setTeleportExp(player, packet.teleportExp);
                    OneKeyMiner.LOGGER.debug("玩家 {} 更新传送设置: 掉落物={}, 经验={}",
                            player.getName().getString(), packet.teleportDrops, packet.teleportExp);
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
                
                CHANNEL.registerMessage(
                    packetIndex++,
                    TeleportSettingsPacket.class,
                    TeleportSettingsPacket::write,
                    TeleportSettingsPacket::fromNetwork,
                    TeleportSettingsPacket::handleOnServer,
                    Optional.of(NetworkDirection.PLAY_TO_SERVER)
                );
            
            OneKeyMiner.LOGGER.debug("已注册 Forge 网络通道");
        } catch (Exception e) {
            OneKeyMiner.LOGGER.error("注册 Forge 网络通道失败", e);
        }
    }
    
    /**
     * 从客户端发送按键状态和形状 ID 到服务端
     */
    public static void sendKeyState(boolean pressed, String shapeId) {
        try {
            CHANNEL.sendToServer(new ChainKeyStatePacket(pressed, shapeId));
        } catch (Exception e) {
            OneKeyMiner.LOGGER.debug("发送按键状态包失败: {}", e.getMessage());
        }
    }
    
    /**
     * 从客户端发送传送设置到服务端
     * 
     * @param teleportDrops 是否传送掉落物
     * @param teleportExp 是否传送经验
     */
    public static void sendTeleportSettings(boolean teleportDrops, boolean teleportExp) {
        try {
            CHANNEL.sendToServer(new TeleportSettingsPacket(teleportDrops, teleportExp));
        } catch (Exception e) {
            OneKeyMiner.LOGGER.debug("发送传送设置包失败: {}", e.getMessage());
        }
    }
}
