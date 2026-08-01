package org.xiyu.onekeyminer.config;

import java.util.Objects;

/** Defines which settings are client-owned while connected to a remote server. */
public final class RemoteConfigPolicy {
    private RemoteConfigPolicy() {
    }

    /**
     * Returns whether a config screen may edit logical-server settings.
     * Integrated singleplayer remains locally authoritative.
     */
    public static boolean canEditServerSettings(
            boolean connected,
            boolean hasIntegratedServer
    ) {
        return !connected || hasIntegratedServer;
    }

    /**
     * Copies only preferences transported by the C2S protocol. Server-owned
     * gameplay limits and policy switches retain their current local values.
     */
    public static MinerConfig mergeClientPreferences(
            MinerConfig current,
            MinerConfig edited
    ) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(edited, "edited");
        MinerConfig merged = current.copy();
        merged.selectedShape = edited.selectedShape;
        merged.shapeMode = null;
        merged.teleportDrops = edited.teleportDrops;
        merged.teleportExp = edited.teleportExp;
        return merged;
    }
}
