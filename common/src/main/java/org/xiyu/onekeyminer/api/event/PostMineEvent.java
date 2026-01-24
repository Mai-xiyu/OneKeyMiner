package org.xiyu.onekeyminer.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.xiyu.onekeyminer.mining.MiningLogic;

import java.util.Collections;
import java.util.List;

/**
 * 挖矿后事件
 * 
 * <p>在连锁挖矿完成后触发。此事件不可取消，仅用于信息收集。</p>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * MiningEvents.registerPostMineListener(event -> {
 *     // 记录挖矿统计
 *     int count = event.getTotalMined();
 *     logger.info("玩家 {} 连锁挖掘了 {} 个方块", 
 *         event.getPlayer().getName().getString(), count);
 *     
 *     // 给予奖励
 *     if (count >= 10) {
 *         giveBonus(event.getPlayer());
 *     }
 * });
 * }</pre>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.0
 * @since Minecraft 1.21.9
 * @see MiningEvents#registerPostMineListener
 */
public class PostMineEvent {
    
    private final ServerPlayer player;
    private final Level level;
    private final BlockPos originPos;
    private final List<BlockPos> minedBlocks;
    private final int totalMined;
    private final MiningLogic.StopReason stopReason;
    
    /**
     * 创建挖矿后事件
     * 
     * @param player 执行挖矿的玩家
     * @param level 世界实例
     * @param originPos 起始方块位置
     * @param minedBlocks 已挖掘的方块列表
     * @param totalMined 总共挖掘的方块数量
     * @param stopReason 停止挖矿的原因
     */
    public PostMineEvent(ServerPlayer player, Level level, BlockPos originPos,
                         List<BlockPos> minedBlocks, int totalMined, 
                         MiningLogic.StopReason stopReason) {
        this.player = player;
        this.level = level;
        this.originPos = originPos;
        this.minedBlocks = Collections.unmodifiableList(minedBlocks);
        this.totalMined = totalMined;
        this.stopReason = stopReason;
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
     * 获取已挖掘的方块列表
     * 
     * @return 不可修改的方块位置列表
     */
    public List<BlockPos> getMinedBlocks() {
        return minedBlocks;
    }
    
    /**
     * 获取总共挖掘的方块数量
     * 
     * @return 挖掘数量
     */
    public int getTotalMined() {
        return totalMined;
    }
    
    /**
     * 获取停止挖矿的原因
     * 
     * @return 停止原因枚举
     */
    public MiningLogic.StopReason getStopReason() {
        return stopReason;
    }
    
    /**
     * 检查挖矿是否正常完成
     * 
     * @return 如果正常完成（非中途停止）返回 true
     */
    public boolean isCompleted() {
        return stopReason == MiningLogic.StopReason.COMPLETED;
    }
}
