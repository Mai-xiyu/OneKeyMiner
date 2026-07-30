package org.xiyu.onekeyminer.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

/**
 * Physical-client-only Forge packet sender.
 */
public final class ForgeClientNetworking {
    private ForgeClientNetworking() {
    }

    public static void sendKeyState(boolean pressed, String shapeId) {
        trySendKeyState(pressed, shapeId);
    }

    public static void sendTeleportSettings(boolean teleportDrops, boolean teleportExp) {
        trySendTeleportSettings(teleportDrops, teleportExp);
    }

    public static boolean trySendKeyState(boolean pressed, String shapeId) {
        try {
            ClientPacketListener listener = Minecraft.getInstance().getConnection();
            if (listener == null) {
                return false;
            }
            var connection = listener.getConnection();
            if (!ForgeNetworking.channel().isRemotePresent(connection)) {
                return false;
            }
            ForgeNetworking.channel().send(
                    new ForgeNetworking.ChainKeyStatePacket(pressed, shapeId),
                    connection
            );
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static boolean trySendTeleportSettings(boolean teleportDrops, boolean teleportExp) {
        try {
            ClientPacketListener listener = Minecraft.getInstance().getConnection();
            if (listener == null) {
                return false;
            }
            var connection = listener.getConnection();
            if (!ForgeNetworking.channel().isRemotePresent(connection)) {
                return false;
            }
            ForgeNetworking.channel().send(
                    new ForgeNetworking.TeleportSettingsPacket(teleportDrops, teleportExp),
                    connection
            );
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
