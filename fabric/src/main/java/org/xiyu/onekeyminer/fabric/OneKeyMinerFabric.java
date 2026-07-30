package org.xiyu.onekeyminer.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.mining.MiningStateManager;
import org.xiyu.onekeyminer.platform.PlatformServices;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

/** Fabric 1.20.1 entrypoint and C2S receiver registration. */
public final class OneKeyMinerFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        PlatformServices.setInstance(new FabricPlatformServices());
        OneKeyMiner.init();
        FabricEventHandler.register();
        registerNetworking();
        OneKeyMiner.LOGGER.info("OneKeyMiner Fabric module initialized");
    }

    private static void registerNetworking() {
        ServerPlayNetworking.registerGlobalReceiver(
                FabricNetworkingIds.CLIENT_STATE,
                (server, player, handler, buf, responseSender) -> {
                    int wireVersion = buf.readUnsignedByte();
                    boolean holding = buf.readBoolean();
                    String shapeId = buf.readUtf(FabricNetworkingIds.MAX_SHAPE_ID_LENGTH);
                    boolean teleportDrops = buf.readBoolean();
                    boolean teleportExp = buf.readBoolean();

                    if (wireVersion != FabricNetworkingIds.WIRE_VERSION) {
                        OneKeyMiner.LOGGER.warn(
                                "Rejected Fabric state wire version {} from {}",
                                wireVersion,
                                player.getGameProfile().getName()
                        );
                        return;
                    }

                    server.execute(() -> applyClientState(
                            player,
                            holding,
                            shapeId,
                            teleportDrops,
                            teleportExp
                    ));
                }
        );

        // Legacy receivers keep rolling upgrades safe while new clients prefer
        // the versioned full-state payload above.
        ServerPlayNetworking.registerGlobalReceiver(
                FabricNetworkingIds.LEGACY_CHAIN_KEY_STATE,
                (server, player, handler, buf, responseSender) -> {
                    boolean holding = buf.readBoolean();
                    String shapeId = buf.readUtf(FabricNetworkingIds.MAX_SHAPE_ID_LENGTH);
                    server.execute(() -> {
                        PlatformServices.getInstance().setChainModeActive(player, holding);
                        applyShape(player, shapeId);
                    });
                }
        );
        ServerPlayNetworking.registerGlobalReceiver(
                FabricNetworkingIds.LEGACY_TELEPORT_SETTINGS,
                (server, player, handler, buf, responseSender) -> {
                    boolean teleportDrops = buf.readBoolean();
                    boolean teleportExp = buf.readBoolean();
                    server.execute(() -> {
                        MiningStateManager.setTeleportDrops(player, teleportDrops);
                        MiningStateManager.setTeleportExp(player, teleportExp);
                    });
                }
        );
    }

    private static void applyClientState(
            ServerPlayer player,
            boolean holding,
            String shapeId,
            boolean teleportDrops,
            boolean teleportExp
    ) {
        PlatformServices.getInstance().setChainModeActive(player, holding);
        applyShape(player, shapeId);
        MiningStateManager.setTeleportDrops(player, teleportDrops);
        MiningStateManager.setTeleportExp(player, teleportExp);
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
