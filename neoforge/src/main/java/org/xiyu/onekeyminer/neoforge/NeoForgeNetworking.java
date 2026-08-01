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
import org.xiyu.onekeyminer.shape.ShapeRegistry;

/**
 * Server-safe NeoForge payload declaration and handler.
 */
public final class NeoForgeNetworking {
    public static final String PROTOCOL_VERSION = "3";
    public static final int WIRE_VERSION = ClientPreferenceProtocol.WIRE_VERSION;
    public static final int MAX_SHAPE_ID_LENGTH = ShapeRegistry.MAX_SHAPE_ID_LENGTH;

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
                        buf -> new ClientPreferencesPayload(
                                buf.readVarInt(),
                                buf.readVarInt(),
                                buf.readBoolean(),
                                buf.readUtf(MAX_SHAPE_ID_LENGTH),
                                buf.readBoolean(),
                                buf.readBoolean()
                        )
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerPreferencesAckPayload(
            int wireVersion,
            int sequence,
            String appliedShapeId,
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
                            buf.writeUtf(payload.appliedShapeId, MAX_SHAPE_ID_LENGTH);
                            buf.writeBoolean(payload.teleportDropsApplied);
                            buf.writeBoolean(payload.teleportExpApplied);
                            buf.writeVarInt(payload.capabilities);
                        },
                        buf -> new ServerPreferencesAckPayload(
                                buf.readVarInt(),
                                buf.readVarInt(),
                                buf.readUtf(MAX_SHAPE_ID_LENGTH),
                                buf.readBoolean(),
                                buf.readBoolean(),
                                buf.readVarInt()
                        )
                );

        ClientPreferenceAck toCommon() {
            return new ClientPreferenceAck(
                    wireVersion,
                    sequence,
                    appliedShapeId,
                    teleportDropsApplied,
                    teleportExpApplied,
                    capabilities
            );
        }

        static ServerPreferencesAckPayload fromCommon(ClientPreferenceAck ack) {
            return new ServerPreferencesAckPayload(
                    ack.wireVersion(),
                    ack.sequence(),
                    ack.appliedShapeId(),
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
        var registrar = event.registrar(PROTOCOL_VERSION);
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
