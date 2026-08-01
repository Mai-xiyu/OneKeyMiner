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
        if (sequence < 1) {
            throw new IllegalArgumentException("sequence must be positive");
        }
        if (appliedShapeId.length() > ClientPreferenceProtocol.MAX_SHAPE_ID_LENGTH) {
            throw new IllegalArgumentException("appliedShapeId is too long");
        }
        if (maxBlocksApplied < 1
                || maxBlocksApplied > ClientPreferenceProtocol.MAX_APPLIED_BLOCKS) {
            throw new IllegalArgumentException("maxBlocksApplied is outside protocol bounds");
        }
        if (maxDistanceApplied < 1
                || maxDistanceApplied > ClientPreferenceProtocol.MAX_APPLIED_DISTANCE) {
            throw new IllegalArgumentException("maxDistanceApplied is outside protocol bounds");
        }
        if ((capabilities & ~ClientPreferenceProtocol.SUPPORTED_CAPABILITIES) != 0) {
            throw new IllegalArgumentException("capabilities contains unsupported bits");
        }
    }

    public boolean supports(int capability) {
        return (capabilities & capability) == capability;
    }
}
