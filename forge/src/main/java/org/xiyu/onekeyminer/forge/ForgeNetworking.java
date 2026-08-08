package org.xiyu.onekeyminer.forge;

import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.Channel.VersionTest;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.SimpleChannel;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;
import org.xiyu.onekeyminer.network.ClientPreferenceAckDispatcher;
import org.xiyu.onekeyminer.network.ClientPreferenceProtocol;
import org.xiyu.onekeyminer.network.PreferenceCodecGuard;

/**
 * Server-safe Forge C2S networking with an exact protocol match.
 */
public final class ForgeNetworking {
    public static final int WIRE_VERSION = ClientPreferenceProtocol.WIRE_VERSION;
    public static final int MAX_SHAPE_ID_LENGTH =
            ClientPreferenceProtocol.MAX_SHAPE_ID_LENGTH;

    private static final SimpleChannel CHANNEL = ChannelBuilder
            .named(Identifier.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "main"))
            .networkProtocolVersion(WIRE_VERSION)
            .clientAcceptedVersions(VersionTest.exact(WIRE_VERSION))
            .serverAcceptedVersions(VersionTest.exact(WIRE_VERSION))
            .optional()
            .simpleChannel();
    private static boolean registered;

    private ForgeNetworking() {
    }

    public record ClientPreferencesPacket(
            int wireVersion,
            int sequence,
            boolean holding,
            String shapeId,
            boolean teleportDrops,
            boolean teleportExp
    ) {
        public ClientPreferencesPacket {
            shapeId = shapeId != null ? shapeId : "";
        }

        public static ClientPreferencesPacket fromNetwork(FriendlyByteBuf buf) {
            ClientPreferencesPacket packet = new ClientPreferencesPacket(
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readBoolean(),
                    buf.readUtf(MAX_SHAPE_ID_LENGTH),
                    buf.readBoolean(),
                    buf.readBoolean()
            );
            PreferenceCodecGuard.requireFullyConsumed(buf);
            return packet;
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(wireVersion);
            buf.writeVarInt(sequence);
            buf.writeBoolean(holding);
            buf.writeUtf(shapeId, MAX_SHAPE_ID_LENGTH);
            buf.writeBoolean(teleportDrops);
            buf.writeBoolean(teleportExp);
        }

        public static void handleOnServer(
                ClientPreferencesPacket packet,
                CustomPayloadEvent.Context context
        ) {
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                ClientPreferenceAck ack = ClientPreferenceProtocol.applyOnServer(
                        player,
                        packet.wireVersion,
                        packet.sequence,
                        packet.holding,
                        packet.shapeId,
                        packet.teleportDrops,
                        packet.teleportExp
                );
                if (ack != null) {
                    CHANNEL.reply(ServerPreferencesAckPacket.fromCommon(ack), context);
                }
            });
            context.setPacketHandled(true);
        }
    }

    public record ServerPreferencesAckPacket(
            int wireVersion,
            int sequence,
            boolean serverEnabled,
            String appliedShapeId,
            int maxBlocksApplied,
            int maxDistanceApplied,
            boolean allowDiagonalApplied,
            boolean teleportDropsApplied,
            boolean teleportExpApplied,
            int capabilities
    ) {
        public ServerPreferencesAckPacket {
            appliedShapeId = appliedShapeId != null ? appliedShapeId : "";
            new ClientPreferenceAck(
                    wireVersion,
                    sequence,
                    serverEnabled,
                    appliedShapeId,
                    maxBlocksApplied,
                    maxDistanceApplied,
                    allowDiagonalApplied,
                    teleportDropsApplied,
                    teleportExpApplied,
                    capabilities
            );
        }

        static ServerPreferencesAckPacket fromCommon(ClientPreferenceAck ack) {
            return new ServerPreferencesAckPacket(
                    ack.wireVersion(),
                    ack.sequence(),
                    ack.serverEnabled(),
                    ack.appliedShapeId(),
                    ack.maxBlocksApplied(),
                    ack.maxDistanceApplied(),
                    ack.allowDiagonalApplied(),
                    ack.teleportDropsApplied(),
                    ack.teleportExpApplied(),
                    ack.capabilities()
            );
        }

        ClientPreferenceAck toCommon() {
            return new ClientPreferenceAck(
                    wireVersion,
                    sequence,
                    serverEnabled,
                    appliedShapeId,
                    maxBlocksApplied,
                    maxDistanceApplied,
                    allowDiagonalApplied,
                    teleportDropsApplied,
                    teleportExpApplied,
                    capabilities
            );
        }

        static ServerPreferencesAckPacket fromNetwork(FriendlyByteBuf buf) {
            ServerPreferencesAckPacket packet = new ServerPreferencesAckPacket(
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readBoolean(),
                    buf.readUtf(MAX_SHAPE_ID_LENGTH),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readVarInt()
            );
            PreferenceCodecGuard.requireFullyConsumed(buf);
            return packet;
        }

        void write(FriendlyByteBuf buf) {
            buf.writeVarInt(wireVersion);
            buf.writeVarInt(sequence);
            buf.writeBoolean(serverEnabled);
            buf.writeUtf(appliedShapeId, MAX_SHAPE_ID_LENGTH);
            buf.writeVarInt(maxBlocksApplied);
            buf.writeVarInt(maxDistanceApplied);
            buf.writeBoolean(allowDiagonalApplied);
            buf.writeBoolean(teleportDropsApplied);
            buf.writeBoolean(teleportExpApplied);
            buf.writeVarInt(capabilities);
        }

        static void handleOnClient(
                ServerPreferencesAckPacket packet,
                CustomPayloadEvent.Context context
        ) {
            ClientPreferenceAckDispatcher.dispatch(packet.toCommon());
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
        CHANNEL.messageBuilder(ServerPreferencesAckPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ServerPreferencesAckPacket::write)
                .decoder(ServerPreferencesAckPacket::fromNetwork)
                .consumerMainThread(ServerPreferencesAckPacket::handleOnClient)
                .add();
    }

    public static boolean trySendPreferences(
            Connection connection,
            int sequence,
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
                            sequence,
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
