package org.xiyu.onekeyminer.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;
import org.xiyu.onekeyminer.network.ClientPreferenceServer;
import org.xiyu.onekeyminer.platform.PlatformServices;

/** Fabric 1.20.1 entrypoint and versioned preference receiver. */
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
                FabricNetworkingIds.CLIENT_PREFERENCES,
                (server, player, handler, buffer, responseSender) -> {
                    final FabricPreferenceCodec.WireRequest request;
                    try {
                        request = FabricPreferenceCodec.readRequest(buffer);
                    } catch (RuntimeException exception) {
                        OneKeyMiner.LOGGER.warn(
                                "Rejected malformed Fabric preference packet from {}",
                                player.getGameProfile().getName()
                        );
                        return;
                    }

                    server.execute(() -> {
                        ClientPreferenceAck ack = ClientPreferenceServer.apply(
                                player,
                                request.wireVersion(),
                                request.sequence(),
                                request.holding(),
                                request.shapeId(),
                                request.teleportDrops(),
                                request.teleportExp()
                        );
                        if (ack == null || !ServerPlayNetworking.canSend(
                                player,
                                FabricNetworkingIds.SERVER_PREFERENCES_ACK
                        )) {
                            return;
                        }
                        FriendlyByteBuf response = PacketByteBufs.create();
                        FabricPreferenceCodec.writeAck(response, ack);
                        ServerPlayNetworking.send(
                                player,
                                FabricNetworkingIds.SERVER_PREFERENCES_ACK,
                                response
                        );
                    });
                }
        );
    }

}
