package org.xiyu.onekeyminer.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.xiyu.onekeyminer.chain.ChainActionType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

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
 * @since Minecraft 1.21.7
 * @see ChainEvents
 * @see PostActionEvent
 */
public final class PreActionEvent {

    private static final int MAX_TARGET_POSITIONS = 40_960;
    
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
        this.player = Objects.requireNonNull(player, "player");
        this.level = Objects.requireNonNull(level, "level");
        this.originPos = Objects.requireNonNull(originPos, "originPos").immutable();
        this.targetPositions = new BoundedTargetList(
                Objects.requireNonNull(targetPositions, "targetPositions")
        );
        this.tool = Objects.requireNonNull(tool, "tool").copy();
        this.actionType = Objects.requireNonNull(actionType, "actionType");
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
     * <p>此列表可以被修改，修改后的列表将用于实际操作。为避免有缺陷的
     * 监听器造成无界内存增长，列表最多保存 40,960 个位置；超过上限的
     * 直接添加会抛出 {@link IllegalStateException}。</p>
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
        return tool.copy();
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
        this.targetPositions = new BoundedTargetList(
                Objects.requireNonNull(positions, "positions")
        );
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
        if (pos == null) {
            return false;
        }
        pos = pos.immutable();
        if (!targetPositions.contains(pos)) {
            return targetPositions.add(pos);
        }
        return false;
    }

    private static final class BoundedTargetList extends ArrayList<BlockPos> {
        private BoundedTargetList(Collection<? extends BlockPos> positions) {
            super(Math.min(MAX_TARGET_POSITIONS, positions.size()));
            int copied = 0;
            for (BlockPos pos : positions) {
                if (copied++ >= MAX_TARGET_POSITIONS) {
                    break;
                }
                super.add(immutable(pos));
            }
        }

        @Override
        public boolean add(BlockPos pos) {
            requireCapacityFor(1);
            return super.add(immutable(pos));
        }

        @Override
        public void add(int index, BlockPos element) {
            requireCapacityFor(1);
            super.add(index, immutable(element));
        }

        @Override
        public boolean addAll(Collection<? extends BlockPos> positions) {
            Objects.requireNonNull(positions, "positions");
            return super.addAll(copyForInsertion(positions));
        }

        @Override
        public boolean addAll(
                int index,
                Collection<? extends BlockPos> positions
        ) {
            Objects.requireNonNull(positions, "positions");
            if (index < 0 || index > size()) {
                throw new IndexOutOfBoundsException(
                        "Index: " + index + ", Size: " + size()
                );
            }
            return super.addAll(index, copyForInsertion(positions));
        }

        @Override
        public BlockPos set(int index, BlockPos element) {
            return super.set(index, immutable(element));
        }

        @Override
        public void ensureCapacity(int minCapacity) {
            super.ensureCapacity(Math.min(MAX_TARGET_POSITIONS, minCapacity));
        }

        private void requireCapacityFor(int additional) {
            if (additional < 0
                    || additional > MAX_TARGET_POSITIONS - size()) {
                throw new IllegalStateException(
                        "PreActionEvent target limit exceeded: "
                                + MAX_TARGET_POSITIONS
                );
            }
        }

        private List<BlockPos> copyForInsertion(
                Collection<? extends BlockPos> positions
        ) {
            int remaining = MAX_TARGET_POSITIONS - size();
            List<BlockPos> copy = new ArrayList<>(
                    Math.min(remaining, positions.size())
            );
            for (BlockPos pos : positions) {
                if (copy.size() >= remaining) {
                    throw new IllegalStateException(
                            "PreActionEvent target limit exceeded: "
                                    + MAX_TARGET_POSITIONS
                    );
                }
                copy.add(immutable(pos));
            }
            return copy;
        }

        private static BlockPos immutable(BlockPos pos) {
            return Objects.requireNonNull(pos, "pos").immutable();
        }
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
