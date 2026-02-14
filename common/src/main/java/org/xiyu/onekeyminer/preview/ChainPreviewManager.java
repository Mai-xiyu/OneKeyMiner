package org.xiyu.onekeyminer.preview;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.MinerConfig;
import org.xiyu.onekeyminer.shape.ChainShape;
import org.xiyu.onekeyminer.shape.ShapeContext;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 连锁预览管理器（客户端单例）
 * 
 * <p>在客户端计算当前形状的预览方块列表，供 HUD 和渲染层使用。
 * 通过 {@link PreviewListener} 接口通知变化。</p>
 * 
 * <p>预览计算在客户端 tick 中执行，使用节流机制避免每帧重算。</p>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.0
 * @since Minecraft 1.20.1
 */
public class ChainPreviewManager {
    
    /** 单例实例 */
    private static ChainPreviewManager instance;
    
    /** 当前预览方块列表 */
    private volatile List<BlockPos> previewBlocks = Collections.emptyList();
    
    /** 当前预览使用的形状名称翻译键 */
    private volatile String currentShapeTranslationKey = "";
    
    /** 上次计算时间（防止频繁重算） */
    private long lastCalculateTime = 0;
    
    /** 计算间隔（毫秒） */
    private static final long CALCULATE_INTERVAL_MS = 200;
    
    /** 预览监听器列表 */
    private final List<PreviewListener> listeners = new CopyOnWriteArrayList<>();
    
    /** 是否启用预览 */
    private boolean enabled = true;
    
    private ChainPreviewManager() {}
    
    /**
     * 获取单例实例
     */
    public static ChainPreviewManager getInstance() {
        if (instance == null) {
            instance = new ChainPreviewManager();
        }
        return instance;
    }
    
    /**
     * 客户端 tick 更新
     * 
     * <p>应在客户端 tick 事件中调用，传入当前看向的方块位置和玩家信息。
     * 如果条件不满足（未按下连锁键、没有看向方块等），会清除预览。</p>
     * 
     * @param level 客户端世界
     * @param lookingAt 玩家看向的方块位置（可为 null）
     * @param playerFacing 玩家水平朝向
     * @param playerPitch 玩家垂直视角角度
     * @param isChainKeyDown 连锁按键是否按下
     */
    public void tick(Level level, BlockPos lookingAt, Direction playerFacing, float playerPitch, boolean isChainKeyDown) {
        if (!enabled || !isChainKeyDown || lookingAt == null || level == null) {
            if (!previewBlocks.isEmpty()) {
                previewBlocks = Collections.emptyList();
                currentShapeTranslationKey = "";
                notifyListeners();
            }
            return;
        }
        
        // 节流：基于时间间隔
        long now = System.currentTimeMillis();
        if (now - lastCalculateTime < CALCULATE_INTERVAL_MS) {
            return;
        }
        lastCalculateTime = now;
        
        // 获取配置和形状
        MinerConfig config = ConfigManager.getConfig();
        if (!config.enabled) {
            clearPreview();
            return;
        }
        
        String shapeIdStr = config.selectedShape;
        ChainShape shape = ShapeRegistry.getShapeOrDefault(shapeIdStr);
        if (shape == null) {
            clearPreview();
            return;
        }
        
        BlockState lookingState = level.getBlockState(lookingAt);
        if (lookingState.isAir()) {
            clearPreview();
            return;
        }
        
        // 构建预览上下文
        Direction verticalDir = null;
        if (playerPitch < -45) {
            verticalDir = Direction.UP;
        } else if (playerPitch > 45) {
            verticalDir = Direction.DOWN;
        }
        
        try {
            ShapeContext ctx = new ShapeContext.Builder()
                    .level(level)
                    .originPos(lookingAt)
                    .originState(lookingState)
                    .playerFacing(playerFacing)
                    .playerLookingVertical(verticalDir)
                    .maxBlocks(config.maxBlocks)
                    .maxDistance(config.maxDistance)
                    .allowDiagonal(config.allowDiagonal)
                    // 预览模式使用简单匹配：同类方块
                    .blockMatcher((origin, target) -> !target.isAir() && target.getBlock() == origin.getBlock())
                    .build();
            
            List<BlockPos> preview = shape.getPreviewPositions(ctx);
            this.previewBlocks = preview != null ? Collections.unmodifiableList(new ArrayList<>(preview)) : Collections.emptyList();
            this.currentShapeTranslationKey = shape.getTranslationKey();
            notifyListeners();
        } catch (Exception e) {
            // 预览计算不应影响游戏
            clearPreview();
        }
    }
    
    /**
     * 获取当前预览方块列表
     * 
     * @return 不可变的方块位置列表
     */
    public List<BlockPos> getPreviewBlocks() {
        return previewBlocks;
    }
    
    /**
     * 获取当前形状的翻译键
     * 
     * @return 翻译键字符串
     */
    public String getCurrentShapeTranslationKey() {
        return currentShapeTranslationKey;
    }
    
    /**
     * 获取预览方块数量
     * 
     * @return 方块数量
     */
    public int getPreviewCount() {
        return previewBlocks.size();
    }
    
    /**
     * 设置是否启用预览
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            clearPreview();
        }
    }
    
    /**
     * 添加预览监听器
     * 
     * @param listener 监听器实例
     */
    public void addListener(PreviewListener listener) {
        listeners.add(listener);
    }
    
    /**
     * 移除预览监听器
     * 
     * @param listener 监听器实例
     */
    public void removeListener(PreviewListener listener) {
        listeners.remove(listener);
    }
    
    private void clearPreview() {
        if (!previewBlocks.isEmpty()) {
            previewBlocks = Collections.emptyList();
            currentShapeTranslationKey = "";
            notifyListeners();
        }
    }
    
    private void notifyListeners() {
        for (PreviewListener listener : listeners) {
            try {
                listener.onPreviewChanged(previewBlocks, currentShapeTranslationKey);
            } catch (Exception e) {
                // 监听器异常不应影响主流程
            }
        }
    }
    
    /**
     * 预览变更监听器接口
     */
    @FunctionalInterface
    public interface PreviewListener {
        /**
         * 当预览内容发生变更时调用
         * 
         * @param blocks 新的预览方块列表
         * @param shapeTranslationKey 当前形状翻译键
         */
        void onPreviewChanged(List<BlockPos> blocks, String shapeTranslationKey);
    }
}
