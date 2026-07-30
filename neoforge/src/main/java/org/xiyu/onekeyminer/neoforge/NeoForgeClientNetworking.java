package org.xiyu.onekeyminer.neoforge;

import net.neoforged.neoforge.network.PacketDistributor;

/** Client-only NeoForge packet sender. */
public final class NeoForgeClientNetworking {

    private NeoForgeClientNetworking() {
    }

    public static void sendClientState(
            boolean pressed,
            String shapeId,
            boolean teleportDrops,
            boolean teleportExp
    ) {
        PacketDistributor.SERVER.noArg().send(
                new NeoForgeNetworking.ChainKeyStatePayload(pressed, shapeId)
        );
        PacketDistributor.SERVER.noArg().send(
                new NeoForgeNetworking.TeleportSettingsPayload(
                        teleportDrops,
                        teleportExp
                )
        );
    }
}
