package org.xiyu.onekeyminer.chain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.List;

/**
 * 链式操作结果记录
 * 
 * <p>封装链式操作执行后的所有结果数据，包括：</p>
 * <ul>
 *   <li>操作类型和成功的位置列表</li>
 *   <li>统计信息（总数、耐久消耗、饥饿消耗等）</li>
 *   <li>停止原因</li>
 * </ul>
 * 
 * <p>使用 Java Record 实现不可变数据结构。</p>
 * 
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.21.9
 */
public record ChainActionResult(
        /** 操作类型 */
        ChainActionType actionType,
        
        /** 成功操作的位置列表 */
        List<BlockPos> successPositions,
        
        /** 操作的总数量 */
        int totalCount,
        
        /** 消耗的工具耐久度 */
        int durabilityUsed,
        
        /** 消耗的饥饿值（半个鸡腿单位） */
        float hungerUsed,
        
        /** 停止原因 */
        StopReason stopReason,
        
        /** 收集到的掉落物列表 */
        List<ItemStack> collectedDrops,
        
        /** 收集到的经验值 */
        int experienceCollected
) {
    
    /**
     * 链式操作停止原因枚举
     */
    public enum StopReason {
        /** 操作正常完成（没有更多可操作的目标） */
        COMPLETED("正常完成"),
        
        /** 达到最大操作数量限制 */
        MAX_COUNT_REACHED("达到数量上限"),
        
        /** 达到最大距离限制 */
        MAX_DISTANCE_REACHED("超出距离限制"),
        
        /** 工具耐久度不足 */
        TOOL_DURABILITY_LOW("工具耐久不足"),
        
        /** 工具损坏 */
        TOOL_BROKEN("工具已损坏"),
        
        /** 玩家饥饿值不足 */
        HUNGER_LOW("饥饿值不足"),
        
        /** 被事件取消 */
        EVENT_CANCELLED("被事件取消"),
        
        /** 权限不足 */
        PERMISSION_DENIED("权限不足"),
        
        /** 物品不足（种植操作） */
        ITEMS_EXHAUSTED("物品不足"),
        
        /** 操作超时 */
        TIMEOUT("操作超时"),
        
        /** 发生错误 */
        ERROR("发生错误");
        
        private final String displayName;
        
        StopReason(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 创建一个表示操作被取消的空结果
     * 
     * @param actionType 操作类型
     * @param reason 取消原因
     * @return 空结果实例
     */
    public static ChainActionResult cancelled(ChainActionType actionType, StopReason reason) {
        return new ChainActionResult(
                actionType,
                Collections.emptyList(),
                0,
                0,
                0f,
                reason,
                Collections.emptyList(),
                0
        );
    }
    
    /**
     * 创建一个成功的结果
     * 
     * @param actionType 操作类型
     * @param positions 成功操作的位置
     * @param durabilityUsed 消耗的耐久
     * @param hungerUsed 消耗的饥饿
     * @param stopReason 停止原因
     * @param collectedDrops 收集到的掉落物列表
     * @param experienceCollected 收集到的经验值
     * @return 结果实例
     */
    public static ChainActionResult success(
            ChainActionType actionType,
            List<BlockPos> positions,
            int durabilityUsed,
            float hungerUsed,
            StopReason stopReason,
            List<ItemStack> collectedDrops,
            int experienceCollected
    ) {
        return new ChainActionResult(
                actionType,
                Collections.unmodifiableList(positions),
                positions.size(),
                durabilityUsed,
                hungerUsed,
                stopReason,
                collectedDrops != null ? Collections.unmodifiableList(collectedDrops) : Collections.emptyList(),
                experienceCollected
        );
    }
    
    /**
     * 检查操作是否成功（至少有一个位置被操作）
     * 
     * @return 如果成功返回 true
     */
    public boolean isSuccess() {
        return totalCount > 0;
    }
    
    /**
     * 检查操作是否被取消
     * 
     * @return 如果被取消返回 true
     */
    public boolean isCancelled() {
        return stopReason == StopReason.EVENT_CANCELLED;
    }
    
    /**
     * 获取操作的简要描述
     * 
     * @return 描述字符串
     */
    public String getSummary() {
        return String.format(
                "%s: %d 个目标, 耐久-%d, 饥饿-%.1f, 掉落物-%d种, 经验-%d, 停止原因: %s",
                actionType.getDisplayName(),
                totalCount,
                durabilityUsed,
                hungerUsed,
                collectedDrops.size(),
                experienceCollected,
                stopReason.getDisplayName()
        );
    }
}
