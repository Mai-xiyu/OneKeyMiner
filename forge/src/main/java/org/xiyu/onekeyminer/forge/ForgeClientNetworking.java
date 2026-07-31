package org.xiyu.onekeyminer.forge;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.xiyu.onekeyminer.config.ConfigManager;

/**
 * Physical-client-only Forge packet sender.
 */
@OnlyIn(Dist.CLIENT)
public final class ForgeClientNetworking {
    private ForgeClientNetworking() {
    }

    public static boolean trySyncPreferences(boolean holding) {
        var listener = Minecraft.getInstance().getConnection();
        if (listener == null) {
            return false;
        }
        var config = ConfigManager.getClientPreferencesSnapshot();
        return ForgeNetworking.trySendPreferences(
                listener.getConnection(),
                holding,
                config.selectedShape(),
                config.teleportDrops(),
                config.teleportExp()
        );
    }
}
