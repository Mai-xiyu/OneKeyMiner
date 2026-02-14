package org.xiyu.onekeyminer.shape.builtin;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.shape.ChainShape;
import org.xiyu.onekeyminer.shape.ShapeContext;

import java.util.*;

/**
 * 不定形形状 - BFS 相邻连通搜索
 * 
 * <p>从起始方块开始，向周围扩展搜索所有匹配的连通方块。
 * 根据配置支持 6 向（正交）或 26 向（含对角线）搜索。</p>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.0
 * @since Minecraft 1.20.1
 */
public class AmorphousShape implements ChainShape {
    
    public static final ResourceLocation ID = new ResourceLocation(OneKeyMiner.MOD_ID, "amorphous");
    
    /** 6 向搜索偏移量 */
    private static final BlockPos[] ORTHOGONAL_OFFSETS = {
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
            new BlockPos(0, 1, 0), new BlockPos(0, -1, 0),
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1)
    };
    
    /** 26 向搜索偏移量 */
    private static final BlockPos[] DIAGONAL_OFFSETS;
    
    static {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        offsets.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        DIAGONAL_OFFSETS = offsets.toArray(new BlockPos[0]);
    }
    
    private static final long TIMEOUT_MS = 2000;
    private static final int MAX_ITERATIONS = 10000;
    
    @Override
    public ResourceLocation getId() {
        return ID;
    }
    
    @Override
    public String getTranslationKey() {
        return "onekeyminer.shape.amorphous";
    }
    
    @Override
    public List<BlockPos> collectBlocks(ShapeContext context) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        
        BlockPos originPos = context.getOriginPos();
        Level level = context.getLevel();
        int maxBlocks = context.getMaxBlocks();
        int maxDistance = context.getMaxDistance();
        
        BlockPos[] offsets = context.isAllowDiagonal() ? DIAGONAL_OFFSETS : ORTHOGONAL_OFFSETS;
        
        // 起始位置标记为已访问，从相邻位置开始搜索
        visited.add(originPos);
        
        for (BlockPos offset : offsets) {
            BlockPos neighbor = originPos.offset(offset);
            if (!visited.contains(neighbor)) {
                BlockState neighborState = level.getBlockState(neighbor);
                if (context.isMatchingBlock(neighborState)) {
                    queue.add(neighbor);
                    visited.add(neighbor);
                }
            }
        }
        
        long startTime = System.currentTimeMillis();
        int iterations = 0;
        
        while (!queue.isEmpty() && result.size() < maxBlocks && iterations < MAX_ITERATIONS) {
            if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                break;
            }
            
            iterations++;
            BlockPos current = queue.poll();
            
            if (current.distManhattan(originPos) > maxDistance) {
                continue;
            }
            
            result.add(current);
            
            for (BlockPos offset : offsets) {
                BlockPos neighbor = current.offset(offset);
                if (!visited.contains(neighbor) && neighbor.distManhattan(originPos) <= maxDistance) {
                    BlockState neighborState = level.getBlockState(neighbor);
                    if (context.isMatchingBlock(neighborState)) {
                        queue.add(neighbor);
                        visited.add(neighbor);
                    }
                }
            }
        }
        
        return result;
    }
    
    @Override
    public boolean requiresDirection() {
        return false;
    }
}
