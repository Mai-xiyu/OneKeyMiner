package org.xiyu.onekeyminer.mining;

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
    
    // ==================== 清理 API ====================
    
    /**
     * 清除玩家的状态（玩家退出时调用）
     * 
     * @param player 玩家
     */
    public static void clearState(ServerPlayer player) {
        PLAYER_STATES.remove(player.getUUID());
        PLAYER_KEY_STATES.remove(player.getUUID());
    }
    
    /**
     * 清除所有状态（服务器关闭时调用）
     */
    public static void clearAll() {
        PLAYER_STATES.clear();
        PLAYER_KEY_STATES.clear();
    }
}
