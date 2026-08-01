package org.xiyu.onekeyminer.forge;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;
import org.xiyu.onekeyminer.network.ClientPreferenceRequest;

/**
 * Physical-client-only Forge packet sender.
 */
@OnlyIn(Dist.CLIENT)
public final class ForgeClientNetworking {
    private ForgeClientNetworking() {
    }

    public static boolean trySyncPreferences(
            int sequence,
            ClientPreferenceRequest request
    ) {
        var listener = Minecraft.getInstance().getConnection();
        if (listener == null) {
            return false;
        }
        return ForgeNetworking.trySendPreferences(
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
