package org.xiyu.onekeyminer.chain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OriginalUseCompletionPolicyTest {

    @Test
    void nativeBlockTransformRequiresAStateChange() {
        assertFalse(OriginalUseCompletionPolicy.permitsDerivedBlockUse(
                OriginalUseCompletionPolicy.BlockRequirement.STATE_CHANGE,
                false,
                true
        ));
        assertTrue(OriginalUseCompletionPolicy.permitsDerivedBlockUse(
                OriginalUseCompletionPolicy.BlockRequirement.STATE_CHANGE,
                true,
                true
        ));
    }

    @Test
    void nativePlantingRequiresAChangedOccupiedPosition() {
        assertFalse(OriginalUseCompletionPolicy.permitsDerivedBlockUse(
                OriginalUseCompletionPolicy.BlockRequirement.PLACEMENT,
                false,
                true
        ));
        assertFalse(OriginalUseCompletionPolicy.permitsDerivedBlockUse(
                OriginalUseCompletionPolicy.BlockRequirement.PLACEMENT,
                true,
                false
        ));
        assertTrue(OriginalUseCompletionPolicy.permitsDerivedBlockUse(
                OriginalUseCompletionPolicy.BlockRequirement.PLACEMENT,
                true,
                true
        ));
    }

    @Test
    void customPlantingStillRequiresARealPlacement() {
        assertSame(
                OriginalUseCompletionPolicy.BlockRequirement.PLACEMENT,
                OriginalUseCompletionPolicy.selectBlockRequirement(
                true,
                true,
                false
                )
        );
    }

    @Test
    void explicitBlockRuleMayTrustTheSuccessfulResult() {
        assertTrue(OriginalUseCompletionPolicy.permitsDerivedBlockUse(
                OriginalUseCompletionPolicy.BlockRequirement.TRUST_RESULT,
                false,
                false
        ));
    }

    @Test
    void nativeShearingRequiresTheTargetToStopBeingReady() {
        assertFalse(OriginalUseCompletionPolicy.permitsDerivedEntityUse(
                false,
                false
        ));
        assertTrue(OriginalUseCompletionPolicy.permitsDerivedEntityUse(
                false,
                true
        ));
        assertTrue(OriginalUseCompletionPolicy.permitsDerivedEntityUse(
                true,
                false
        ));
    }

    @Test
    void nativeShearingRejectsRemovedOrReplacedTargets() {
        assertFalse(OriginalUseCompletionPolicy.nativeShearingCompleted(
                false,
                false,
                false,
                false
        ));
        assertFalse(OriginalUseCompletionPolicy.nativeShearingCompleted(
                true,
                false,
                false,
                false
        ));
        assertFalse(OriginalUseCompletionPolicy.nativeShearingCompleted(
                true,
                true,
                true,
                false
        ));
        assertTrue(OriginalUseCompletionPolicy.nativeShearingCompleted(
                true,
                true,
                false,
                false
        ));
    }

    @Test
    void nativeShearingAcceptsOnlyAnExplicitKnownReplacement() {
        assertTrue(OriginalUseCompletionPolicy.nativeShearingCompleted(
                false,
                false,
                false,
                true
        ));
        assertFalse(OriginalUseCompletionPolicy.nativeShearingCompleted(
                true,
                false,
                false,
                true
        ));
    }

    @Test
    void knownReplacementRequiresANewLiveEntityAtTheOriginalPosition() {
        assertFalse(OriginalUseCompletionPolicy.knownShearingReplacementCompleted(
                false,
                true,
                true,
                true
        ));
        assertFalse(OriginalUseCompletionPolicy.knownShearingReplacementCompleted(
                true,
                false,
                true,
                true
        ));
        assertFalse(OriginalUseCompletionPolicy.knownShearingReplacementCompleted(
                true,
                true,
                false,
                true
        ));
        assertFalse(OriginalUseCompletionPolicy.knownShearingReplacementCompleted(
                true,
                true,
                true,
                false
        ));
        assertTrue(OriginalUseCompletionPolicy.knownShearingReplacementCompleted(
                true,
                true,
                true,
                true
        ));
    }
}
