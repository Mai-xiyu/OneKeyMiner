package org.xiyu.onekeyminer.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.xiyu.onekeyminer.chain.ChainActionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 链式操作前事件（可取消）
 * 
 * <p>在链式操作（挖掘/交互/种植）开始执行之前触发。
 * 监听器可以：</p>
 * <ul>
 *   <li>取消整个操作（调用 {@link #cancel()}）</li>
 *   <li>修改将要操作的目标列表（调用 {@link #setTargetPositions(List)}）</li>
 *   <li>根据区域保护等条件阻止操作</li>
 * </ul>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 在领地内禁止连锁挖掘
 * ChainEvents.registerPreActionListener(event -> {
 *     if (event.getActionType() == ChainActionType.MINING) {
 *         if (isProtectedArea(event.getOriginPos())) {
 *             event.cancel();
 *             return;
 *         }
 *         
 *         // 移除受保护区域内的方块
 *         List<BlockPos> filtered = event.getTargetPositions().stream()
 *             .filter(pos -> !isProtectedArea(pos))
 *             .toList();
 *         event.setTargetPositions(filtered);
 *     }
 * });
 * }</pre>
 * 
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.21.9
 * @see ChainEvents
 * @see PostActionEvent
 */
public final class PreActionEvent {
    
    /** 执行操作的玩家 */
    private final ServerPlayer player;
    
    /** 世界实例 */
    private final Level level;
    
    /** 起始位置 */
    private final BlockPos originPos;
    
    /** 将要操作的目标位置列表（可修改） */
    private List<BlockPos> targetPositions;
    
    /** 使用的物品 */
    private final ItemStack tool;
    
    /** 操作类型 */
    private final ChainActionType actionType;
    
    /** 事件是否被取消 */
    private boolean cancelled = false;
    
    /** 取消原因 */
    private String cancelReason = null;
    
    /**
     * 创建链式操作前事件
     * 
     * @param player 玩家
     * @param level 世界
     * @param originPos 起始位置
     * @param targetPositions 目标位置列表
     * @param tool 使用的物品
     * @param actionType 操作类型
     */
    public PreActionEvent(
            ServerPlayer player,
            Level level,
            BlockPos originPos,
            List<BlockPos> targetPositions,
            ItemStack tool,
            ChainActionType actionType
    ) {
        this.player = player;
        this.level = level;
        this.originPos = originPos;
        this.targetPositions = new ArrayList<>(targetPositions);
        this.tool = tool;
        this.actionType = actionType;
    }
    
    // ==================== Getters ====================
    
    /**
     * 获取执行操作的玩家
     * 
     * @return 服务端玩家实例
     */
    public ServerPlayer getPlayer() {
        return player;
    }
    
    /**
     * 获取世界实例
     * 
     * @return Level 实例
     */
    public Level getLevel() {
        return level;
    }
    
    /**
     * 获取起始位置
     * 
     * @return 操作的起始 BlockPos
     */
    public BlockPos getOriginPos() {
        return originPos;
    }
    
    /**
     * 获取将要操作的目标位置列表
     * 
     * <p>此列表可以被修改，修改后的列表将用于实际操作。</p>
     * 
     * @return 目标位置列表（可修改）
     */
    public List<BlockPos> getTargetPositions() {
        return targetPositions;
    }
    
    /**
     * 获取目标数量
     * 
     * @return 目标位置数量
     */
    public int getTargetCount() {
        return targetPositions.size();
    }
    
    /**
     * 获取使用的物品
     * 
     * @return 物品堆
     */
    public ItemStack getTool() {
        return tool;
    }
    
    /**
     * 获取操作类型
     * 
     * @return 链式操作类型
     */
    public ChainActionType getActionType() {
        return actionType;
    }
    
    // ==================== 事件控制 ====================
    
    /**
     * 取消操作
     * 
     * <p>调用此方法后，整个链式操作将不会执行。</p>
     */
    public void cancel() {
        this.cancelled = true;
    }
    
    /**
     * 取消操作并提供原因
     * 
     * @param reason 取消原因（可用于日志或调试）
     */
    public void cancel(String reason) {
        this.cancelled = true;
        this.cancelReason = reason;
    }
    
    /**
     * 检查事件是否被取消
     * 
     * @return 如果事件被取消返回 true
     */
    public boolean isCancelled() {
        return cancelled;
    }
    
    /**
     * 获取取消原因
     * 
     * @return 取消原因字符串，如果未设置返回 null
     */
    public String getCancelReason() {
        return cancelReason;
    }
    
    /**
     * 设置目标位置列表
     * 
     * <p>可以用此方法过滤或修改将要操作的位置。</p>
     * 
     * @param positions 新的目标位置列表
     */
    public void setTargetPositions(List<BlockPos> positions) {
        this.targetPositions = new ArrayList<>(positions);
    }
    
    /**
     * 从目标列表中移除指定位置
     * 
     * @param pos 要移除的位置
     * @return 如果成功移除返回 true
     */
    public boolean removeTarget(BlockPos pos) {
        return targetPositions.remove(pos);
    }
    
    /**
     * 向目标列表添加位置
     * 
     * @param pos 要添加的位置
     * @return 如果成功添加返回 true
     */
    public boolean addTarget(BlockPos pos) {
        if (!targetPositions.contains(pos)) {
            return targetPositions.add(pos);
        }
        return false;
    }
    
    /**
     * 清空目标列表
     * 
     * <p>清空后操作将不会执行任何事情（但不会触发取消事件）。</p>
     */
    public void clearTargets() {
        targetPositions.clear();
    }
    
    @Override
    public String toString() {
        return String.format(
                "PreActionEvent{actionType=%s, player=%s, origin=%s, targets=%d, cancelled=%s}",
                actionType,
                player.getName().getString(),
                originPos,
                targetPositions.size(),
                cancelled
        );
    }
}
