package org.xiyu.onekeyminer.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.mining.MiningStateManager;
import org.xiyu.onekeyminer.shape.ShapeRegistry;
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
                FabricPayloads.ClientPreferencesPayload.TYPE,
                FabricPayloads.ClientPreferencesPayload.STREAM_CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                FabricPayloads.ClientPreferencesPayload.TYPE,
                (payload, context) -> {
                    if (payload.wireVersion() != FabricPayloads.WIRE_VERSION) {
                        OneKeyMiner.LOGGER.warn(
                                "Ignoring client preferences from {} with unsupported wire version {}",
                                context.player().getGameProfile().getName(),
                                payload.wireVersion()
                        );
                        return;
                    }
                    ResourceLocation id = ResourceLocation.tryParse(payload.shapeId());
                    if (id == null || !ShapeRegistry.isRegistered(id)) {
                        OneKeyMiner.LOGGER.warn(
                                "Replacing invalid shape preference '{}' from {} with the server default",
                                payload.shapeId(),
                                context.player().getGameProfile().getName()
                        );
                        id = ShapeRegistry.DEFAULT_SHAPE_ID;
                        if (!ShapeRegistry.isRegistered(id)) {
                            return;
                        }
                    }
                    MiningStateManager.updatePreferences(
                            context.player().getUUID(),
                            payload.holding(),
                            id,
                            payload.teleportDrops(),
                            payload.teleportExp()
                    );
                }
        );
    }
}
