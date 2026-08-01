package org.xiyu.onekeyminer.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;
import org.xiyu.onekeyminer.network.ClientPreferenceRequest;

/** Physical-client adapter for the side-neutral Forge channel. */
public final class ForgeClientNetworking {
    private ForgeClientNetworking() {
    }

    public static boolean trySyncPreferences(
            int sequence,
            ClientPreferenceRequest request
    ) {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        return listener != null && ForgeNetworking.trySendPreferences(
                listener.getConnection(),
                sequence,
                request.holding(),
                request.shapeId(),
                request.teleportDrops(),
                request.teleportExp()
        );
    }

    public static void handlePreferencesAck(ClientPreferenceAck ack) {
        ForgeKeyBindings.handlePreferencesAck(ack);
    }
}
