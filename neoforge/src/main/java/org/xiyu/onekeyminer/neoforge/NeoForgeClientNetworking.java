package org.xiyu.onekeyminer.neoforge;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/**
 * Physical-client-only NeoForge packet sender.
 */
public final class NeoForgeClientNetworking {
    private NeoForgeClientNetworking() {
    }

    public static void sendKeyState(boolean pressed, String shapeId) {
        trySendKeyState(pressed, shapeId);
    }

    public static void sendTeleportSettings(boolean teleportDrops, boolean teleportExp) {
        trySendTeleportSettings(teleportDrops, teleportExp);
    }

    public static boolean trySendKeyState(boolean pressed, String shapeId) {
        try {
            var listener = Minecraft.getInstance().getConnection();
            if (listener == null
                    || !NetworkRegistry.hasChannel(
                            listener,
                            NeoForgeNetworking.ChainKeyStatePayload.ID
                    )) {
                return false;
            }
            ClientPacketDistributor.sendToServer(
                    new NeoForgeNetworking.ChainKeyStatePayload(pressed, shapeId)
            );
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static boolean trySendTeleportSettings(boolean teleportDrops, boolean teleportExp) {
        try {
            var listener = Minecraft.getInstance().getConnection();
            if (listener == null
                    || !NetworkRegistry.hasChannel(
                            listener,
                            NeoForgeNetworking.TeleportSettingsPayload.ID
                    )) {
                return false;
            }
            ClientPacketDistributor.sendToServer(
                    new NeoForgeNetworking.TeleportSettingsPayload(teleportDrops, teleportExp)
            );
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
