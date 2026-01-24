package org.xiyu.onekeyminer.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.xiyu.onekeyminer.chain.ChainActionResult;
import org.xiyu.onekeyminer.chain.ChainActionType;

import java.util.Collections;
import java.util.List;

/**
 * 链式操作后事件（不可取消）
 * 
 * <p>在链式操作（挖掘/交互/种植）完成后触发。
 * 此事件仅用于收集信息，不能取消或修改操作结果。</p>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 统计连锁挖掘数量
 * ChainEvents.registerPostActionListener(event -> {
 *     if (event.getActionType() == ChainActionType.MINING) {
 *         int mined = event.getTotalCount();
 *         PlayerStats.addMinedBlocks(event.getPlayer(), mined);
 *         
 *         // 记录日志
 *         OneKeyMiner.LOGGER.info("{} 连锁挖掘了 {} 个方块", 
 *             event.getPlayer().getName().getString(), mined);
 *     }
 * });
 * }</pre>
 * 
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.21.9
 * @see ChainEvents
 * @see PreActionEvent
 */
public final class PostActionEvent {
    
    /** 执行操作的玩家 */
    private final ServerPlayer player;
    
    /** 世界实例 */
    private final Level level;
    
    /** 起始位置 */
    private final BlockPos originPos;
    
    /** 操作结果 */
    private final ChainActionResult result;
    
    /**
     * 创建链式操作后事件
     * 
     * @param player 玩家
     * @param level 世界
     * @param originPos 起始位置
     * @param result 操作结果
     */
    public PostActionEvent(
            ServerPlayer player,
            Level level,
            BlockPos originPos,
            ChainActionResult result
    ) {
        this.player = player;
        this.level = level;
        this.originPos = originPos;
        this.result = result;
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
     * 获取完整的操作结果
     * 
     * @return ChainActionResult 实例
     */
    public ChainActionResult getResult() {
        return result;
    }
    
    /**
     * 获取操作类型
     * 
     * @return 链式操作类型
     */
    public ChainActionType getActionType() {
        return result.actionType();
    }
    
    /**
     * 获取成功操作的位置列表
     * 
     * @return 不可修改的位置列表
     */
    public List<BlockPos> getSuccessPositions() {
        return result.successPositions();
    }
    
    /**
     * 获取操作的总数量
     * 
     * @return 成功操作的目标数量
     */
    public int getTotalCount() {
        return result.totalCount();
    }
    
    /**
     * 获取消耗的工具耐久度
     * 
     * @return 耐久度消耗值
     */
    public int getDurabilityUsed() {
        return result.durabilityUsed();
    }
    
    /**
     * 获取消耗的饥饿值
     * 
     * @return 饥饿值消耗（半个鸡腿单位）
     */
    public float getHungerUsed() {
        return result.hungerUsed();
    }
    
    /**
     * 获取停止原因
     * 
     * @return 停止原因枚举
     */
    public ChainActionResult.StopReason getStopReason() {
        return result.stopReason();
    }
    
    /**
     * 检查操作是否成功（至少有一个目标被操作）
     * 
     * @return 如果成功返回 true
     */
    public boolean isSuccess() {
        return result.isSuccess();
    }
    
    /**
     * 获取操作的简要描述
     * 
     * @return 描述字符串
     */
    public String getSummary() {
        return result.getSummary();
    }
    
    @Override
    public String toString() {
        return String.format(
                "PostActionEvent{actionType=%s, player=%s, origin=%s, count=%d, stopReason=%s}",
                getActionType(),
                player.getName().getString(),
                originPos,
                getTotalCount(),
                getStopReason()
        );
    }
}
