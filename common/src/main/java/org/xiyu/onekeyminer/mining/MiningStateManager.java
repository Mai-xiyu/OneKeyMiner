package org.xiyu.onekeyminer.mining;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player chain mining state synchronized from clients.
 */
public class MiningStateManager {
    private static final ConcurrentHashMap<UUID, PlayerState> PLAYER_STATES = new ConcurrentHashMap<>();

    private record PlayerState(
            boolean holding,
            boolean activated,
            ResourceLocation shape,
            boolean teleportDrops,
            boolean teleportExp
    ) {
        private static final PlayerState DEFAULT = new PlayerState(false, false, null, false, false);
    }

    private static PlayerState state(UUID uuid) {
        return PLAYER_STATES.getOrDefault(uuid, PlayerState.DEFAULT);
    }

    public static boolean isHoldingKey(ServerPlayer player) {
        return state(player.getUUID()).holding();
    }

    public static void setHoldingKey(ServerPlayer player, boolean holding) {
        setHoldingKey(player.getUUID(), holding);
    }

    public static void setHoldingKey(UUID uuid, boolean holding) {
        PLAYER_STATES.compute(uuid, (ignored, oldState) -> {
            PlayerState old = oldState != null ? oldState : PlayerState.DEFAULT;
            return new PlayerState(holding, old.activated(), old.shape(), old.teleportDrops(), old.teleportExp());
        });
    }

    public static boolean isActivated(ServerPlayer player) {
        return state(player.getUUID()).activated();
    }

    public static void setActivated(ServerPlayer player, boolean activated) {
        PLAYER_STATES.compute(player.getUUID(), (ignored, oldState) -> {
            PlayerState old = oldState != null ? oldState : PlayerState.DEFAULT;
            return new PlayerState(old.holding(), activated, old.shape(), old.teleportDrops(), old.teleportExp());
        });
    }

    public static boolean toggle(ServerPlayer player) {
        PlayerState updated = PLAYER_STATES.compute(player.getUUID(), (ignored, oldState) -> {
            PlayerState old = oldState != null ? oldState : PlayerState.DEFAULT;
            return new PlayerState(
                    old.holding(),
                    !old.activated(),
                    old.shape(),
                    old.teleportDrops(),
                    old.teleportExp()
            );
        });
        return updated.activated();
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
        if (shapeId != null) {
            PLAYER_STATES.compute(uuid, (ignored, oldState) -> {
                PlayerState old = oldState != null ? oldState : PlayerState.DEFAULT;
                return new PlayerState(old.holding(), old.activated(), shapeId, old.teleportDrops(), old.teleportExp());
            });
        }
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
        PLAYER_STATES.compute(uuid, (ignored, oldState) -> {
            PlayerState old = oldState != null ? oldState : PlayerState.DEFAULT;
            return new PlayerState(old.holding(), old.activated(), old.shape(), enabled, old.teleportExp());
        });
    }

    public static void setTeleportExp(ServerPlayer player, boolean enabled) {
        setTeleportExp(player.getUUID(), enabled);
    }

    public static void setTeleportExp(UUID uuid, boolean enabled) {
        PLAYER_STATES.compute(uuid, (ignored, oldState) -> {
            PlayerState old = oldState != null ? oldState : PlayerState.DEFAULT;
            return new PlayerState(old.holding(), old.activated(), old.shape(), old.teleportDrops(), enabled);
        });
    }

    public static void clearState(ServerPlayer player) {
        clearState(player.getUUID());
    }

    public static void clearState(UUID uuid) {
        PLAYER_STATES.remove(uuid);
    }

    public static void updatePreferences(
            UUID uuid,
            boolean holding,
            ResourceLocation shapeId,
            boolean teleportDrops,
            boolean teleportExp
    ) {
        PLAYER_STATES.compute(uuid, (ignored, oldState) -> {
            PlayerState old = oldState != null ? oldState : PlayerState.DEFAULT;
            return new PlayerState(holding, old.activated(), shapeId, teleportDrops, teleportExp);
        });
    }

    public static void clearAll() {
        PLAYER_STATES.clear();
    }
}
