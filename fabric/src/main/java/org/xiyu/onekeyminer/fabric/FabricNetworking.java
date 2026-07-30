package org.xiyu.onekeyminer.fabric;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.mining.MiningStateManager;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

/**
 * Server-safe Fabric payload declarations and handlers.
 */
public final class FabricNetworking {
    public static final int WIRE_VERSION = 2;
    public static final int MAX_SHAPE_ID_LENGTH = 128;

    private FabricNetworking() {
    }

    public record ClientPreferencesPayload(
            int wireVersion,
            boolean holding,
            String shapeId,
            boolean teleportDrops,
            boolean teleportExp
    ) implements CustomPacketPayload {
        public static final ResourceLocation ID =
                ResourceLocation.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "client_preferences");
        public static final Type<ClientPreferencesPayload> TYPE = new Type<>(ID);
        public static final StreamCodec<FriendlyByteBuf, ClientPreferencesPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(payload.wireVersion);
                    buf.writeBoolean(payload.holding);
                    buf.writeUtf(payload.shapeId == null ? "" : payload.shapeId, MAX_SHAPE_ID_LENGTH);
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

    public static void register() {
        PayloadTypeRegistry.playC2S().register(
                ClientPreferencesPayload.TYPE,
                ClientPreferencesPayload.STREAM_CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                ClientPreferencesPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    if (payload.wireVersion() != WIRE_VERSION) {
                        OneKeyMiner.LOGGER.warn(
                                "Ignoring client preferences from {} with unsupported wire version {}",
                                context.player().getGameProfile().getName(),
                                payload.wireVersion()
                        );
                        return;
                    }

                    ResourceLocation shapeId = ResourceLocation.tryParse(payload.shapeId());
                    if (shapeId == null || !ShapeRegistry.isRegistered(shapeId)) {
                        OneKeyMiner.LOGGER.warn(
                                "Replacing invalid shape preference '{}' from {} with the server default",
                                payload.shapeId(),
                                context.player().getGameProfile().getName()
                        );
                        shapeId = ShapeRegistry.DEFAULT_SHAPE_ID;
                        if (!ShapeRegistry.isRegistered(shapeId)) {
                            return;
                        }
                    }

                    MiningStateManager.updatePreferences(
                            context.player().getUUID(),
                            payload.holding(),
                            shapeId,
                            payload.teleportDrops(),
                            payload.teleportExp()
                    );
                })
        );
    }
}
