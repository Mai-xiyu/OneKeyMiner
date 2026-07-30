package org.xiyu.onekeyminer.neoforge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.mining.MiningStateManager;
import org.xiyu.onekeyminer.platform.PlatformServices;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

public class NeoForgeNetworking {
    private static final String NETWORK_PROTOCOL_VERSION = "2";

    public record ChainKeyStatePayload(boolean holding, String shapeId) implements CustomPacketPayload {
        public static final Identifier ID = Identifier.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "chain_key_state");
        public static final CustomPacketPayload.Type<ChainKeyStatePayload> TYPE = new CustomPacketPayload.Type<>(ID);
        public static final StreamCodec<FriendlyByteBuf, ChainKeyStatePayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeBoolean(payload.holding);
                    buf.writeUtf(payload.shapeId == null ? "" : payload.shapeId);
                },
                buf -> new ChainKeyStatePayload(buf.readBoolean(), buf.readUtf(256))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record TeleportSettingsPayload(boolean teleportDrops, boolean teleportExp) implements CustomPacketPayload {
        public static final Identifier ID = Identifier.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "teleport_settings");
        public static final CustomPacketPayload.Type<TeleportSettingsPayload> TYPE = new CustomPacketPayload.Type<>(ID);
        public static final StreamCodec<FriendlyByteBuf, TeleportSettingsPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeBoolean(payload.teleportDrops);
                    buf.writeBoolean(payload.teleportExp);
                },
                buf -> new TeleportSettingsPayload(buf.readBoolean(), buf.readBoolean())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(NETWORK_PROTOCOL_VERSION);
        registrar.playToServer(
                ChainKeyStatePayload.TYPE,
                ChainKeyStatePayload.STREAM_CODEC,
                NeoForgeNetworking::handleChainKeyState
        );
        registrar.playToServer(
                TeleportSettingsPayload.TYPE,
                TeleportSettingsPayload.STREAM_CODEC,
                NeoForgeNetworking::handleTeleportSettings
        );
        OneKeyMiner.LOGGER.debug("Registered NeoForge networking payloads");
    }

    private static void handleChainKeyState(ChainKeyStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                PlatformServices.getInstance().setChainModeActive(serverPlayer, payload.holding());
                Identifier shapeId = Identifier.tryParse(payload.shapeId());
                if (shapeId != null && ShapeRegistry.getShape(shapeId) != null) {
                    MiningStateManager.setPlayerShape(serverPlayer, shapeId);
                }
            }
        });
    }

    private static void handleTeleportSettings(TeleportSettingsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                MiningStateManager.setTeleportDrops(serverPlayer, payload.teleportDrops());
                MiningStateManager.setTeleportExp(serverPlayer, payload.teleportExp());
            }
        });
    }
}
