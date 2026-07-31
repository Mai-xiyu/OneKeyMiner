package org.xiyu.onekeyminer.neoforge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.mining.MiningStateManager;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

/**
 * Server-safe NeoForge payload declaration and handler.
 */
public final class NeoForgeNetworking {
    public static final String PROTOCOL_VERSION = "2";
    public static final int WIRE_VERSION = 2;
    public static final int MAX_SHAPE_ID_LENGTH = ShapeRegistry.MAX_SHAPE_ID_LENGTH;

    private NeoForgeNetworking() {
    }

    public record ClientPreferencesPayload(
            int wireVersion,
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

    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION).playToServer(
                ClientPreferencesPayload.TYPE,
                ClientPreferencesPayload.STREAM_CODEC,
                NeoForgeNetworking::handleClientPreferences
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
        if (payload.wireVersion() != WIRE_VERSION) {
            OneKeyMiner.LOGGER.warn(
                    "Ignoring client preferences from {} with unsupported wire version {}",
                    serverPlayer.getGameProfile().name(),
                    payload.wireVersion()
            );
            return;
        }

        ResourceLocation shapeId = ResourceLocation.tryParse(payload.shapeId());
        if (shapeId == null || !ShapeRegistry.isRegistered(shapeId)) {
            OneKeyMiner.LOGGER.warn(
                    "Replacing invalid shape preference '{}' from {} with the server default",
                    payload.shapeId(),
                    serverPlayer.getGameProfile().name()
            );
            shapeId = ShapeRegistry.DEFAULT_SHAPE_ID;
            if (!ShapeRegistry.isRegistered(shapeId)) {
                return;
            }
        }

        MiningStateManager.updatePreferences(
                serverPlayer.getUUID(),
                payload.holding(),
                shapeId,
                payload.teleportDrops(),
                payload.teleportExp()
        );
    }
}
