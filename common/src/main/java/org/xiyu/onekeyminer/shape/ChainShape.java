package org.xiyu.onekeyminer.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 连锁形状接口
 * 
 * <p>定义连锁挖矿/交互的搜索形状。附属模组可以实现此接口并通过
 * {@link ShapeRegistry#register(ChainShape)} 注册自定义形状。</p>
 * 
 * <h2>内置形状</h2>
 * <ul>
 *   <li>不定形 (amorphous) - BFS 相邻连通搜索</li>
 *   <li>小型通道 (small_tunnel) - 1×1 直线通道</li>
 *   <li>大型通道 (large_tunnel) - 3×3 空心通道</li>
 *   <li>小型方形 (small_square) - 3×3 实心截面</li>
 *   <li>采矿通道 (mining_tunnel) - 下降阶梯通道</li>
 *   <li>逃生通道 (escape_tunnel) - 上升阶梯通道</li>
 * </ul>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.0
 * @since Minecraft 1.20.1
 */
public interface ChainShape {
    
    /**
     * 获取形状的唯一标识符
     * 
     * @return 形状 ID，格式为 "namespace:path"
     */
    ResourceLocation getId();
    
    /**
     * 获取形状名称的翻译键
     * 
     * <p>用于在配置界面和 HUD 中显示形状名称</p>
     * 
     * @return 翻译键，如 "onekeyminer.shape.amorphous"
     */
    String getTranslationKey();
    
    /**
     * 收集符合条件的方块位置
     * 
     * <p>根据形状定义的搜索算法，从起始位置开始搜索并返回目标方块列表。
     * 实现时应遵守 {@link ShapeContext} 中的 maxBlocks 和 maxDistance 限制。</p>
     * 
     * @param context 形状搜索上下文
     * @return 目标方块位置列表（不含起始位置）
     */
    List<BlockPos> collectBlocks(ShapeContext context);
    
    /**
     * 获取预览方块位置（客户端用）
     * 
     * <p>用于在 HUD 或渲染中显示将被连锁处理的方块。
     * 默认实现直接调用 {@link #collectBlocks(ShapeContext)}。</p>
     * 
     * @param context 形状搜索上下文
     * @return 预览方块位置列表
     */
    default List<BlockPos> getPreviewPositions(ShapeContext context) {
        return collectBlocks(context);
    }
    
    /**
     * 此形状是否需要玩家视线方向
     * 
     * <p>通道型形状需要知道玩家面朝方向来确定延伸方向。
     * 不定形等基于连通性的形状通常不需要。</p>
     * 
     * @return 如果需要方向信息返回 true
     */
    default boolean requiresDirection() {
        return false;
    }
}
