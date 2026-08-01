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
        if (shapeId.length() > ClientPreferenceProtocol.MAX_SHAPE_ID_LENGTH) {
            throw new IllegalArgumentException("shapeId exceeds the wire limit");
        }
    }

    public static ClientPreferenceRequest capture(boolean holding) {
        ConfigManager.ClientPreferencesSnapshot config =
                ConfigManager.getClientPreferencesSnapshot();
        return new ClientPreferenceRequest(
                holding,
                config.selectedShape(),
                config.teleportDrops(),
                config.teleportExp()
        );
    }
}
