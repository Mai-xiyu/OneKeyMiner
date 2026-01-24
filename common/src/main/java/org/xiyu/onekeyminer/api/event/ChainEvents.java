package org.xiyu.onekeyminer.api.event;

import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.chain.ChainActionType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 链式操作事件注册和分发系统
 * 
 * <p>提供统一的事件监听器注册入口，支持：</p>
 * <ul>
 *   <li>{@link PreActionEvent} - 操作前事件（可取消、可修改）</li>
 *   <li>{@link PostActionEvent} - 操作后事件（只读）</li>
 * </ul>
 * 
 * <p>支持按操作类型过滤监听器，提高性能。</p>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 注册通用监听器（接收所有类型的事件）
 * ChainEvents.registerPreActionListener(event -> {
 *     // 处理所有操作类型
 * });
 * 
 * // 注册针对特定操作类型的监听器
 * ChainEvents.registerPreActionListener(ChainActionType.MINING, event -> {
 *     // 只处理挖掘事件
 * });
 * 
 * // 使用条件过滤
 * ChainEvents.registerPreActionListener(
 *     event -> event.getTargetCount() > 10,  // 只处理超过10个目标的操作
 *     event -> {
 *         // 处理逻辑
 *     }
 * );
 * }</pre>
 * 
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.21.9
 */
public final class ChainEvents {
    
    /** 操作前事件监听器列表 */
    private static final List<ListenerEntry<PreActionEvent>> PRE_ACTION_LISTENERS = new ArrayList<>();
    
    /** 操作后事件监听器列表 */
    private static final List<ListenerEntry<PostActionEvent>> POST_ACTION_LISTENERS = new ArrayList<>();
    
    private ChainEvents() {
        // 工具类，禁止实例化
    }
    
    // ==================== PreActionEvent 监听器注册 ====================
    
    /**
     * 注册操作前事件监听器（接收所有类型）
     * 
     * @param listener 事件监听器
     */
    public static void registerPreActionListener(Consumer<PreActionEvent> listener) {
        PRE_ACTION_LISTENERS.add(new ListenerEntry<>(null, null, listener));
        OneKeyMiner.LOGGER.debug("已注册 PreActionEvent 通用监听器");
    }
    
    /**
     * 注册针对特定操作类型的操作前事件监听器
     * 
     * @param actionType 要监听的操作类型
     * @param listener 事件监听器
     */
    public static void registerPreActionListener(ChainActionType actionType, Consumer<PreActionEvent> listener) {
        PRE_ACTION_LISTENERS.add(new ListenerEntry<>(actionType, null, listener));
        OneKeyMiner.LOGGER.debug("已注册 PreActionEvent 监听器，类型: {}", actionType);
    }
    
    /**
     * 注册带条件过滤的操作前事件监听器
     * 
     * @param filter 过滤条件
     * @param listener 事件监听器
     */
    public static void registerPreActionListener(
            Predicate<PreActionEvent> filter, 
            Consumer<PreActionEvent> listener
    ) {
        PRE_ACTION_LISTENERS.add(new ListenerEntry<>(null, filter, listener));
        OneKeyMiner.LOGGER.debug("已注册 PreActionEvent 条件监听器");
    }
    
    /**
     * 移除操作前事件监听器
     * 
     * @param listener 要移除的监听器
     * @return 如果成功移除返回 true
     */
    public static boolean unregisterPreActionListener(Consumer<PreActionEvent> listener) {
        return PRE_ACTION_LISTENERS.removeIf(entry -> entry.listener == listener);
    }
    
    // ==================== PostActionEvent 监听器注册 ====================
    
    /**
     * 注册操作后事件监听器（接收所有类型）
     * 
     * @param listener 事件监听器
     */
    public static void registerPostActionListener(Consumer<PostActionEvent> listener) {
        POST_ACTION_LISTENERS.add(new ListenerEntry<>(null, null, listener));
        OneKeyMiner.LOGGER.debug("已注册 PostActionEvent 通用监听器");
    }
    
