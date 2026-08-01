package org.xiyu.onekeyminer.forge;

import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;
import org.xiyu.onekeyminer.network.ClientPreferenceAckDispatcher;
import org.xiyu.onekeyminer.network.ClientPreferenceProtocol;
import org.xiyu.onekeyminer.network.ClientPreferenceServer;

import java.util.Optional;
import java.util.function.Supplier;

/** Forge 1.20.1 optional SimpleChannel carrying the atomic v3 protocol. */
public final class ForgeNetworking {
    public static final int WIRE_VERSION = ClientPreferenceProtocol.WIRE_VERSION;
    private static final String PROTOCOL_VERSION = Integer.toString(WIRE_VERSION);
    private static final int MAX_SHAPE_ID_LENGTH = ClientPreferenceProtocol.MAX_SHAPE_ID_LENGTH;

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(OneKeyMiner.MOD_ID, "preferences_v3"),
            () -> PROTOCOL_VERSION,
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION)
    );

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
            if (shapeId == null || shapeId.length() > MAX_SHAPE_ID_LENGTH) {
                throw new IllegalArgumentException("invalid shape id length");
            }
        }

        static ClientPreferencesPacket fromNetwork(FriendlyByteBuf buffer) {
            ClientPreferencesPacket packet = new ClientPreferencesPacket(
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readBoolean(),
                    buffer.readUtf(MAX_SHAPE_ID_LENGTH),
                    buffer.readBoolean(),
                    buffer.readBoolean()
            );
            ForgePreferenceCodec.requireFullyConsumed(buffer);
            return packet;
        }

        void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(wireVersion);
            buffer.writeVarInt(sequence);
            buffer.writeBoolean(holding);
            buffer.writeUtf(shapeId, MAX_SHAPE_ID_LENGTH);
            buffer.writeBoolean(teleportDrops);
            buffer.writeBoolean(teleportExp);
        }

        static void handle(
                ClientPreferencesPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                ClientPreferenceAck ack = ClientPreferenceServer.apply(
                        player,
                        packet.wireVersion,
                        packet.sequence,
                        packet.holding,
                        packet.shapeId,
                        packet.teleportDrops,
                        packet.teleportExp
                );
                if (ack != null && CHANNEL.isRemotePresent(player.connection.connection)) {
                    CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            ServerPreferencesAckPacket.fromCommon(ack)
                    );
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
            if (appliedShapeId == null || appliedShapeId.length() > MAX_SHAPE_ID_LENGTH) {
                throw new IllegalArgumentException("invalid applied shape id length");
            }
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

        static ServerPreferencesAckPacket fromNetwork(FriendlyByteBuf buffer) {
            ServerPreferencesAckPacket packet = new ServerPreferencesAckPacket(
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readBoolean(),
                    buffer.readUtf(MAX_SHAPE_ID_LENGTH),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readVarInt()
            );
            ForgePreferenceCodec.requireFullyConsumed(buffer);
            return packet;
        }

        void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(wireVersion);
            buffer.writeVarInt(sequence);
            buffer.writeBoolean(serverEnabled);
            buffer.writeUtf(appliedShapeId, MAX_SHAPE_ID_LENGTH);
            buffer.writeVarInt(maxBlocksApplied);
            buffer.writeVarInt(maxDistanceApplied);
            buffer.writeBoolean(allowDiagonalApplied);
            buffer.writeBoolean(teleportDropsApplied);
            buffer.writeBoolean(teleportExpApplied);
            buffer.writeVarInt(capabilities);
        }

        static void handle(
                ServerPreferencesAckPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> ClientPreferenceAckDispatcher.dispatch(packet.toCommon()));
            context.setPacketHandled(true);
        }
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        CHANNEL.registerMessage(
                0,
                ClientPreferencesPacket.class,
                ClientPreferencesPacket::write,
                ClientPreferencesPacket::fromNetwork,
                ClientPreferencesPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                1,
                ServerPreferencesAckPacket.class,
                ServerPreferencesAckPacket::write,
                ServerPreferencesAckPacket::fromNetwork,
                ServerPreferencesAckPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        registered = true;
    }

    /** Returns false for a vanilla/unmodded server so the client can retry safely. */
    public static boolean trySendClientPreferences(
            Connection connection,
            int sequence,
            boolean holding,
            String shapeId,
            boolean teleportDrops,
            boolean teleportExp
    ) {
        if (connection == null || !CHANNEL.isRemotePresent(connection)) {
            return false;
        }
        try {
            CHANNEL.sendToServer(new ClientPreferencesPacket(
                    WIRE_VERSION,
                    sequence,
                    holding,
                    shapeId,
                    teleportDrops,
                    teleportExp
            ));
            return true;
        } catch (RuntimeException exception) {
            OneKeyMiner.LOGGER.debug("Failed to send Forge preferences", exception);
            return false;
        }
    }
}
