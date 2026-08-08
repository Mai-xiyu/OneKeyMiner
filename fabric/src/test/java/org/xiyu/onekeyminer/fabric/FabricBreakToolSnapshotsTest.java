package org.xiyu.onekeyminer.fabric;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FabricBreakToolSnapshotsTest {

    @Test
    void consumesAPreBreakToolSnapshotOnlyOnce() {
        FabricBreakToolSnapshots snapshots = new FabricBreakToolSnapshots();
        UUID playerId = UUID.randomUUID();
        Object level = new Object();

        snapshots.capture(playerId, level, BlockPos.ZERO, 3, ItemStack.EMPTY);

        FabricBreakToolSnapshots.Snapshot captured =
                snapshots.consume(playerId, level, BlockPos.ZERO);
        assertEquals(3, captured.selectedSlot());
        assertTrue(captured.tool().isEmpty());
        assertNull(snapshots.consume(playerId, level, BlockPos.ZERO));
    }

    @Test
    void cancellationAndLifecycleCleanupRemovePendingSnapshots() {
        FabricBreakToolSnapshots snapshots = new FabricBreakToolSnapshots();
        UUID playerId = UUID.randomUUID();
        Object firstLevel = new Object();
        Object secondLevel = new Object();

        snapshots.capture(playerId, firstLevel, BlockPos.ZERO, 0,
                ItemStack.EMPTY);
        snapshots.capture(playerId, secondLevel, BlockPos.ZERO, 1,
                ItemStack.EMPTY);

        snapshots.discard(playerId, firstLevel, BlockPos.ZERO);
        assertNull(snapshots.consume(playerId, firstLevel, BlockPos.ZERO));

        snapshots.clearPlayer(playerId);
        assertNull(snapshots.consume(playerId, secondLevel, BlockPos.ZERO));
    }

    @Test
    void acceptsMatchingEmptyHandsButRejectsNull() {
        assertTrue(FabricBreakToolSnapshots.matchesAfterBreak(
                ItemStack.EMPTY,
                ItemStack.EMPTY
        ));
        assertFalse(FabricBreakToolSnapshots.matchesAfterBreak(
                null,
                ItemStack.EMPTY
        ));
    }
}
