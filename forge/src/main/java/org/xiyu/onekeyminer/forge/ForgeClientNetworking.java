package org.xiyu.onekeyminer.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

/** Client connection access kept out of the dedicated-server protocol class. */
public final class ForgeClientNetworking {

    private ForgeClientNetworking() {
    }

    public static void sendClientState(
            boolean pressed,
            String shapeId,
            boolean teleportDrops,
            boolean teleportExp
    ) {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        if (listener == null) {
            return;
        }
        ForgeNetworking.sendToServer(
                new ForgeNetworking.ChainKeyStatePacket(pressed, shapeId),
                listener.getConnection()
        );
        ForgeNetworking.sendToServer(
                new ForgeNetworking.TeleportSettingsPacket(teleportDrops, teleportExp),
                listener.getConnection()
        );
    }
}
