package org.xiyu.onekeyminer.network;

import java.util.Objects;

/** Server-authoritative result for one client preference snapshot. */
public record ClientPreferenceAck(
        int wireVersion,
        int sequence,
        boolean serverEnabled,
        String appliedShapeId,
        int maxBlocksApplied,
        int maxDistanceApplied,
        boolean allowDiagonalApplied,
        boolean teleportDropsApplied,
        boolean teleportExpApplied,
        int capabilities
) {
    public ClientPreferenceAck {
        appliedShapeId = Objects.requireNonNull(appliedShapeId, "appliedShapeId");
        if (maxBlocksApplied < 1) {
            throw new IllegalArgumentException("maxBlocksApplied must be positive");
        }
        if (maxDistanceApplied < 1) {
            throw new IllegalArgumentException("maxDistanceApplied must be positive");
        }
    }

    public boolean supports(int capability) {
        return (capabilities & capability) == capability;
    }
}
