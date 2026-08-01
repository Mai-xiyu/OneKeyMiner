package org.xiyu.onekeyminer.fabric;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;
import org.xiyu.onekeyminer.network.ClientPreferenceProtocol;
import org.xiyu.onekeyminer.network.PreferenceCodecGuard;

/**
 * Server-safe Fabric payload declarations.
 */
public final class FabricPayloads {
    public static final int WIRE_VERSION = ClientPreferenceProtocol.WIRE_VERSION;
    public static final int MAX_SHAPE_ID_LENGTH =
            ClientPreferenceProtocol.MAX_SHAPE_ID_LENGTH;

    private FabricPayloads() {
    }

    /**
     * One coherent client preference snapshot. The versioned payload identifier
     * makes incompatible Fabric peers report the channel as unavailable instead
     * of decoding a different wire layout.
     */
    public record ClientPreferencesPayload(
            int wireVersion,
            int sequence,
            boolean holding,
            String shapeId,
            boolean teleportDrops,
            boolean teleportExp
    ) implements CustomPacketPayload {
        public static final ResourceLocation ID =
                ResourceLocation.fromNamespaceAndPath(
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
                        FabricPayloads::decodeClientPreferences
                );

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
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
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
                        FabricPayloads::decodeServerPreferencesAck
                );

        public static ServerPreferencesAckPayload fromCommon(ClientPreferenceAck ack) {
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

        public ClientPreferenceAck toCommon() {
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
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static ClientPreferencesPayload decodeClientPreferences(FriendlyByteBuf buffer) {
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

    private static ServerPreferencesAckPayload decodeServerPreferencesAck(
            FriendlyByteBuf buffer
    ) {
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
}
