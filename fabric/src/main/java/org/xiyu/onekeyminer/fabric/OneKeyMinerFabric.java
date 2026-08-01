package org.xiyu.onekeyminer.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;
import org.xiyu.onekeyminer.network.ClientPreferenceProtocol;
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
        PayloadTypeRegistry.playS2C().register(
                FabricPayloads.ServerPreferencesAckPayload.TYPE,
                FabricPayloads.ServerPreferencesAckPayload.STREAM_CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                FabricPayloads.ClientPreferencesPayload.TYPE,
                (payload, context) -> {
                    ClientPreferenceAck ack = ClientPreferenceProtocol.applyOnServer(
                            context.player(),
                            payload.wireVersion(),
                            payload.sequence(),
                            payload.holding(),
                            payload.shapeId(),
                            payload.teleportDrops(),
                            payload.teleportExp()
                    );
                    if (ack != null) {
                        ServerPlayNetworking.send(
                                context.player(),
                                new FabricPayloads.ServerPreferencesAckPayload(
                                        ack.wireVersion(),
                                        ack.sequence(),
                                        ack.appliedShapeId(),
                                        ack.teleportDropsApplied(),
                                        ack.teleportExpApplied(),
                                        ack.capabilities()
                                )
                        );
                    }
                }
        );
    }
}
