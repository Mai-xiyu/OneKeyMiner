package org.xiyu.onekeyminer.fabric;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

/**
 * Server-safe Fabric payload declarations.
 */
public final class FabricPayloads {
    public static final int WIRE_VERSION = 2;
    public static final int MAX_SHAPE_ID_LENGTH = ShapeRegistry.MAX_SHAPE_ID_LENGTH;

    private FabricPayloads() {
    }

    /**
     * One coherent client preference snapshot. The versioned payload identifier
     * makes incompatible Fabric peers report the channel as unavailable instead
     * of decoding a different wire layout.
     */
    public record ClientPreferencesPayload(
            int wireVersion,
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
}
