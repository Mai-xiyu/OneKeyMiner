package org.xiyu.onekeyminer.api.event;

import org.xiyu.onekeyminer.OneKeyMiner;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 挖矿事件注册和分发系统
 * 
 * <p>提供统一的事件监听器注册入口，支持：</p>
 * <ul>
 *   <li>{@link PreMineEvent} - 挖矿前事件（可取消）</li>
 *   <li>{@link PostMineEvent} - 挖矿后事件（只读）</li>
 * </ul>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 在模组初始化时注册监听器
 * MiningEvents.registerPreMineListener(event -> {
 *     // 处理挖矿前事件
 * });
 * 
 * MiningEvents.registerPostMineListener(event -> {
 *     // 处理挖矿后事件
 * });
 * }</pre>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.0
 * @since Minecraft 1.21.9
 */
public final class MiningEvents {
    
    /** 挖矿前事件监听器列表 */
    private static final List<Consumer<PreMineEvent>> PRE_MINE_LISTENERS = new ArrayList<>();
    
    /** 挖矿后事件监听器列表 */
    private static final List<Consumer<PostMineEvent>> POST_MINE_LISTENERS = new ArrayList<>();
    
    private MiningEvents() {
        // 私有构造函数，防止实例化
    }
    
    /**
     * 注册挖矿前事件监听器
     * 
     * <p>监听器将在连锁挖矿开始前被调用。
     * 可以取消事件或修改将要挖掘的方块列表。</p>
     * 
     * @param listener 事件监听器
     */
    public static void registerPreMineListener(Consumer<PreMineEvent> listener) {
        PRE_MINE_LISTENERS.add(listener);
        OneKeyMiner.LOGGER.debug("已注册 PreMineEvent 监听器");
    }
    
    /**
     * 注册挖矿后事件监听器
     * 
     * <p>监听器将在连锁挖矿完成后被调用。
     * 此事件不可取消，仅用于收集信息。</p>
     * 
     * @param listener 事件监听器
     */
    public static void registerPostMineListener(Consumer<PostMineEvent> listener) {
        POST_MINE_LISTENERS.add(listener);
        OneKeyMiner.LOGGER.debug("已注册 PostMineEvent 监听器");
    }
    
    /**
     * 移除挖矿前事件监听器
     * 
     * @param listener 要移除的监听器
     * @return 如果移除成功返回 true
     */
    public static boolean unregisterPreMineListener(Consumer<PreMineEvent> listener) {
        return PRE_MINE_LISTENERS.remove(listener);
    }
    
    /**
     * 移除挖矿后事件监听器
     * 
     * @param listener 要移除的监听器
     * @return 如果移除成功返回 true
     */
    public static boolean unregisterPostMineListener(Consumer<PostMineEvent> listener) {
        return POST_MINE_LISTENERS.remove(listener);
    }
    
    /**
     * 触发挖矿前事件
     * 
     * <p>内部方法，由 {@link org.xiyu.onekeyminer.mining.MiningLogic} 调用</p>
     * 
     * @param event 事件实例
     */
    public static void firePreMineEvent(PreMineEvent event) {
        for (Consumer<PreMineEvent> listener : PRE_MINE_LISTENERS) {
            try {
                listener.accept(event);
                
                // 如果事件被取消，可以选择是否继续通知其他监听器
                // 这里选择继续通知，让所有监听器都能知道事件发生
            } catch (Exception e) {
                OneKeyMiner.LOGGER.error("PreMineEvent 监听器发生异常: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 触发挖矿后事件
     * 
     * <p>内部方法，由 {@link org.xiyu.onekeyminer.mining.MiningLogic} 调用</p>
     * 
     * @param event 事件实例
     */
    public static void firePostMineEvent(PostMineEvent event) {
        for (Consumer<PostMineEvent> listener : POST_MINE_LISTENERS) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                OneKeyMiner.LOGGER.error("PostMineEvent 监听器发生异常: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 清除所有已注册的监听器
     * 
     * <p>通常不需要调用，除非在测试或特殊情况下</p>
     */
    public static void clearAllListeners() {
        PRE_MINE_LISTENERS.clear();
        POST_MINE_LISTENERS.clear();
        OneKeyMiner.LOGGER.debug("已清除所有挖矿事件监听器");
    }
    
    /**
     * 获取已注册的 PreMineEvent 监听器数量
     * 
     * @return 监听器数量
     */
    public static int getPreMineListenerCount() {
        return PRE_MINE_LISTENERS.size();
    }
    
    /**
     * 获取已注册的 PostMineEvent 监听器数量
     * 
     * @return 监听器数量
     */
    public static int getPostMineListenerCount() {
        return POST_MINE_LISTENERS.size();
    }
}
