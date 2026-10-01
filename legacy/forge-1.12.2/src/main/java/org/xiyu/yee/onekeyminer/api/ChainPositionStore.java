package org.xiyu.yee.onekeyminer.api;


import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储每个玩家计算出的连锁方块位置（供附属模组读取）
 */
public class ChainPositionStore {
    // 使用玩家 UUID 字符串为 key，value 为对应的 BlockPos 列表
    private static final Map<String, List<BlockPos>> miningPositions = new ConcurrentHashMap<>();
    private static final Map<String, List<BlockPos>> interactionPositions = new ConcurrentHashMap<>();
    private static final Map<String, List<BlockPos>> plantingPositions = new ConcurrentHashMap<>();
    // 存储连锁剪毛相关的实体 UUID 列表（供附属模组读取）
    private static final Map<String, List<UUID>> shearingEntityUuids = new ConcurrentHashMap<>();
    // 存储玩家当前连锁模式是否开启（由服务端同步到客户端）
    private static final Map<String, Boolean> chainModeState = new ConcurrentHashMap<>();

    public static void setMiningPositions(String playerUuid, List<BlockPos> positions) {
        if (positions == null) {
            miningPositions.remove(playerUuid);
        } else {
            miningPositions.put(playerUuid, Collections.unmodifiableList(new ArrayList<>(positions)));
        }
    }

    public static List<BlockPos> getMiningPositions(String playerUuid) {
        return miningPositions.getOrDefault(playerUuid, Collections.emptyList());
    }

    public static void setInteractionPositions(String playerUuid, List<BlockPos> positions) {
        if (positions == null) {
            interactionPositions.remove(playerUuid);
        } else {
            interactionPositions.put(playerUuid, Collections.unmodifiableList(new ArrayList<>(positions)));
        }
    }

    public static List<BlockPos> getInteractionPositions(String playerUuid) {
        return interactionPositions.getOrDefault(playerUuid, Collections.emptyList());
    }

    public static void setPlantingPositions(String playerUuid, List<BlockPos> positions) {
        if (positions == null) {
            plantingPositions.remove(playerUuid);
        } else {
            plantingPositions.put(playerUuid, Collections.unmodifiableList(new ArrayList<>(positions)));
        }
    }

    public static List<BlockPos> getPlantingPositions(String playerUuid) {
        return plantingPositions.getOrDefault(playerUuid, Collections.emptyList());
    }

    public static void clearAll(String playerUuid) {
        miningPositions.remove(playerUuid);
        interactionPositions.remove(playerUuid);
        plantingPositions.remove(playerUuid);
        chainModeState.remove(playerUuid);
        shearingEntityUuids.remove(playerUuid);
    }

    // Shearing entity UUIDs API
    public static void setShearingEntityUuids(String playerUuid, List<UUID> uuids) {
        if (uuids == null) {
            shearingEntityUuids.remove(playerUuid);
        } else {
            shearingEntityUuids.put(playerUuid, Collections.unmodifiableList(new ArrayList<>(uuids)));
        }
    }

    public static List<UUID> getShearingEntityUuids(String playerUuid) {
        return shearingEntityUuids.getOrDefault(playerUuid, Collections.emptyList());
    }

    public static void setChainMode(String playerUuid, boolean active) {
        chainModeState.put(playerUuid, active);
    }

    public static boolean isChainModeActive(String playerUuid) {
        return chainModeState.getOrDefault(playerUuid, false);
    }
}
