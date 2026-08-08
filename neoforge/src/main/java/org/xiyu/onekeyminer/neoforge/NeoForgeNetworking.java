package org.xiyu.onekeyminer.neoforge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;
import org.xiyu.onekeyminer.network.ClientPreferenceProtocol;
import org.xiyu.onekeyminer.network.PreferenceCodecGuard;

/**
 * Server-safe NeoForge payload declaration and handler.
 */
public final class NeoForgeNetworking {
    public static final String PROTOCOL_VERSION =
            Integer.toString(ClientPreferenceProtocol.WIRE_VERSION);
    public static final int WIRE_VERSION = ClientPreferenceProtocol.WIRE_VERSION;
    public static final int MAX_SHAPE_ID_LENGTH =
            ClientPreferenceProtocol.MAX_SHAPE_ID_LENGTH;

    private NeoForgeNetworking() {
    }

    public record ClientPreferencesPayload(
            int wireVersion,
            int sequence,
            boolean holding,
            String shapeId,
            boolean teleportDrops,
            boolean teleportExp
    ) implements CustomPacketPayload {
        public static final Identifier ID =
                Identifier.fromNamespaceAndPath(
                        OneKeyMiner.MOD_ID,
                        "client_preferences_v" + WIRE_VERSION
                );
        public static final Type<ClientPreferencesPayload> TYPE = new Type<>(ID);
        public static final StreamCodec<FriendlyByteBuf, ClientPreferencesPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeVarInt(payload.wireVersion);
                            buf.writeVarInt(payload.sequence);
                            buf.writeBoolean(payload.holding);
                            buf.writeUtf(
                                    payload.shapeId == null ? "" : payload.shapeId,
                                    MAX_SHAPE_ID_LENGTH
                            );
                            buf.writeBoolean(payload.teleportDrops);
                            buf.writeBoolean(payload.teleportExp);
                        },
                        ClientPreferencesPayload::decode
                );

        private static ClientPreferencesPayload decode(FriendlyByteBuf buffer) {
            ClientPreferencesPayload payload = new ClientPreferencesPayload(
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readBoolean(),
                    buffer.readUtf(MAX_SHAPE_ID_LENGTH),
                    buffer.readBoolean(),
                    buffer.readBoolean()
            );
            PreferenceCodecGuard.requireFullyConsumed(buffer);
            return payload;
        }

        public ClientPreferencesPayload {
            shapeId = shapeId == null ? "" : shapeId;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerPreferencesAckPayload(
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
    ) implements CustomPacketPayload {
        public static final Identifier ID = Identifier.fromNamespaceAndPath(
                OneKeyMiner.MOD_ID,
                "server_preferences_ack_v" + WIRE_VERSION
        );
        public static final Type<ServerPreferencesAckPayload> TYPE = new Type<>(ID);
        public static final StreamCodec<FriendlyByteBuf, ServerPreferencesAckPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeVarInt(payload.wireVersion);
                            buf.writeVarInt(payload.sequence);
                            buf.writeBoolean(payload.serverEnabled);
                            buf.writeUtf(payload.appliedShapeId, MAX_SHAPE_ID_LENGTH);
                            buf.writeVarInt(payload.maxBlocksApplied);
                            buf.writeVarInt(payload.maxDistanceApplied);
                            buf.writeBoolean(payload.allowDiagonalApplied);
                            buf.writeBoolean(payload.teleportDropsApplied);
                            buf.writeBoolean(payload.teleportExpApplied);
                            buf.writeVarInt(payload.capabilities);
                        },
                        ServerPreferencesAckPayload::decode
                );

        private static ServerPreferencesAckPayload decode(FriendlyByteBuf buffer) {
            ServerPreferencesAckPayload payload = new ServerPreferencesAckPayload(
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
            PreferenceCodecGuard.requireFullyConsumed(buffer);
            return payload;
        }

        public ServerPreferencesAckPayload {
            appliedShapeId = appliedShapeId == null ? "" : appliedShapeId;
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

        static ServerPreferencesAckPayload fromCommon(ClientPreferenceAck ack) {
            return new ServerPreferencesAckPayload(
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

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL_VERSION).optional();
        registrar.playToServer(
                ClientPreferencesPayload.TYPE,
                ClientPreferencesPayload.STREAM_CODEC,
                NeoForgeNetworking::handleClientPreferences
        );
        registrar.playToClient(
                ServerPreferencesAckPayload.TYPE,
                ServerPreferencesAckPayload.STREAM_CODEC
        );
        OneKeyMiner.LOGGER.debug("Registered NeoForge networking payloads");
    }

    private static void handleClientPreferences(
            ClientPreferencesPayload payload,
            IPayloadContext context
    ) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ClientPreferenceAck ack = ClientPreferenceProtocol.applyOnServer(
                serverPlayer,
                payload.wireVersion(),
                payload.sequence(),
                payload.holding(),
                payload.shapeId(),
                payload.teleportDrops(),
                payload.teleportExp()
        );
        if (ack != null) {
            PacketDistributor.sendToPlayer(serverPlayer, ServerPreferencesAckPayload.fromCommon(ack));
        }
    }
}
