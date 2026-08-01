package org.xiyu.onekeyminer.mining;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.xiyu.onekeyminer.network.ClientPreferenceServer;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;

/** Atomic per-player state synchronized from the physical client. */
public final class MiningStateManager {
    private static final ConcurrentHashMap<UUID, PlayerState> PLAYER_STATES =
            new ConcurrentHashMap<>();

    private MiningStateManager() {
    }

    private record PlayerState(
            boolean holding,
            boolean activated,
            ResourceLocation shape,
            boolean teleportDrops,
            boolean teleportExp
    ) {
        private static final PlayerState DEFAULT =
                new PlayerState(false, false, null, false, false);

        private boolean isEmpty() {
            return !holding
                    && !activated
                    && shape == null
                    && !teleportDrops
                    && !teleportExp;
        }
    }

    private static PlayerState state(UUID uuid) {
        return PLAYER_STATES.getOrDefault(
                Objects.requireNonNull(uuid, "uuid"),
                PlayerState.DEFAULT
        );
    }

    private static void update(UUID uuid, UnaryOperator<PlayerState> updater) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(updater, "updater");
        PLAYER_STATES.compute(uuid, (ignored, oldState) -> {
            PlayerState updated = Objects.requireNonNull(
                    updater.apply(oldState != null ? oldState : PlayerState.DEFAULT),
                    "updated state"
            );
            return updated.isEmpty() ? null : updated;
        });
    }

    public static boolean isHoldingKey(ServerPlayer player) {
        return state(player.getUUID()).holding();
    }

    public static void setHoldingKey(ServerPlayer player, boolean holding) {
        setHoldingKey(player.getUUID(), holding);
    }

    public static void setHoldingKey(UUID uuid, boolean holding) {
        update(uuid, old -> new PlayerState(
                holding,
                old.activated(),
                old.shape(),
                old.teleportDrops(),
                old.teleportExp()
        ));
    }

    public static boolean isActivated(ServerPlayer player) {
        return state(player.getUUID()).activated();
    }

    public static void setActivated(ServerPlayer player, boolean activated) {
        update(player.getUUID(), old -> new PlayerState(
                old.holding(),
                activated,
                old.shape(),
                old.teleportDrops(),
                old.teleportExp()
        ));
    }

    public static boolean toggle(ServerPlayer player) {
        AtomicBoolean result = new AtomicBoolean();
        update(player.getUUID(), old -> {
            boolean activated = !old.activated();
            result.set(activated);
            return new PlayerState(
                    old.holding(),
                    activated,
                    old.shape(),
                    old.teleportDrops(),
                    old.teleportExp()
            );
        });
        return result.get();
    }

    public static ResourceLocation getPlayerShape(ServerPlayer player) {
        return getPlayerShape(player.getUUID());
    }

    public static ResourceLocation getPlayerShape(UUID uuid) {
        return state(uuid).shape();
    }

    public static void setPlayerShape(ServerPlayer player, ResourceLocation shapeId) {
        setPlayerShape(player.getUUID(), shapeId);
    }

    public static void setPlayerShape(UUID uuid, ResourceLocation shapeId) {
        if (shapeId == null) {
            return;
        }
        update(uuid, old -> new PlayerState(
                old.holding(),
                old.activated(),
                shapeId,
                old.teleportDrops(),
                old.teleportExp()
        ));
    }

    public static boolean isTeleportDrops(ServerPlayer player) {
        return state(player.getUUID()).teleportDrops();
    }

    public static boolean isTeleportExp(ServerPlayer player) {
        return state(player.getUUID()).teleportExp();
    }

    public static void setTeleportDrops(ServerPlayer player, boolean enabled) {
        setTeleportDrops(player.getUUID(), enabled);
    }

    public static void setTeleportDrops(UUID uuid, boolean enabled) {
        update(uuid, old -> new PlayerState(
                old.holding(),
                old.activated(),
                old.shape(),
                enabled,
                old.teleportExp()
        ));
    }

    public static void setTeleportExp(ServerPlayer player, boolean enabled) {
        setTeleportExp(player.getUUID(), enabled);
    }

    public static void setTeleportExp(UUID uuid, boolean enabled) {
        update(uuid, old -> new PlayerState(
                old.holding(),
                old.activated(),
                old.shape(),
                old.teleportDrops(),
                enabled
        ));
    }

    /** Applies one validated client preference snapshot atomically. */
    public static void updatePreferences(
            UUID uuid,
            boolean holding,
            ResourceLocation shapeId,
            boolean teleportDrops,
            boolean teleportExp
    ) {
        update(uuid, old -> new PlayerState(
                holding,
                old.activated(),
                shapeId != null ? shapeId : old.shape(),
                teleportDrops,
                teleportExp
        ));
    }

    public static void clearState(ServerPlayer player) {
        clearState(player.getUUID());
    }

    public static void clearState(UUID uuid) {
        UUID playerId = Objects.requireNonNull(uuid, "uuid");
        PLAYER_STATES.remove(playerId);
        ClientPreferenceServer.clearPlayer(playerId);
    }

    public static void clearAll() {
        PLAYER_STATES.clear();
        ClientPreferenceServer.clearAll();
    }
}
