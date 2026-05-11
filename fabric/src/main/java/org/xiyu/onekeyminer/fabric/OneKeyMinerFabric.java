package org.xiyu.onekeyminer.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
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
        PayloadTypeRegistry.playC2S().register(
                KeyBindings.ChainKeyStatePayload.TYPE,
                KeyBindings.ChainKeyStatePayload.STREAM_CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                KeyBindings.TeleportSettingsPayload.TYPE,
                KeyBindings.TeleportSettingsPayload.STREAM_CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                KeyBindings.ChainKeyStatePayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    if (context.player() != null) {
                        PlatformServices.getInstance().setChainModeActive(context.player(), payload.holding());
                        Identifier id = Identifier.tryParse(payload.shapeId());
                        if (id != null) {
                            MiningStateManager.setPlayerShape(context.player(), id);
                        }
                    }
                })
        );

        ServerPlayNetworking.registerGlobalReceiver(
                KeyBindings.TeleportSettingsPayload.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    if (context.player() != null) {
                        MiningStateManager.setTeleportDrops(context.player(), payload.teleportDrops());
                        MiningStateManager.setTeleportExp(context.player(), payload.teleportExp());
                    }
                })
        );
    }
}
