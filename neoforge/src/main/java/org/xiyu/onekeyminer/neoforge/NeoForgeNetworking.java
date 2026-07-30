package org.xiyu.onekeyminer.neoforge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.mining.MiningStateManager;
import org.xiyu.onekeyminer.platform.PlatformServices;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

/** Side-neutral NeoForge 20.4 C2S payload registration. */
public final class NeoForgeNetworking {

    private static final String WIRE_VERSION = "1";
    private static final int MAX_SHAPE_ID_LENGTH = 256;

    private NeoForgeNetworking() {
    }

    public record ChainKeyStatePayload(
            boolean holding,
            String shapeId
    ) implements CustomPacketPayload {

        public static final ResourceLocation ID =
                new ResourceLocation(OneKeyMiner.MOD_ID, "chain_key_state");
        public static final FriendlyByteBuf.Reader<ChainKeyStatePayload> READER =
                buf -> new ChainKeyStatePayload(
                        buf.readBoolean(),
                        buf.readUtf(MAX_SHAPE_ID_LENGTH)
                );

        public ChainKeyStatePayload {
            shapeId = sanitizeShapeId(shapeId);
        }

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(holding);
            buf.writeUtf(shapeId, MAX_SHAPE_ID_LENGTH);
        }
    }

    public record TeleportSettingsPayload(
            boolean teleportDrops,
            boolean teleportExp
    ) implements CustomPacketPayload {

        public static final ResourceLocation ID =
                new ResourceLocation(OneKeyMiner.MOD_ID, "teleport_settings");
        public static final FriendlyByteBuf.Reader<TeleportSettingsPayload> READER =
                buf -> new TeleportSettingsPayload(buf.readBoolean(), buf.readBoolean());

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(teleportDrops);
            buf.writeBoolean(teleportExp);
        }
    }

    public static void registerPayloadHandlers(RegisterPayloadHandlerEvent event) {
        var registrar = event.registrar(OneKeyMiner.MOD_ID).versioned(WIRE_VERSION);
        registrar.play(
                ChainKeyStatePayload.ID,
                ChainKeyStatePayload.READER,
                handler -> handler.server(NeoForgeNetworking::handleChainKeyState)
        );
        registrar.play(
                TeleportSettingsPayload.ID,
                TeleportSettingsPayload.READER,
                handler -> handler.server(NeoForgeNetworking::handleTeleportSettings)
        );
    }

    private static void handleChainKeyState(
            ChainKeyStatePayload payload,
            PlayPayloadContext context
    ) {
        context.workHandler().submitAsync(() ->
                context.player().ifPresent(player -> {
                    if (player instanceof ServerPlayer serverPlayer) {
                        PlatformServices.getInstance().setChainModeActive(
                                serverPlayer,
                                payload.holding()
                        );
                        applyShape(serverPlayer, payload.shapeId());
                    }
                })
        ).exceptionally(exception -> {
            OneKeyMiner.LOGGER.error("Failed to handle NeoForge key state", exception);
            return null;
        });
    }

    private static void handleTeleportSettings(
            TeleportSettingsPayload payload,
            PlayPayloadContext context
    ) {
        context.workHandler().submitAsync(() ->
                context.player().ifPresent(player -> {
                    if (player instanceof ServerPlayer serverPlayer) {
                        MiningStateManager.setTeleportDrops(
                                serverPlayer,
                                payload.teleportDrops()
                        );
                        MiningStateManager.setTeleportExp(
                                serverPlayer,
                                payload.teleportExp()
                        );
                    }
                })
        ).exceptionally(exception -> {
            OneKeyMiner.LOGGER.error("Failed to handle NeoForge teleport settings", exception);
            return null;
        });
    }

    private static String sanitizeShapeId(String shapeId) {
        if (shapeId == null || shapeId.length() > MAX_SHAPE_ID_LENGTH) {
            return ShapeRegistry.DEFAULT_SHAPE_ID.toString();
        }
        return shapeId;
    }

    private static void applyShape(ServerPlayer player, String shapeId) {
        ResourceLocation parsed = ResourceLocation.tryParse(shapeId);
        if (parsed != null && ShapeRegistry.isRegistered(parsed)) {
            MiningStateManager.setPlayerShape(player, parsed);
            return;
        }
        OneKeyMiner.LOGGER.warn(
                "Rejected unregistered shape id from {}: {}",
                player.getGameProfile().getName(),
                shapeId
        );
    }
}
