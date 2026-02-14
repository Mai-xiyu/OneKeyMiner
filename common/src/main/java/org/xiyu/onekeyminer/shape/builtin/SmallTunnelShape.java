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
 * 小型通道形状 - 1×1 直线通道
 * 
 * <p>沿玩家视线方向延伸的 1×1 直线通道。
 * 只破坏正前方一条线上的匹配方块。</p>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.0
 * @since Minecraft 1.20.1
 */
public class SmallTunnelShape implements ChainShape {
    
    public static final ResourceLocation ID = new ResourceLocation(OneKeyMiner.MOD_ID, "small_tunnel");
    
    @Override
    public ResourceLocation getId() {
        return ID;
    }
    
    @Override
    public String getTranslationKey() {
        return "onekeyminer.shape.small_tunnel";
    }
    
    @Override
    public List<BlockPos> collectBlocks(ShapeContext context) {
        List<BlockPos> result = new ArrayList<>();
        
        Direction direction = context.getTunnelDirection();
        BlockPos current = context.getOriginPos();
        Level level = context.getLevel();
        int maxBlocks = context.getMaxBlocks();
        int maxDistance = context.getMaxDistance();
        
        for (int i = 1; i <= maxDistance && result.size() < maxBlocks; i++) {
            current = current.relative(direction);
            BlockState state = level.getBlockState(current);
            
            if (context.isMatchingBlock(state)) {
                result.add(current);
            } else {
                break; // 遇到不匹配方块时停止
            }
        }
        
        return result;
    }
    
    @Override
    public boolean requiresDirection() {
        return true;
    }
}
