package org.xiyu.onekeyminer.config;

import java.util.Objects;

/** Defines what a config screen may persist while connected to a remote server. */
public final class RemoteConfigPolicy {
    private RemoteConfigPolicy() {
    }

    public static boolean canEditServerSettings(boolean connected, boolean hasIntegratedServer) {
        return !connected || hasIntegratedServer;
    }

    public static MinerConfig mergeClientPreferences(MinerConfig current, MinerConfig edited) {
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
