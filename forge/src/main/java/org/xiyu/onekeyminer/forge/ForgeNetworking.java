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
import org.xiyu.onekeyminer.mining.MiningStateManager;
import org.xiyu.onekeyminer.platform.PlatformServices;

/**
 * Forge C2S networking.
 */
public class ForgeNetworking {
    private static final SimpleChannel CHANNEL = ChannelBuilder
            .named(Identifier.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "main"))
            .optional()
            .simpleChannel();

    private static int packetIndex = 0;
    private static boolean registered = false;

    public static class ChainKeyStatePacket {
        private final boolean pressed;
        private final String shapeId;

        public ChainKeyStatePacket(boolean pressed, String shapeId) {
            this.pressed = pressed;
            this.shapeId = shapeId != null ? shapeId : "onekeyminer:amorphous";
        }

        public static ChainKeyStatePacket fromNetwork(FriendlyByteBuf buf) {
            return new ChainKeyStatePacket(buf.readBoolean(), buf.readUtf(256));
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(pressed);
            buf.writeUtf(shapeId);
        }

        public static void handleOnServer(ChainKeyStatePacket packet, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    PlatformServices.getInstance().setChainModeActive(player, packet.pressed);
                    Identifier id = Identifier.tryParse(packet.shapeId);
                    if (id != null) {
                        MiningStateManager.setPlayerShape(player, id);
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }

    public static class TeleportSettingsPacket {
        private final boolean teleportDrops;
        private final boolean teleportExp;

        public TeleportSettingsPacket(boolean teleportDrops, boolean teleportExp) {
            this.teleportDrops = teleportDrops;
            this.teleportExp = teleportExp;
        }

        public static TeleportSettingsPacket fromNetwork(FriendlyByteBuf buf) {
            return new TeleportSettingsPacket(buf.readBoolean(), buf.readBoolean());
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(teleportDrops);
            buf.writeBoolean(teleportExp);
        }

        public static void handleOnServer(TeleportSettingsPacket packet, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    MiningStateManager.setTeleportDrops(player, packet.teleportDrops);
                    MiningStateManager.setTeleportExp(player, packet.teleportExp);
                }
            });
            context.setPacketHandled(true);
        }
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        CHANNEL.messageBuilder(ChainKeyStatePacket.class, packetIndex++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ChainKeyStatePacket::write)
                .decoder(ChainKeyStatePacket::fromNetwork)
                .consumerNetworkThread(ChainKeyStatePacket::handleOnServer)
                .add();

        CHANNEL.messageBuilder(TeleportSettingsPacket.class, packetIndex++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TeleportSettingsPacket::write)
                .decoder(TeleportSettingsPacket::fromNetwork)
                .consumerNetworkThread(TeleportSettingsPacket::handleOnServer)
                .add();
    }

    public static void sendKeyState(boolean pressed, String shapeId) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            CHANNEL.send(new ChainKeyStatePacket(pressed, shapeId), connection.getConnection());
        }
    }

    public static void sendTeleportSettings(boolean teleportDrops, boolean teleportExp) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            CHANNEL.send(new TeleportSettingsPacket(teleportDrops, teleportExp), connection.getConnection());
        }
    }
}
