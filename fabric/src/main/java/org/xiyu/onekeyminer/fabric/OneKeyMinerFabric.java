package org.xiyu.onekeyminer.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;
import org.xiyu.onekeyminer.network.ClientPreferenceServer;
import org.xiyu.onekeyminer.platform.PlatformServices;

/** Fabric 1.20.4 entrypoint and C2S receiver registration. */
public final class OneKeyMinerFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        PlatformServices.setInstance(new FabricPlatformServices());
        OneKeyMiner.init();
        FabricEventHandler.register();
        registerNetworking();
    }

    private static void registerNetworking() {
        ServerPlayNetworking.registerGlobalReceiver(
                FabricNetworkingIds.CLIENT_PREFERENCES,
                (server, player, handler, buf, responseSender) -> {
                    FabricPayloads.ClientPreferences payload =
                            FabricPayloads.ClientPreferences.read(buf);
                    server.execute(() -> {
                        ClientPreferenceAck ack = ClientPreferenceServer.apply(
                                player,
                                payload.wireVersion(),
                                payload.sequence(),
                                payload.holding(),
                                payload.shapeId(),
                                payload.teleportDrops(),
                                payload.teleportExp()
                        );
                        if (ack != null) {
                            var response = PacketByteBufs.create();
                            new FabricPayloads.ServerPreferencesAck(ack).write(response);
                            ServerPlayNetworking.send(
                                    player,
                                    FabricNetworkingIds.SERVER_PREFERENCES_ACK,
                                    response
                            );
                        }
                    });
                }
        );
    }
}
