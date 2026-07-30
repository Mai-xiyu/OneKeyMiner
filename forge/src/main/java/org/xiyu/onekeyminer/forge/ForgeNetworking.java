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
import org.xiyu.onekeyminer.shape.ShapeRegistry;

/**
 * Server-safe Forge C2S networking.
 */
public final class ForgeNetworking {
    public static final int WIRE_VERSION = 2;
    public static final int MAX_SHAPE_ID_LENGTH = 128;

    private static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "main"))
            .networkProtocolVersion(WIRE_VERSION)
            .simpleChannel();
    private static boolean registered;

    private ForgeNetworking() {
    }

    public record ClientPreferencesPacket(
            int wireVersion,
            boolean holding,
            String shapeId,
            boolean teleportDrops,
            boolean teleportExp
    ) {
        public ClientPreferencesPacket {
            shapeId = shapeId != null ? shapeId : "";
        }

        public static ClientPreferencesPacket fromNetwork(FriendlyByteBuf buf) {
            return new ClientPreferencesPacket(
                    buf.readVarInt(),
                    buf.readBoolean(),
                    buf.readUtf(MAX_SHAPE_ID_LENGTH),
                    buf.readBoolean(),
                    buf.readBoolean()
            );
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(wireVersion);
            buf.writeBoolean(holding);
            buf.writeUtf(shapeId, MAX_SHAPE_ID_LENGTH);
            buf.writeBoolean(teleportDrops);
            buf.writeBoolean(teleportExp);
        }

        public static void handleOnServer(ClientPreferencesPacket packet, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                if (packet.wireVersion != WIRE_VERSION) {
                    OneKeyMiner.LOGGER.warn(
                            "Ignoring client preferences from {} with unsupported wire version {}",
                            player.getGameProfile().getName(),
                            packet.wireVersion
                    );
                    return;
                }
                ResourceLocation shapeId = ResourceLocation.tryParse(packet.shapeId);
                if (shapeId == null || !ShapeRegistry.isRegistered(shapeId)) {
                    OneKeyMiner.LOGGER.warn(
                            "Replacing invalid shape preference '{}' from {} with the server default",
                            packet.shapeId,
                            player.getGameProfile().getName()
                    );
                    shapeId = ShapeRegistry.DEFAULT_SHAPE_ID;
                    if (!ShapeRegistry.isRegistered(shapeId)) {
                        return;
                    }
                }
                MiningStateManager.updatePreferences(
                        player.getUUID(),
                        packet.holding,
                        shapeId,
                        packet.teleportDrops,
                        packet.teleportExp
                );
            });
            context.setPacketHandled(true);
        }
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.messageBuilder(ClientPreferencesPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ClientPreferencesPacket::write)
                .decoder(ClientPreferencesPacket::fromNetwork)
                .consumerNetworkThread(ClientPreferencesPacket::handleOnServer)
                .add();
    }

    public static void sendPreferences(
            Connection connection,
            boolean holding,
            String shapeId,
            boolean teleportDrops,
            boolean teleportExp
    ) {
        trySendPreferences(connection, holding, shapeId, teleportDrops, teleportExp);
    }

    public static boolean trySendPreferences(
            Connection connection,
            boolean holding,
            String shapeId,
            boolean teleportDrops,
            boolean teleportExp
    ) {
        if (connection == null) {
            return false;
        }
        try {
            if (!CHANNEL.isRemotePresent(connection)) {
                return false;
            }
            CHANNEL.send(
                    new ClientPreferencesPacket(
                            WIRE_VERSION,
                            holding,
                            shapeId,
                            teleportDrops,
                            teleportExp
                    ),
                    connection
            );
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
