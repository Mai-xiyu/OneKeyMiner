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
 * 大型通道形状 - 3×3 实心通道
 * 
 * <p>沿玩家视线方向延伸的 3×3 截面实心通道。
 * 截面为以玩家所看方块为中心的 3×3 方形，包含全部 9 个方块。</p>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.0
 * @since Minecraft 1.20.1
 */
public class LargeTunnelShape implements ChainShape {
    
    public static final ResourceLocation ID = new ResourceLocation(OneKeyMiner.MOD_ID, "large_tunnel");
    
    @Override
    public ResourceLocation getId() {
        return ID;
    }
    
    @Override
    public String getTranslationKey() {
        return "onekeyminer.shape.large_tunnel";
    }
    
    @Override
    public List<BlockPos> collectBlocks(ShapeContext context) {
        List<BlockPos> result = new ArrayList<>();
        
        Direction direction = context.getTunnelDirection();
        BlockPos originPos = context.getOriginPos();
        Level level = context.getLevel();
        int maxBlocks = context.getMaxBlocks();
        int maxDistance = context.getMaxDistance();
        
        // 确定截面的两个垂直轴
        Direction[] crossAxes = getCrossAxes(direction);
        Direction axis1 = crossAxes[0];
        Direction axis2 = crossAxes[1];
        
        // 从当前位置开始（包括当前截面的其他方块），沿方向延伸
        for (int depth = 0; depth <= maxDistance && result.size() < maxBlocks; depth++) {
            BlockPos center = originPos.relative(direction, depth);
            boolean anyMatch = false;
            
            // 3×3 截面：遍历 -1 到 +1 的偏移（实心，包含中心）
            for (int a = -1; a <= 1 && result.size() < maxBlocks; a++) {
                for (int b = -1; b <= 1 && result.size() < maxBlocks; b++) {
                    BlockPos pos = center
                            .relative(axis1, a)
                            .relative(axis2, b);
                    
                    // 跳过起始位置
                    if (pos.equals(originPos)) continue;
                    
                    BlockState state = level.getBlockState(pos);
                    if (context.isMatchingBlock(state)) {
                        result.add(pos);
                        anyMatch = true;
                    }
                }
            }
            
            // 如果这一层深度完全没有匹配方块（除了第一层），停止延伸
            if (depth > 0 && !anyMatch) {
                break;
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
