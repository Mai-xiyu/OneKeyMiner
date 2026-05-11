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

public class NeoForgeNetworking {
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
        var registrar = event.registrar(OneKeyMiner.MOD_ID);
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

    public static void sendKeyState(boolean pressed, String shapeId) {
        try {
            var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
            if (connection != null) {
                connection.send(new ChainKeyStatePayload(pressed, shapeId));
            }
        } catch (Exception e) {
            OneKeyMiner.LOGGER.debug("Failed to send NeoForge key state: {}", e.getMessage());
        }
    }

    public static void sendTeleportSettings(boolean teleportDrops, boolean teleportExp) {
        try {
            var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
            if (connection != null) {
                connection.send(new TeleportSettingsPayload(teleportDrops, teleportExp));
            }
        } catch (Exception e) {
            OneKeyMiner.LOGGER.debug("Failed to send NeoForge teleport settings: {}", e.getMessage());
        }
    }

    private static void handleChainKeyState(ChainKeyStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                PlatformServices.getInstance().setChainModeActive(serverPlayer, payload.holding());
                Identifier shapeId = Identifier.tryParse(payload.shapeId());
                if (shapeId != null) {
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
