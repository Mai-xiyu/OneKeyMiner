package org.xiyu.onekeyminer.forge;

import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.SimpleChannel;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.mining.MiningStateManager;
import org.xiyu.onekeyminer.platform.PlatformServices;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

/** Side-neutral Forge 1.20.4 C2S protocol. */
public final class ForgeNetworking {

    private static final int WIRE_VERSION = 1;
    private static final int MAX_SHAPE_ID_LENGTH = 256;

    private static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "main"))
            .networkProtocolVersion(WIRE_VERSION)
            .simpleChannel();

    private static int packetIndex;
    private static boolean registered;

    private ForgeNetworking() {
    }

    public static final class ChainKeyStatePacket {
        private final boolean pressed;
        private final String shapeId;

        public ChainKeyStatePacket(boolean pressed, String shapeId) {
            this.pressed = pressed;
            this.shapeId = sanitizeShapeId(shapeId);
        }

        public static ChainKeyStatePacket fromNetwork(FriendlyByteBuf buf) {
            return new ChainKeyStatePacket(
                    buf.readBoolean(),
                    buf.readUtf(MAX_SHAPE_ID_LENGTH)
            );
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(pressed);
            buf.writeUtf(shapeId, MAX_SHAPE_ID_LENGTH);
        }

        public static void handleOnServer(
                ChainKeyStatePacket packet,
                CustomPayloadEvent.Context context
        ) {
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                PlatformServices.getInstance().setChainModeActive(player, packet.pressed);
                applyShape(player, packet.shapeId);
            });
            context.setPacketHandled(true);
        }
    }

    public static final class TeleportSettingsPacket {
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

        public static void handleOnServer(
                TeleportSettingsPacket packet,
                CustomPayloadEvent.Context context
        ) {
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

    public static synchronized void register() {
        if (registered) {
            return;
        }

        CHANNEL.messageBuilder(
                        ChainKeyStatePacket.class,
                        packetIndex++,
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(ChainKeyStatePacket::write)
                .decoder(ChainKeyStatePacket::fromNetwork)
                .consumerNetworkThread(ChainKeyStatePacket::handleOnServer)
                .add();
        CHANNEL.messageBuilder(
                        TeleportSettingsPacket.class,
                        packetIndex++,
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(TeleportSettingsPacket::write)
                .decoder(TeleportSettingsPacket::fromNetwork)
                .consumerNetworkThread(TeleportSettingsPacket::handleOnServer)
                .add();

        registered = true;
    }

    static void sendToServer(Object packet, Connection connection) {
        CHANNEL.send(packet, connection);
    }

    private static String sanitizeShapeId(String shapeId) {
        if (shapeId == null || shapeId.length() > MAX_SHAPE_ID_LENGTH) {
            return ShapeRegistry.DEFAULT_SHAPE_ID.toString();
        }
        return shapeId;
    }

    private static void applyShape(ServerPlayer player, String shapeId) {
        ResourceLocation parsed = ResourceLocation.tryParse(shapeId);
        if (parsed != null && ShapeRegistry.isRegistered(parsed)) {
            MiningStateManager.setPlayerShape(player, parsed);
            return;
        }
        OneKeyMiner.LOGGER.warn(
                "Rejected unregistered shape id from {}: {}",
                player.getGameProfile().getName(),
                shapeId
        );
    }
}
