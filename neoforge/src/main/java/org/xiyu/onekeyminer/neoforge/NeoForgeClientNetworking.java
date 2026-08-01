package org.xiyu.onekeyminer.neoforge;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;
import org.xiyu.onekeyminer.network.ClientPreferenceRequest;

/**
 * Physical-client-only NeoForge packet sender.
 */
@OnlyIn(Dist.CLIENT)
public final class NeoForgeClientNetworking {
    private NeoForgeClientNetworking() {
    }

    public static boolean trySyncPreferences(
            int sequence,
            ClientPreferenceRequest request
    ) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null
                || !NetworkRegistry.hasChannel(
                        connection,
                        NeoForgeNetworking.ClientPreferencesPayload.ID
                )) {
            return false;
        }
        try {
            ClientPacketDistributor.sendToServer(new NeoForgeNetworking.ClientPreferencesPayload(
                    NeoForgeNetworking.WIRE_VERSION,
                    sequence,
                    request.holding(),
                    request.shapeId(),
                    request.teleportDrops(),
                    request.teleportExp()
            ));
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(
                NeoForgeNetworking.ServerPreferencesAckPayload.TYPE,
                (payload, context) -> context.enqueueWork(
                        () -> handlePreferencesAck(payload.toCommon())
                )
        );
    }

    private static void handlePreferencesAck(ClientPreferenceAck ack) {
        NeoForgeKeyBindings.handlePreferencesAck(ack);
    }
}
