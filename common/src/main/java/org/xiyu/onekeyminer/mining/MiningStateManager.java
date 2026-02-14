package org.xiyu.onekeyminer.mining;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 挖矿状态管理器
 * 
 * <p>管理玩家的连锁挖矿激活状态。
 * 当激活模式设置为 KEYBIND_HOLD 时，使用此管理器跟踪玩家的按键状态。</p>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.0
 * @since Minecraft 1.21.9
 */
public class MiningStateManager {
    
    /** 玩家按键按住状态映射（线程安全）- 用于 KEYBIND_HOLD 模式 */
    private static final Map<UUID, Boolean> PLAYER_KEY_STATES = new ConcurrentHashMap<>();
    
    /** 玩家激活状态映射（线程安全）- 兼容旧版 toggle 模式 */
    private static final Map<UUID, Boolean> PLAYER_STATES = new ConcurrentHashMap<>();
    
    /** 玩家选择的形状 ID 映射（线程安全）- 由客户端网络包同步 */
    private static final Map<UUID, ResourceLocation> PLAYER_SHAPES = new ConcurrentHashMap<>();
    
    /** 玩家传送掉落物设置（线程安全）- 由客户端网络包同步 */
    private static final Map<UUID, Boolean> PLAYER_TELEPORT_DROPS = new ConcurrentHashMap<>();
    
    /** 玩家传送经验设置（线程安全）- 由客户端网络包同步 */
    private static final Map<UUID, Boolean> PLAYER_TELEPORT_EXP = new ConcurrentHashMap<>();
    
    // ==================== 按住模式 API ====================
    
    /**
     * 检查玩家是否正在按住连锁按键
     * 
     * @param player 玩家
     * @return 如果玩家正在按住按键返回 true
     */
    public static boolean isHoldingKey(ServerPlayer player) {
        return PLAYER_KEY_STATES.getOrDefault(player.getUUID(), false);
    }
    
    /**
     * 设置玩家的按键按住状态
     * <p>由客户端按键事件触发，通过网络包同步到服务端</p>
     * 
     * @param player 玩家
     * @param holding 是否正在按住
     */
    public static void setHoldingKey(ServerPlayer player, boolean holding) {
        PLAYER_KEY_STATES.put(player.getUUID(), holding);
    }
    
    /**
     * 通过 UUID 设置玩家的按键按住状态
     * <p>用于网络包处理</p>
     * 
     * @param uuid 玩家UUID
     * @param holding 是否正在按住
     */
    public static void setHoldingKey(UUID uuid, boolean holding) {
        PLAYER_KEY_STATES.put(uuid, holding);
    }
    
    // ==================== 兼容 API ====================
    
    /**
     * 检查玩家是否激活了连锁挖矿（兼容旧版）
     * 
     * @param player 玩家
     * @return 如果玩家已激活返回 true
     */
    public static boolean isActivated(ServerPlayer player) {
        return PLAYER_STATES.getOrDefault(player.getUUID(), false);
    }
    
    /**
     * 设置玩家的激活状态（兼容旧版）
     * 
     * @param player 玩家
     * @param activated 是否激活
     */
    public static void setActivated(ServerPlayer player, boolean activated) {
        PLAYER_STATES.put(player.getUUID(), activated);
    }
    
    /**
     * 切换玩家的激活状态（兼容旧版）
     * 
     * @param player 玩家
     * @return 切换后的状态
     */
    public static boolean toggle(ServerPlayer player) {
        boolean newState = !isActivated(player);
        setActivated(player, newState);
        return newState;
    }
    
    // ==================== 形状 API ====================
    
    /**
     * 获取玩家当前选择的形状 ID
     * 
     * @param player 玩家
     * @return 形状 ID，如果未设置返回 null
     */
    public static ResourceLocation getPlayerShape(ServerPlayer player) {
        return PLAYER_SHAPES.get(player.getUUID());
    }
    
