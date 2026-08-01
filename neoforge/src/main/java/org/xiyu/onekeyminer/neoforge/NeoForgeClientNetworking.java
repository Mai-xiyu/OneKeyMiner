package org.xiyu.onekeyminer.neoforge;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;
import org.xiyu.onekeyminer.network.ClientPreferenceRequest;

/** Physical-client NeoForge packet adapter. */
public final class NeoForgeClientNetworking {
    private NeoForgeClientNetworking() {
    }

    public static boolean trySyncPreferences(
            int sequence,
            ClientPreferenceRequest request
    ) {
        if (Minecraft.getInstance().getConnection() == null) {
            return false;
        }
        try {
            PacketDistributor.SERVER.noArg().send(
                    new NeoForgeNetworking.ClientPreferencesPayload(
                            NeoForgeNetworking.WIRE_VERSION,
                            sequence,
                            request.holding(),
                            request.shapeId(),
                            request.teleportDrops(),
                            request.teleportExp()
                    )
            );
            return true;
        } catch (RuntimeException e) {
            OneKeyMiner.LOGGER.debug(
                    "Failed to send NeoForge client preferences: {}",
                    e.getMessage()
            );
            return false;
        }
    }

    public static void handlePreferencesAck(ClientPreferenceAck ack) {
        NeoForgeKeyBindings.handlePreferencesAck(ack);
    }
}
