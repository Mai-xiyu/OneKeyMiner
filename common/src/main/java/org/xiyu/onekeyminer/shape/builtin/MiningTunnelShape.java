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
 * 采矿通道形状 - 下降阶梯
 * 
 * <p>沿玩家水平视线方向每前进一格下降一格的阶梯式通道。
 * 每一阶为 1×2 截面（宽1高2），从起始位置向前+向下延伸。</p>
 * 
 * <pre>
 *  起点
 *  ██
 *    ██
 *      ██
 *        ██  (不断向前+向下)
 * </pre>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.0
 * @since Minecraft 1.20.1
 */
public class MiningTunnelShape implements ChainShape {
    
    public static final ResourceLocation ID = new ResourceLocation(OneKeyMiner.MOD_ID, "mining_tunnel");
    
    @Override
    public ResourceLocation getId() {
        return ID;
    }
    
    @Override
    public String getTranslationKey() {
        return "onekeyminer.shape.mining_tunnel";
    }
    
    @Override
    public List<BlockPos> collectBlocks(ShapeContext context) {
        List<BlockPos> result = new ArrayList<>();
        
        // 采矿通道始终使用水平方向
        Direction horizontal = context.getPlayerFacing();
        if (horizontal == null) horizontal = Direction.NORTH;
        // 确保是水平方向
        if (horizontal == Direction.UP || horizontal == Direction.DOWN) {
            horizontal = Direction.NORTH;
        }
        
        BlockPos originPos = context.getOriginPos();
        Level level = context.getLevel();
        int maxBlocks = context.getMaxBlocks();
        int maxDistance = context.getMaxDistance();
        
        // 每一步：向前一格 + 向下一格，挖 1×2 截面（本格和上面一格）
        BlockPos stepPos = originPos;
        
        for (int step = 1; step <= maxDistance && result.size() < maxBlocks; step++) {
            // 向前一格 + 向下一格
            stepPos = stepPos.relative(horizontal).below();
            
            // 挖 1×2 截面：stepPos (脚部) 和 stepPos.above() (头部)
            for (int h = 0; h <= 1 && result.size() < maxBlocks; h++) {
                BlockPos pos = stepPos.above(h);
                
                if (pos.equals(originPos)) continue;
                
                BlockState state = level.getBlockState(pos);
                if (context.isMatchingBlock(state)) {
                    result.add(pos);
                }
            }
        }
        
        return result;
    }
    
    @Override
    public boolean requiresDirection() {
        return true;
    }
}
