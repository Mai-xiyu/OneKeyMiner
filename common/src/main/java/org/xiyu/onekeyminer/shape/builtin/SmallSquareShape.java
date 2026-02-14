package org.xiyu.onekeyminer.shape.builtin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.shape.ChainShape;
import org.xiyu.onekeyminer.shape.ShapeContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 小型方形形状 - 3×3 单层平面
 * 
 * <p>以目标方块为中心，在垂直于玩家视线方向的平面上
 * 收集 3×3 的方形区域（共 9 个方块，含中心）。不向深度方向延伸。</p>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.0
 * @since Minecraft 1.20.1
 */
public class SmallSquareShape implements ChainShape {
    
    public static final ResourceLocation ID = new ResourceLocation(OneKeyMiner.MOD_ID, "small_square");
    
    @Override
    public ResourceLocation getId() {
        return ID;
    }
    
    @Override
    public String getTranslationKey() {
        return "onekeyminer.shape.small_square";
    }
    
    @Override
    public List<BlockPos> collectBlocks(ShapeContext context) {
        List<BlockPos> result = new ArrayList<>();
        
        Direction direction = context.getTunnelDirection();
        BlockPos originPos = context.getOriginPos();
        Level level = context.getLevel();
        int maxBlocks = context.getMaxBlocks();
        
        // 确定截面的两个垂直轴
        Direction[] crossAxes = getCrossAxes(direction);
        Direction axis1 = crossAxes[0];
        Direction axis2 = crossAxes[1];
        
        // 只在起始位置的截面挖掘 3×3 平面（不延伸）
        for (int a = -1; a <= 1 && result.size() < maxBlocks; a++) {
            for (int b = -1; b <= 1 && result.size() < maxBlocks; b++) {
                BlockPos pos = originPos
                        .relative(axis1, a)
                        .relative(axis2, b);
                
                // 跳过起始位置
                if (pos.equals(originPos)) continue;
                
                BlockState state = level.getBlockState(pos);
                if (context.isMatchingBlock(state)) {
                    result.add(pos);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 获取与延伸方向垂直的两个轴
     */
    private Direction[] getCrossAxes(Direction tunnelDir) {
        switch (tunnelDir) {
            case NORTH:
            case SOUTH:
                return new Direction[]{Direction.EAST, Direction.UP};
            case EAST:
            case WEST:
                return new Direction[]{Direction.SOUTH, Direction.UP};
            case UP:
            case DOWN:
                return new Direction[]{Direction.EAST, Direction.SOUTH};
            default:
                return new Direction[]{Direction.EAST, Direction.UP};
        }
    }
    
    @Override
    public boolean requiresDirection() {
        return true;
    }
}
