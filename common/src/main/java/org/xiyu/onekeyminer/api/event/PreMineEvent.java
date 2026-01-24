package org.xiyu.onekeyminer.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 挖矿前事件
 * 
 * <p>在连锁挖矿开始前触发。监听器可以：</p>
 * <ul>
 *   <li>取消整个挖矿操作</li>
 *   <li>修改将要挖掘的方块列表</li>
 *   <li>获取挖矿相关信息</li>
 * </ul>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * MiningEvents.registerPreMineListener(event -> {
 *     // 禁止在特定区域挖矿
 *     if (isProtectedArea(event.getOriginPos())) {
 *         event.cancel();
 *         return;
 *     }
 *     
 *     // 移除特定方块
 *     event.getBlocksToMine().removeIf(pos -> isSpecialBlock(pos));
 * });
 * }</pre>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.0
 * @since Minecraft 1.21.9
 * @see MiningEvents#registerPreMineListener
 */
public class PreMineEvent {
    
    private final ServerPlayer player;
    private final Level level;
    private final BlockPos originPos;
    private final List<BlockPos> blocksToMine;
    private final ItemStack tool;
    private boolean cancelled = false;
    
    /**
     * 创建挖矿前事件
     * 
     * @param player 执行挖矿的玩家
     * @param level 世界实例
     * @param originPos 起始方块位置
     * @param blocksToMine 将要挖掘的方块列表（可修改）
     * @param tool 使用的工具
     */
    public PreMineEvent(ServerPlayer player, Level level, BlockPos originPos, 
                        List<BlockPos> blocksToMine, ItemStack tool) {
        this.player = player;
        this.level = level;
        this.originPos = originPos;
        this.blocksToMine = new ArrayList<>(blocksToMine); // 创建可修改副本
        this.tool = tool;
    }
    
    /**
     * 获取执行挖矿的玩家
     * 
     * @return 服务端玩家实例
     */
    public ServerPlayer getPlayer() {
        return player;
    }
    
    /**
     * 获取世界实例
     * 
     * @return 世界实例
     */
    public Level getLevel() {
        return level;
    }
    
    /**
     * 获取起始方块位置
     * 
     * @return 起始位置
     */
    public BlockPos getOriginPos() {
        return originPos;
    }
    
    /**
     * 获取将要挖掘的方块列表
     * 
     * <p>此列表可被修改。移除的方块将不会被挖掘。</p>
     * 
     * @return 可修改的方块位置列表
     */
    public List<BlockPos> getBlocksToMine() {
        return blocksToMine;
    }
    
    /**
     * 获取使用的工具
     * 
     * @return 工具物品栈
     */
    public ItemStack getTool() {
        return tool;
    }
    
    /**
     * 检查事件是否已被取消
     * 
     * @return 如果已取消返回 true
     */
    public boolean isCancelled() {
        return cancelled;
    }
    
    /**
     * 取消连锁挖矿操作
     * 
     * <p>调用后，所有方块都不会被挖掘（起始方块除外，因为已经被破坏）</p>
     */
    public void cancel() {
        this.cancelled = true;
    }
    
    /**
     * 设置取消状态
     * 
     * @param cancelled 是否取消
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
