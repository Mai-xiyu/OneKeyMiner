package org.xiyu.onekeyminer.fabric;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.xiyu.onekeyminer.OneKeyMiner;

/**
 * Server-safe Fabric payload definitions shared by both physical sides.
 */
public final class FabricPayloads {
    private static final int NETWORK_PROTOCOL_VERSION = 2;

    private FabricPayloads() {
    }

    public record ChainKeyState(boolean holding, String shapeId) implements CustomPacketPayload {
        public static final Identifier ID =
                Identifier.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "chain_key_state");
        public static final Type<ChainKeyState> TYPE = new Type<>(ID);
        public static final StreamCodec<FriendlyByteBuf, ChainKeyState> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(NETWORK_PROTOCOL_VERSION);
                    buf.writeBoolean(payload.holding);
                    buf.writeUtf(payload.shapeId == null ? "" : payload.shapeId, 256);
                },
                buf -> {
                    verifyProtocolVersion(buf.readVarInt());
                    return new ChainKeyState(buf.readBoolean(), buf.readUtf(256));
                }
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record TeleportSettings(boolean teleportDrops, boolean teleportExp)
            implements CustomPacketPayload {
        public static final Identifier ID =
                Identifier.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "teleport_settings");
        public static final Type<TeleportSettings> TYPE = new Type<>(ID);
        public static final StreamCodec<FriendlyByteBuf, TeleportSettings> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(NETWORK_PROTOCOL_VERSION);
                    buf.writeBoolean(payload.teleportDrops);
                    buf.writeBoolean(payload.teleportExp);
                },
                buf -> {
                    verifyProtocolVersion(buf.readVarInt());
                    return new TeleportSettings(buf.readBoolean(), buf.readBoolean());
                }
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static void verifyProtocolVersion(int version) {
        if (version != NETWORK_PROTOCOL_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported OneKeyMiner network protocol: " + version
            );
        }
    }
}
