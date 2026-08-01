package org.xiyu.onekeyminer.network;

import org.xiyu.onekeyminer.config.ConfigManager;

import java.util.Objects;

/** Immutable semantic snapshot associated with one client sequence number. */
public record ClientPreferenceRequest(
        boolean holding,
        String shapeId,
        boolean teleportDrops,
        boolean teleportExp
) {
    public ClientPreferenceRequest {
        shapeId = Objects.requireNonNull(shapeId, "shapeId");
    }

    public static ClientPreferenceRequest capture(boolean holding) {
        var config = ConfigManager.getClientPreferencesSnapshot();
        return new ClientPreferenceRequest(
                holding,
                config.selectedShape(),
                config.teleportDrops(),
                config.teleportExp()
        );
    }
}
