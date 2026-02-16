package org.xiyu.onekeyminer.config;

import org.xiyu.onekeyminer.OneKeyMiner;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 配置同步助手
 * 
 * <p>提供通用层的配置同步回调机制，允许平台模块注册网络同步回调，
 * 当通过 API 修改配置时自动触发网络包发送。</p>
 * 
 * <h2>工作原理</h2>
 * <ol>
 *   <li>Fabric/Forge 客户端初始化时注册 syncCallback（发送网络包）</li>
 *   <li>OneKeyMinerAPI 的 set 方法修改配置后调用 {@link #triggerSync()}</li>
 *   <li>triggerSync() 调用已注册的回调，平台发送网络包同步到服务器</li>
 * </ol>
 * 
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.20.1
 */
public final class ConfigSyncHelper {
    
    /** 同步回调（由平台模块注册） */
    private static Runnable syncCallback = null;
    
    /** 配置变更监听器列表 */
    private static final List<Consumer<String>> CONFIG_CHANGE_LISTENERS = new ArrayList<>();
    
    private ConfigSyncHelper() {
        // 工具类不允许实例化
    }
    
    /**
     * 注册同步回调
     * 
     * <p>由平台客户端模块在初始化时调用，注册网络包发送逻辑。</p>
     * 
     * @param callback 同步回调，调用时应发送网络包到服务器
     */
    public static void registerSyncCallback(Runnable callback) {
        syncCallback = callback;
        OneKeyMiner.LOGGER.debug("已注册配置同步回调");
    }
    
    /**
     * 触发配置同步
     * 
     * <p>在配置修改后调用，将变更同步到服务器。
     * 如果未注册回调（如服务端环境），则静默跳过。</p>
     */
    public static void triggerSync() {
        if (syncCallback != null) {
            try {
                syncCallback.run();
            } catch (Exception e) {
                OneKeyMiner.LOGGER.debug("配置同步回调执行失败: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 通知配置变更
     * 
     * <p>触发所有已注册的配置变更监听器。</p>
     * 
     * @param configKey 变更的配置键名（如 "selectedShape", "teleportDrops", "teleportExp"）
     */
    public static void notifyConfigChanged(String configKey) {
        for (Consumer<String> listener : CONFIG_CHANGE_LISTENERS) {
            try {
                listener.accept(configKey);
            } catch (Exception e) {
                OneKeyMiner.LOGGER.warn("配置变更监听器执行失败: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 添加配置变更监听器
     * 
     * @param listener 监听器，接收变更的配置键名
     */
    public static void addConfigChangeListener(Consumer<String> listener) {
        CONFIG_CHANGE_LISTENERS.add(listener);
    }
    
    /**
     * 移除配置变更监听器
     * 
     * @param listener 要移除的监听器
     * @return 如果成功移除返回 true
     */
    public static boolean removeConfigChangeListener(Consumer<String> listener) {
        return CONFIG_CHANGE_LISTENERS.remove(listener);
    }
    
    /**
     * 清除所有监听器（用于测试或重置）
     */
    public static void clearListeners() {
        CONFIG_CHANGE_LISTENERS.clear();
    }
}
