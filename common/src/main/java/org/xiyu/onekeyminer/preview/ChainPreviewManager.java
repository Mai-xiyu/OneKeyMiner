package org.xiyu.onekeyminer.preview;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * Client-side preview state and throttled shape calculation.
 */
public class ChainPreviewManager {
    private static ChainPreviewManager instance;
    private static final long CALCULATE_INTERVAL_MS = 200;

    private volatile List<BlockPos> previewBlocks = Collections.emptyList();
    private volatile String currentShapeTranslationKey = "";
    private long lastCalculateTime = 0;
    private boolean enabled = true;
    private final List<PreviewListener> listeners = new CopyOnWriteArrayList<>();

    private ChainPreviewManager() {
    }

    public static ChainPreviewManager getInstance() {
        if (instance == null) {
            instance = new ChainPreviewManager();
        }
        return instance;
    }

    public void tick(Level level, BlockPos lookingAt, Direction playerFacing, float playerPitch, boolean isChainKeyDown) {
        if (!enabled || !isChainKeyDown || lookingAt == null || level == null) {
            clearPreview();
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastCalculateTime < CALCULATE_INTERVAL_MS) {
            return;
        }
        lastCalculateTime = now;

        MinerConfig config = ConfigManager.getConfig();
        if (!config.enabled) {
            clearPreview();
            return;
        }

        ChainShape shape = ShapeRegistry.getShapeOrDefault(config.selectedShape);
        if (shape == null) {
            clearPreview();
            return;
        }

        BlockState lookingState = level.getBlockState(lookingAt);
        if (lookingState.isAir()) {
            clearPreview();
            return;
        }

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
                    .blockMatcher((origin, target) -> !target.isAir() && target.getBlock() == origin.getBlock())
                    .build();

            List<BlockPos> preview = shape.getPreviewPositions(ctx);
            previewBlocks = preview != null
                    ? Collections.unmodifiableList(new ArrayList<>(preview))
                    : Collections.emptyList();
            currentShapeTranslationKey = shape.getTranslationKey();
            notifyListeners();
        } catch (Exception ignored) {
            clearPreview();
        }
    }

    public List<BlockPos> getPreviewBlocks() {
        return previewBlocks;
    }

    public String getCurrentShapeTranslationKey() {
        return currentShapeTranslationKey;
    }

    public int getPreviewCount() {
        return previewBlocks.size();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            clearPreview();
        }
    }

    public void addListener(PreviewListener listener) {
        listeners.add(listener);
    }

    public boolean removeListener(PreviewListener listener) {
        return listeners.remove(listener);
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
            } catch (Exception ignored) {
            }
        }
    }

    @FunctionalInterface
    public interface PreviewListener {
        void onPreviewChanged(List<BlockPos> blocks, String shapeTranslationKey);
    }
}
