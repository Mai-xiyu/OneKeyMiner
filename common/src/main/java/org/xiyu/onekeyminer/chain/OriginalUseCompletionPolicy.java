package org.xiyu.onekeyminer.chain;

/**
 * Fail-closed authorization for work derived from an already completed use.
 */
final class OriginalUseCompletionPolicy {

    enum BlockRequirement {
        TRUST_RESULT,
        STATE_CHANGE,
        PLACEMENT
    }

    private OriginalUseCompletionPolicy() {
    }

    static BlockRequirement selectBlockRequirement(
            boolean planting,
            boolean customRule,
            boolean statefulNativeTool
    ) {
        if (planting) {
            return BlockRequirement.PLACEMENT;
        }
        if (customRule) {
            return BlockRequirement.TRUST_RESULT;
        }
        if (statefulNativeTool) {
            return BlockRequirement.STATE_CHANGE;
        }
        return BlockRequirement.TRUST_RESULT;
    }

    static boolean permitsDerivedBlockUse(
            BlockRequirement requirement,
            boolean stateChanged,
            boolean occupiedAfter
    ) {
        return switch (requirement) {
            case TRUST_RESULT -> true;
            case STATE_CHANGE -> stateChanged;
            case PLACEMENT -> stateChanged && occupiedAfter;
        };
    }

    static boolean permitsDerivedEntityUse(
            boolean trustResult,
            boolean nativeShearCompleted
    ) {
        return trustResult || nativeShearCompleted;
    }

    static boolean nativeShearingCompleted(
            boolean sameEntityPresent,
            boolean stillShearable,
            boolean readyAfter,
            boolean knownReplacementCompleted
    ) {
        if (!sameEntityPresent) {
            return knownReplacementCompleted;
        }
        return stillShearable && !readyAfter;
    }

    static boolean knownShearingReplacementCompleted(
            boolean originalRemoved,
            boolean replacementAlive,
            boolean replacementIsNew,
            boolean positionMatches
    ) {
        return originalRemoved
                && replacementAlive
                && replacementIsNew
                && positionMatches;
    }
}
