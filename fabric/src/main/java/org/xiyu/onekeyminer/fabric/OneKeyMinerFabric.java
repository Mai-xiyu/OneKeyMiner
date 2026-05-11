package org.xiyu.onekeyminer.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.mining.MiningStateManager;
import org.xiyu.onekeyminer.platform.PlatformServices;

/**
 * Fabric common entry point.
 */
public class OneKeyMinerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PlatformServices.setInstance(new FabricPlatformServices());
        OneKeyMiner.init();
        FabricEventHandler.register();
        registerNetworking();
        OneKeyMiner.LOGGER.info("OneKeyMiner Fabric initialized");
    }

    private void registerNetworking() {
        ServerPlayNetworking.registerGlobalReceiver(
                FabricNetworkingIds.CHAIN_KEY_STATE_ID,
                (server, player, handler, buf, responseSender) -> {
                    boolean holding = buf.readBoolean();
                    String shapeId = buf.readUtf(256);
                    server.execute(() -> {
                        PlatformServices.getInstance().setChainModeActive(player, holding);
                        ResourceLocation id = ResourceLocation.tryParse(shapeId);
                        if (id != null) {
                            MiningStateManager.setPlayerShape(player, id);
                        }
                    });
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                FabricNetworkingIds.TELEPORT_SETTINGS_ID,
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
}
