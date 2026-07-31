package org.xiyu.onekeyminer.neoforge;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.xiyu.onekeyminer.config.ConfigManager;

/**
 * Physical-client-only NeoForge packet sender.
 */
@OnlyIn(Dist.CLIENT)
public final class NeoForgeClientNetworking {
    private NeoForgeClientNetworking() {
    }

    public static boolean trySyncPreferences(boolean holding) {
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
}
