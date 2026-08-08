package org.xiyu.onekeyminer.fabric;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xiyu.onekeyminer.chain.OriginalToolGuard;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FabricBreakToolSnapshotsTest {

    @BeforeAll
    static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void consumesAnIndependentPreBreakToolSnapshotOnlyOnce() {
        FabricBreakToolSnapshots snapshots = new FabricBreakToolSnapshots();
        UUID playerId = UUID.randomUUID();
        Object level = new Object();
        ItemStack original = new ItemStack(Items.DIAMOND_PICKAXE);

        snapshots.capture(playerId, level, BlockPos.ZERO, 3, original);
        original.shrink(1);

        FabricBreakToolSnapshots.Snapshot captured =
                snapshots.consume(playerId, level, BlockPos.ZERO);
        assertEquals(3, captured.selectedSlot());
        assertTrue(captured.tool().is(Items.DIAMOND_PICKAXE));
        assertEquals(1, captured.tool().getCount());
        assertNull(snapshots.consume(playerId, level, BlockPos.ZERO));
    }

    @Test
    void cancellationAndLifecycleCleanupRemovePendingSnapshots() {
        FabricBreakToolSnapshots snapshots = new FabricBreakToolSnapshots();
        UUID playerId = UUID.randomUUID();
        Object firstLevel = new Object();
        Object secondLevel = new Object();

        snapshots.capture(playerId, firstLevel, BlockPos.ZERO, 0,
                new ItemStack(Items.IRON_PICKAXE));
        snapshots.capture(playerId, secondLevel, BlockPos.ZERO, 1,
                new ItemStack(Items.IRON_PICKAXE));

        snapshots.discard(playerId, firstLevel, BlockPos.ZERO);
        assertNull(snapshots.consume(playerId, firstLevel, BlockPos.ZERO));

        snapshots.clearPlayer(playerId);
        assertNull(snapshots.consume(playerId, secondLevel, BlockPos.ZERO));
    }

    @Test
    void allowsDurabilityChangeButRejectsAConsumedTool() {
        ItemStack original = new ItemStack(Items.IRON_PICKAXE);
        ItemStack damaged = original.copy();
        damaged.setDamageValue(1);

        assertTrue(OriginalToolGuard.matchesAfterBreak(original, damaged));
        assertFalse(OriginalToolGuard.matchesAfterBreak(original, ItemStack.EMPTY));
    }
}