    /**
     * 注册针对特定操作类型的操作后事件监听器
     * 
     * @param actionType 要监听的操作类型
     * @param listener 事件监听器
     */
    public static void registerPostActionListener(ChainActionType actionType, Consumer<PostActionEvent> listener) {
        POST_ACTION_LISTENERS.add(new ListenerEntry<>(actionType, null, listener));
        OneKeyMiner.LOGGER.debug("已注册 PostActionEvent 监听器，类型: {}", actionType);
    }
    
    /**
     * 注册带条件过滤的操作后事件监听器
     * 
     * @param filter 过滤条件
     * @param listener 事件监听器
     */
    public static void registerPostActionListener(
            Predicate<PostActionEvent> filter,
            Consumer<PostActionEvent> listener
    ) {
        POST_ACTION_LISTENERS.add(new ListenerEntry<>(null, filter, listener));
        OneKeyMiner.LOGGER.debug("已注册 PostActionEvent 条件监听器");
    }
    
    /**
     * 移除操作后事件监听器
     * 
     * @param listener 要移除的监听器
     * @return 如果成功移除返回 true
     */
    public static boolean unregisterPostActionListener(Consumer<PostActionEvent> listener) {
        return POST_ACTION_LISTENERS.removeIf(entry -> entry.listener == listener);
    }
    
    // ==================== 事件触发 ====================
    
    /**
     * 触发操作前事件
     * 
     * <p>内部方法，由 {@link org.xiyu.onekeyminer.chain.ChainActionLogic} 调用</p>
     * 
     * @param event 事件实例
     */
    public static void firePreActionEvent(PreActionEvent event) {
        for (ListenerEntry<PreActionEvent> entry : PRE_ACTION_LISTENERS) {
            try {
                // 检查操作类型过滤
                if (entry.actionType != null && entry.actionType != event.getActionType()) {
                    continue;
                }
                
                // 检查条件过滤
                if (entry.filter != null && !entry.filter.test(event)) {
                    continue;
                }
                
                // 调用监听器
                entry.listener.accept(event);
                
            } catch (Exception e) {
                OneKeyMiner.LOGGER.error("PreActionEvent 监听器发生异常: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * 触发操作后事件
     * 
     * <p>内部方法，由 {@link org.xiyu.onekeyminer.chain.ChainActionLogic} 调用</p>
     * 
     * @param event 事件实例
     */
    public static void firePostActionEvent(PostActionEvent event) {
        for (ListenerEntry<PostActionEvent> entry : POST_ACTION_LISTENERS) {
            try {
                // 检查操作类型过滤
                if (entry.actionType != null && entry.actionType != event.getActionType()) {
                    continue;
                }
                
                // 检查条件过滤
                if (entry.filter != null && !entry.filter.test(event)) {
                    continue;
                }
                
                // 调用监听器
                entry.listener.accept(event);
                
            } catch (Exception e) {
                OneKeyMiner.LOGGER.error("PostActionEvent 监听器发生异常: {}", e.getMessage(), e);
            }
        }
    }
    
    // ==================== 管理方法 ====================
    
    /**
     * 清除所有已注册的监听器
     * 
     * <p>通常不需要调用，除非在测试或特殊情况下</p>
     */
    public static void clearAllListeners() {
        PRE_ACTION_LISTENERS.clear();
        POST_ACTION_LISTENERS.clear();
        OneKeyMiner.LOGGER.debug("已清除所有链式操作事件监听器");
    }
    
    /**
     * 获取已注册的 PreActionEvent 监听器数量
     * 
     * @return 监听器数量
     */
    public static int getPreActionListenerCount() {
        return PRE_ACTION_LISTENERS.size();
    }
    
    /**
     * 获取已注册的 PostActionEvent 监听器数量
     * 
     * @return 监听器数量
     */
    public static int getPostActionListenerCount() {
        return POST_ACTION_LISTENERS.size();
    }
    
    /**
     * 监听器条目记录
     * 
     * @param actionType 操作类型过滤（null 表示接收所有类型）
     * @param filter 条件过滤（null 表示不过滤）
     * @param listener 实际的监听器
     */
    private record ListenerEntry<T>(
            ChainActionType actionType,
            Predicate<T> filter,
            Consumer<T> listener
    ) {}
}
