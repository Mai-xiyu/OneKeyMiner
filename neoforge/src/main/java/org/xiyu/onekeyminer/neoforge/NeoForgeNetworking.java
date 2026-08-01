package org.xiyu.onekeyminer.neoforge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;
import org.xiyu.onekeyminer.network.ClientPreferenceAckDispatcher;
import org.xiyu.onekeyminer.network.ClientPreferenceProtocol;
import org.xiyu.onekeyminer.network.ClientPreferenceServer;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

/** Server-safe NeoForge 20.4 preference payloads. */
public final class NeoForgeNetworking {
    public static final int WIRE_VERSION = ClientPreferenceProtocol.WIRE_VERSION;
    public static final String PROTOCOL_VERSION = Integer.toString(WIRE_VERSION);
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
        public static final ResourceLocation ID = new ResourceLocation(
                OneKeyMiner.MOD_ID,
                "client_preferences_v2"
        );
        public static final FriendlyByteBuf.Reader<ClientPreferencesPayload> READER =
                ClientPreferencesPayload::read;

        public ClientPreferencesPayload {
            shapeId = shapeId != null ? shapeId : "";
        }

        private static ClientPreferencesPayload read(FriendlyByteBuf buf) {
            return new ClientPreferencesPayload(
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readBoolean(),
                    buf.readUtf(MAX_SHAPE_ID_LENGTH),
                    buf.readBoolean(),
                    buf.readBoolean()
            );
        }

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(wireVersion);
            buf.writeVarInt(sequence);
            buf.writeBoolean(holding);
            buf.writeUtf(shapeId, MAX_SHAPE_ID_LENGTH);
            buf.writeBoolean(teleportDrops);
            buf.writeBoolean(teleportExp);
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
        public static final ResourceLocation ID = new ResourceLocation(
                OneKeyMiner.MOD_ID,
                "server_preferences_ack_v2"
        );
        public static final FriendlyByteBuf.Reader<ServerPreferencesAckPayload> READER =
                ServerPreferencesAckPayload::read;

        public ServerPreferencesAckPayload {
            appliedShapeId = appliedShapeId != null ? appliedShapeId : "";
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

        private static ServerPreferencesAckPayload read(FriendlyByteBuf buf) {
            return new ServerPreferencesAckPayload(
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

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
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
    }

    public static void registerPayloadHandlers(RegisterPayloadHandlerEvent event) {
        var registrar = event.registrar(OneKeyMiner.MOD_ID)
                .versioned(PROTOCOL_VERSION);
        registrar.play(
                ClientPreferencesPayload.ID,
                ClientPreferencesPayload.READER,
                handler -> handler.server(NeoForgeNetworking::handleClientPreferences)
        );
        registrar.play(
                ServerPreferencesAckPayload.ID,
                ServerPreferencesAckPayload.READER,
                handler -> handler.client(NeoForgeNetworking::handleServerAck)
        );
    }

    private static void handleClientPreferences(
            ClientPreferencesPayload payload,
            PlayPayloadContext context
    ) {
        context.workHandler().submitAsync(() ->
                context.player().ifPresent(player -> {
                    if (!(player instanceof ServerPlayer serverPlayer)) {
                        return;
                    }
                    ClientPreferenceAck ack = ClientPreferenceServer.apply(
                            serverPlayer,
                            payload.wireVersion(),
                            payload.sequence(),
                            payload.holding(),
                            payload.shapeId(),
                            payload.teleportDrops(),
                            payload.teleportExp()
                    );
                    if (ack != null) {
                        context.replyHandler().send(
                                ServerPreferencesAckPayload.fromCommon(ack)
                        );
                    }
                })
        ).exceptionally(exception -> {
            OneKeyMiner.LOGGER.error(
                    "Failed to handle NeoForge client preferences",
                    exception
            );
            return null;
        });
    }

    private static void handleServerAck(
            ServerPreferencesAckPayload payload,
            PlayPayloadContext context
    ) {
        context.workHandler().submitAsync(() ->
                ClientPreferenceAckDispatcher.dispatch(payload.toCommon())
        ).exceptionally(exception -> {
            OneKeyMiner.LOGGER.error(
                    "Failed to handle NeoForge preference acknowledgement",
                    exception
            );
            return null;
        });
    }
}