    /**
     * 通过 UUID 获取玩家当前选择的形状 ID
     * 
     * @param uuid 玩家 UUID
     * @return 形状 ID，如果未设置返回 null
     */
    public static ResourceLocation getPlayerShape(UUID uuid) {
        return PLAYER_SHAPES.get(uuid);
    }
    
    /**
     * 设置玩家的形状选择
     * 
     * @param player 玩家
     * @param shapeId 形状 ID
     */
    public static void setPlayerShape(ServerPlayer player, ResourceLocation shapeId) {
        if (shapeId != null) {
            PLAYER_SHAPES.put(player.getUUID(), shapeId);
        }
    }
    
    /**
     * 通过 UUID 设置玩家的形状选择
     * 
     * @param uuid 玩家 UUID
     * @param shapeId 形状 ID
     */
    public static void setPlayerShape(UUID uuid, ResourceLocation shapeId) {
        if (shapeId != null) {
            PLAYER_SHAPES.put(uuid, shapeId);
        }
    }
    
    // ==================== 传送设置 API ====================
    
    /**
     * 获取玩家是否启用传送掉落物
     * 
     * @param player 玩家
     * @return 如果玩家启用了传送掉落物返回 true
     */
    public static boolean isTeleportDrops(ServerPlayer player) {
        return PLAYER_TELEPORT_DROPS.getOrDefault(player.getUUID(), false);
    }
    
    /**
     * 获取玩家是否启用传送经验
     * 
     * @param player 玩家
     * @return 如果玩家启用了传送经验返回 true
     */
    public static boolean isTeleportExp(ServerPlayer player) {
        return PLAYER_TELEPORT_EXP.getOrDefault(player.getUUID(), false);
    }
    
    /**
     * 设置玩家的传送掉落物状态
     * 
     * @param player 玩家
     * @param enabled 是否启用
     */
    public static void setTeleportDrops(ServerPlayer player, boolean enabled) {
        PLAYER_TELEPORT_DROPS.put(player.getUUID(), enabled);
    }
    
    /**
     * 通过 UUID 设置玩家的传送掉落物状态
     * 
     * @param uuid 玩家 UUID
     * @param enabled 是否启用
     */
    public static void setTeleportDrops(UUID uuid, boolean enabled) {
        PLAYER_TELEPORT_DROPS.put(uuid, enabled);
    }
    
    /**
     * 设置玩家的传送经验状态
     * 
     * @param player 玩家
     * @param enabled 是否启用
     */
    public static void setTeleportExp(ServerPlayer player, boolean enabled) {
        PLAYER_TELEPORT_EXP.put(player.getUUID(), enabled);
    }
    
    /**
     * 通过 UUID 设置玩家的传送经验状态
     * 
     * @param uuid 玩家 UUID
     * @param enabled 是否启用
     */
    public static void setTeleportExp(UUID uuid, boolean enabled) {
        PLAYER_TELEPORT_EXP.put(uuid, enabled);
    }
    
    // ==================== 清理 API ====================
    
    /**
     * 清除玩家的状态（玩家退出时调用）
     * 
     * @param player 玩家
     */
    public static void clearState(ServerPlayer player) {
        PLAYER_STATES.remove(player.getUUID());
        PLAYER_KEY_STATES.remove(player.getUUID());
        PLAYER_SHAPES.remove(player.getUUID());
        PLAYER_TELEPORT_DROPS.remove(player.getUUID());
        PLAYER_TELEPORT_EXP.remove(player.getUUID());
    }
    
    /**
     * 清除所有状态（服务器关闭时调用）
     */
    public static void clearAll() {
        PLAYER_STATES.clear();
        PLAYER_KEY_STATES.clear();
        PLAYER_SHAPES.clear();
        PLAYER_TELEPORT_DROPS.clear();
        PLAYER_TELEPORT_EXP.clear();
    }
}
