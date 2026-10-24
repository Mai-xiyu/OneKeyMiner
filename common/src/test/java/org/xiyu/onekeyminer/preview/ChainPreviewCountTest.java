package org.xiyu.onekeyminer.preview;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class ChainPreviewCountTest {
    private static final BlockPos ORIGIN = new BlockPos(0, 64, 0);

    @Test
    void isolatedTargetCountsAsOne() {
        assertEquals(Collections.singletonList(ORIGIN),
                ChainPreviewManager.includeOrigin(ORIGIN, Collections.emptyList(), 64));
    }

    @Test
    void derivedTargetsIncludeOriginalAndRespectDerivedLimit() {
        BlockPos first = ORIGIN.above();
        BlockPos second = first.above();
        assertEquals(Arrays.asList(ORIGIN, first, second),
                ChainPreviewManager.includeOrigin(ORIGIN, Arrays.asList(first, second), 64));
        assertEquals(Arrays.asList(ORIGIN, first),
                ChainPreviewManager.includeOrigin(ORIGIN, Arrays.asList(first, second), 1));
        assertEquals(Collections.singletonList(ORIGIN),
                ChainPreviewManager.includeOrigin(ORIGIN, Arrays.asList(first, second), 0));
    }

    @Test
    void customShapeOriginAndDuplicatesAreCountedOnlyOnce() {
        BlockPos neighbor = ORIGIN.above();
        List<BlockPos> result = ChainPreviewManager.includeOrigin(ORIGIN,
                Arrays.asList(ORIGIN, neighbor, neighbor, ORIGIN), 64);
        assertEquals(Arrays.asList(ORIGIN, neighbor), result);
        assertThrows(UnsupportedOperationException.class, () -> result.add(neighbor));
    }

    @Test
    void invalidResultDoesNotCreatePhantomPreview() {
        assertTrue(ChainPreviewManager.includeOrigin(ORIGIN, null, 64).isEmpty());
    }
}
