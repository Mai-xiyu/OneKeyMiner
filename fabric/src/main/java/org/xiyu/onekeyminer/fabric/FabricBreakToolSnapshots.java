package org.xiyu.onekeyminer.fabric;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Stores the tool that caused a Fabric break until the matching AFTER event. */
final class FabricBreakToolSnapshots {

    private final ConcurrentHashMap<BreakKey, Snapshot> pending =
            new ConcurrentHashMap<>();

    void capture(
            UUID playerId,
            Object levelIdentity,
            BlockPos pos,
            int selectedSlot,
            ItemStack tool
    ) {
        pending.put(
                key(playerId, levelIdentity, pos),
                new Snapshot(selectedSlot, Objects.requireNonNull(tool, "tool"))
        );
    }

    Snapshot consume(UUID playerId, Object levelIdentity, BlockPos pos) {
        return pending.remove(key(playerId, levelIdentity, pos));
    }

    void discard(UUID playerId, Object levelIdentity, BlockPos pos) {
        pending.remove(key(playerId, levelIdentity, pos));
    }

    void clearPlayer(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        pending.keySet().removeIf(key -> key.playerId().equals(playerId));
    }

    void clearAll() {
        pending.clear();
    }

    private static BreakKey key(UUID playerId, Object levelIdentity, BlockPos pos) {
        return new BreakKey(
                Objects.requireNonNull(playerId, "playerId"),
                Objects.requireNonNull(levelIdentity, "levelIdentity"),
                Objects.requireNonNull(pos, "pos").immutable()
        );
    }

    private record BreakKey(UUID playerId, Object levelIdentity, BlockPos pos) {
    }

    record Snapshot(int selectedSlot, ItemStack tool) {
        Snapshot {
            if (selectedSlot < 0 || selectedSlot > 8) {
                throw new IllegalArgumentException("selectedSlot must be in [0, 8]");
            }
            tool = Objects.requireNonNull(tool, "tool").copy();
        }
    }
}
