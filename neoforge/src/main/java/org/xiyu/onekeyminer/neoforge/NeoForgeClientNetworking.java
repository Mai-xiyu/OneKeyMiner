package org.xiyu.onekeyminer.neoforge;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;

/**
 * Physical-client-only NeoForge packet sender.
 */
public final class NeoForgeClientNetworking {
    private NeoForgeClientNetworking() {
    }

    public static boolean trySyncPreferences(int sequence, boolean holding) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null
                || !NetworkRegistry.hasChannel(
                        connection,
                        NeoForgeNetworking.ClientPreferencesPayload.ID
                )) {
            return false;
        }

        var config = ConfigManager.getClientPreferencesSnapshot();
        try {
            ClientPacketDistributor.sendToServer(new NeoForgeNetworking.ClientPreferencesPayload(
                    NeoForgeNetworking.WIRE_VERSION,
                    sequence,
                    holding,
                    config.selectedShape(),
                    config.teleportDrops(),
                    config.teleportExp()
            ));
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(
                NeoForgeNetworking.ServerPreferencesAckPayload.TYPE,
                (payload, context) -> handlePreferencesAck(payload.toCommon())
        );
    }

    private static void handlePreferencesAck(ClientPreferenceAck ack) {
        NeoForgeKeyBindings.handlePreferencesAck(ack);
    }
}
