package org.xiyu.onekeyminer.neoforge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.mining.MiningStateManager;
import org.xiyu.onekeyminer.platform.PlatformServices;

public class NeoForgeNetworking {
    public record ChainKeyStatePayload(boolean holding, String shapeId) implements CustomPacketPayload {
        public static final ResourceLocation ID = new ResourceLocation(OneKeyMiner.MOD_ID, "chain_key_state");
        public static final FriendlyByteBuf.Reader<ChainKeyStatePayload> READER =
                buf -> new ChainKeyStatePayload(buf.readBoolean(), buf.readUtf());

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(holding);
            buf.writeUtf(shapeId == null ? "" : shapeId);
        }
    }

    public record TeleportSettingsPayload(boolean teleportDrops, boolean teleportExp) implements CustomPacketPayload {
        public static final ResourceLocation ID = new ResourceLocation(OneKeyMiner.MOD_ID, "teleport_settings");
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
        var registrar = event.registrar(OneKeyMiner.MOD_ID);
        registrar.play(ChainKeyStatePayload.ID, ChainKeyStatePayload.READER, NeoForgeNetworking::handleChainKeyState);
        registrar.play(TeleportSettingsPayload.ID, TeleportSettingsPayload.READER, NeoForgeNetworking::handleTeleportSettings);
        OneKeyMiner.LOGGER.debug("Registered NeoForge networking payloads");
    }

    public static void sendKeyState(boolean pressed, String shapeId) {
        try {
            PacketDistributor.SERVER.noArg().send(new ChainKeyStatePayload(pressed, shapeId));
        } catch (Exception e) {
            OneKeyMiner.LOGGER.debug("Failed to send NeoForge key state: {}", e.getMessage());
        }
    }

    public static void sendTeleportSettings(boolean teleportDrops, boolean teleportExp) {
        try {
            PacketDistributor.SERVER.noArg().send(new TeleportSettingsPayload(teleportDrops, teleportExp));
        } catch (Exception e) {
            OneKeyMiner.LOGGER.debug("Failed to send NeoForge teleport settings: {}", e.getMessage());
        }
    }

    private static void handleChainKeyState(ChainKeyStatePayload payload, PlayPayloadContext context) {
        context.workHandler().execute(() ->
                context.player().ifPresent(player -> {
                    if (player instanceof ServerPlayer serverPlayer) {
                        PlatformServices.getInstance().setChainModeActive(serverPlayer, payload.holding());
                        ResourceLocation shapeId = ResourceLocation.tryParse(payload.shapeId());
                        if (shapeId != null) {
                            MiningStateManager.setPlayerShape(serverPlayer, shapeId);
                        }
                    }
                })
        );
    }

    private static void handleTeleportSettings(TeleportSettingsPayload payload, PlayPayloadContext context) {
        context.workHandler().execute(() ->
                context.player().ifPresent(player -> {
                    if (player instanceof ServerPlayer serverPlayer) {
                        MiningStateManager.setTeleportDrops(serverPlayer, payload.teleportDrops());
                        MiningStateManager.setTeleportExp(serverPlayer, payload.teleportExp());
                    }
                })
        );
    }
}
