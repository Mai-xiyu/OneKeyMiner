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
        if (wireVersion != ClientPreferenceProtocol.WIRE_VERSION) {
            throw new IllegalArgumentException("unsupported wire version");
        }
        if (sequence <= 0) {
            throw new IllegalArgumentException("sequence must be positive");
        }
        if (appliedShapeId.length() > ClientPreferenceProtocol.MAX_SHAPE_ID_LENGTH) {
            throw new IllegalArgumentException("appliedShapeId exceeds the wire limit");
        }
        if (maxBlocksApplied < 1
                || maxBlocksApplied > ClientPreferenceProtocol.MAX_APPLIED_BLOCKS
                || maxDistanceApplied < 1
                || maxDistanceApplied > ClientPreferenceProtocol.MAX_APPLIED_DISTANCE) {
            throw new IllegalArgumentException("applied limits are outside protocol bounds");
        }
        if ((capabilities & ~ClientPreferenceProtocol.SUPPORTED_CAPABILITIES) != 0) {
            throw new IllegalArgumentException("unsupported capability bits");
        }
    }

    public boolean supports(int capability) {
        return (capabilities & capability) == capability;
    }
}
