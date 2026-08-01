package org.xiyu.onekeyminer.network;

import java.util.Objects;

/** Server-authoritative result for one client preference snapshot. */
public record ClientPreferenceAck(
        int wireVersion,
        int sequence,
        String appliedShapeId,
        boolean teleportDropsApplied,
        boolean teleportExpApplied,
        int capabilities
) {
    public ClientPreferenceAck {
        appliedShapeId = Objects.requireNonNull(appliedShapeId, "appliedShapeId");
    }
}
