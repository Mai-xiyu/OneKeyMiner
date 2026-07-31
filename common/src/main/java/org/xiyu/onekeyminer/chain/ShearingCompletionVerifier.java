package org.xiyu.onekeyminer.chain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Verifies the synchronous entity transition produced by a shearing action.
 */
final class ShearingCompletionVerifier {

    private static final double REPLACEMENT_POSITION_TOLERANCE = 0.01D;
    private static final double REPLACEMENT_POSITION_TOLERANCE_SQR =
            REPLACEMENT_POSITION_TOLERANCE
                    * REPLACEMENT_POSITION_TOLERANCE;

    private ShearingCompletionVerifier() {
    }

    static Snapshot capture(Level level, Entity target) {
        if (!(level instanceof ServerLevel serverLevel)
                || target == null
                || target.isRemoved()
                || !target.isAlive()
                || !(target instanceof Shearable shearable)
                || !shearable.readyForShearing()) {
            return null;
        }

        Set<UUID> existingCowIds = target.getType() == EntityTypes.MOOSHROOM
                ? collectCowIds(serverLevel, target.position())
                : Set.of();
        return new Snapshot(
                target,
                target.getUUID(),
                target.position(),
                existingCowIds
        );
    }

    static boolean completed(Level level, Snapshot snapshot) {
        if (!(level instanceof ServerLevel serverLevel)
                || snapshot == null) {
            return false;
        }

        Entity current = serverLevel.getEntity(snapshot.originalEntityId());
        boolean sameEntityPresent = current == snapshot.originalEntity()
                && !current.isRemoved()
                && current.isAlive();
        boolean stillShearable = current instanceof Shearable;
        boolean readyAfter = stillShearable
                && ((Shearable) current).readyForShearing();
        boolean knownReplacementCompleted =
                hasCompletedMooshroomConversion(serverLevel, snapshot);
        return OriginalUseCompletionPolicy.nativeShearingCompleted(
                sameEntityPresent,
                stillShearable,
                readyAfter,
                knownReplacementCompleted
        );
    }

    private static boolean hasCompletedMooshroomConversion(
            ServerLevel level,
            Snapshot snapshot
    ) {
        Entity original = snapshot.originalEntity();
        boolean originalRemoved = original.getType() == EntityTypes.MOOSHROOM
                && original.isRemoved()
                && original.getRemovalReason()
                == Entity.RemovalReason.DISCARDED;
        if (!originalRemoved) {
            return false;
        }

        AABB searchBox = replacementSearchBox(snapshot.originalPosition());
        for (Entity candidate : level.getEntitiesOfClass(
                Entity.class,
                searchBox,
                entity -> entity.getType() == EntityTypes.COW
        )) {
            if (OriginalUseCompletionPolicy
                    .knownShearingReplacementCompleted(
                            true,
                            candidate.isAlive() && !candidate.isRemoved(),
                            !snapshot.existingCowIds()
                                    .contains(candidate.getUUID()),
                            candidate.position().distanceToSqr(
                                    snapshot.originalPosition()
                            ) <= REPLACEMENT_POSITION_TOLERANCE_SQR
                    )) {
                return true;
            }
        }
        return false;
    }

    private static Set<UUID> collectCowIds(
            ServerLevel level,
            Vec3 position
    ) {
        Set<UUID> result = new HashSet<>();
        for (Entity entity : level.getEntitiesOfClass(
                Entity.class,
                replacementSearchBox(position),
                candidate -> candidate.getType() == EntityTypes.COW
        )) {
            result.add(entity.getUUID());
        }
        return Set.copyOf(result);
    }

    private static AABB replacementSearchBox(Vec3 position) {
        return new AABB(
                position.x - REPLACEMENT_POSITION_TOLERANCE,
                position.y - REPLACEMENT_POSITION_TOLERANCE,
                position.z - REPLACEMENT_POSITION_TOLERANCE,
                position.x + REPLACEMENT_POSITION_TOLERANCE,
                position.y + REPLACEMENT_POSITION_TOLERANCE,
                position.z + REPLACEMENT_POSITION_TOLERANCE
        );
    }

    record Snapshot(
            Entity originalEntity,
            UUID originalEntityId,
            Vec3 originalPosition,
            Set<UUID> existingCowIds
    ) {
    }
}
