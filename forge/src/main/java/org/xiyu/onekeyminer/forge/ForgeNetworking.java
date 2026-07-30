package org.xiyu.onekeyminer.forge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.mining.MiningStateManager;
import org.xiyu.onekeyminer.platform.PlatformServices;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

import java.util.Optional;
import java.util.function.Supplier;

/** Forge 1.20.1 C2S protocol. */
public final class ForgeNetworking {

    /** Change when a discriminator, packet order, or field layout changes. */
    private static final String WIRE_VERSION = "1";
    private static final int MAX_SHAPE_ID_LENGTH = 256;

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(OneKeyMiner.MOD_ID, "main"),
            () -> WIRE_VERSION,
            WIRE_VERSION::equals,
            WIRE_VERSION::equals
    );

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
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();
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
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();
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

        registered = true;
        OneKeyMiner.LOGGER.debug("Registered Forge C2S channel, wire version {}", WIRE_VERSION);
    }

    public static void sendKeyState(boolean pressed, String shapeId) {
        CHANNEL.sendToServer(new ChainKeyStatePacket(pressed, shapeId));
    }

    public static void sendTeleportSettings(boolean teleportDrops, boolean teleportExp) {
        CHANNEL.sendToServer(new TeleportSettingsPacket(teleportDrops, teleportExp));
    }

    /**
     * Sends a complete client preference snapshot using the two existing wire
     * layouts, preserving compatibility with wire version 1 peers.
     */
    public static void sendClientState(
            boolean pressed,
            String shapeId,
            boolean teleportDrops,
            boolean teleportExp
    ) {
        sendKeyState(pressed, shapeId);
        sendTeleportSettings(teleportDrops, teleportExp);
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
