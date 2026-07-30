package org.xiyu.onekeyminer.mining;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player chain mining state synchronized from clients.
 */
public class MiningStateManager {
    private static final Map<UUID, Boolean> PLAYER_KEY_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> PLAYER_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, ResourceLocation> PLAYER_SHAPES = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> PLAYER_TELEPORT_DROPS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> PLAYER_TELEPORT_EXP = new ConcurrentHashMap<>();

    public static boolean isHoldingKey(ServerPlayer player) {
        return PLAYER_KEY_STATES.getOrDefault(player.getUUID(), false);
    }

    public static void setHoldingKey(ServerPlayer player, boolean holding) {
        setHoldingKey(player.getUUID(), holding);
    }

    public static void setHoldingKey(UUID uuid, boolean holding) {
        if (holding) {
            PLAYER_KEY_STATES.put(uuid, true);
        } else {
            PLAYER_KEY_STATES.remove(uuid);
        }
    }

    public static boolean isActivated(ServerPlayer player) {
        return PLAYER_STATES.getOrDefault(player.getUUID(), false);
    }

    public static void setActivated(ServerPlayer player, boolean activated) {
        if (activated) {
            PLAYER_STATES.put(player.getUUID(), true);
        } else {
            PLAYER_STATES.remove(player.getUUID());
        }
    }

    public static boolean toggle(ServerPlayer player) {
        boolean newState = !isActivated(player);
        setActivated(player, newState);
        return newState;
    }

    public static ResourceLocation getPlayerShape(ServerPlayer player) {
        return PLAYER_SHAPES.get(player.getUUID());
    }

    public static ResourceLocation getPlayerShape(UUID uuid) {
        return PLAYER_SHAPES.get(uuid);
    }

    public static void setPlayerShape(ServerPlayer player, ResourceLocation shapeId) {
        if (shapeId != null) {
            PLAYER_SHAPES.put(player.getUUID(), shapeId);
        }
    }

    public static void setPlayerShape(UUID uuid, ResourceLocation shapeId) {
        if (shapeId != null) {
            PLAYER_SHAPES.put(uuid, shapeId);
        }
    }

    public static boolean isTeleportDrops(ServerPlayer player) {
        return PLAYER_TELEPORT_DROPS.getOrDefault(player.getUUID(), false);
    }

    public static boolean isTeleportExp(ServerPlayer player) {
        return PLAYER_TELEPORT_EXP.getOrDefault(player.getUUID(), false);
    }

    public static void setTeleportDrops(ServerPlayer player, boolean enabled) {
        setTeleportDrops(player.getUUID(), enabled);
    }

    public static void setTeleportDrops(UUID uuid, boolean enabled) {
        if (enabled) {
            PLAYER_TELEPORT_DROPS.put(uuid, true);
        } else {
            PLAYER_TELEPORT_DROPS.remove(uuid);
        }
    }

    public static void setTeleportExp(ServerPlayer player, boolean enabled) {
        setTeleportExp(player.getUUID(), enabled);
    }

    public static void setTeleportExp(UUID uuid, boolean enabled) {
        if (enabled) {
            PLAYER_TELEPORT_EXP.put(uuid, true);
        } else {
            PLAYER_TELEPORT_EXP.remove(uuid);
        }
    }

    public static void clearState(ServerPlayer player) {
        clearState(player.getUUID());
    }

    public static void clearState(UUID playerId) {
        PLAYER_STATES.remove(playerId);
        PLAYER_KEY_STATES.remove(playerId);
        PLAYER_SHAPES.remove(playerId);
        PLAYER_TELEPORT_DROPS.remove(playerId);
        PLAYER_TELEPORT_EXP.remove(playerId);
    }

    public static void clearAll() {
        PLAYER_STATES.clear();
        PLAYER_KEY_STATES.clear();
        PLAYER_SHAPES.clear();
        PLAYER_TELEPORT_DROPS.clear();
        PLAYER_TELEPORT_EXP.clear();
    }
}
