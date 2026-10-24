package org.xiyu.onekeyminer.api;

import net.minecraft.util.math.BlockPos;
import org.xiyu.onekeyminer.legacy.fabric.LegacyFabricConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class OneKeyMinerAPI {
    private static volatile List<BlockPos> preview = Collections.emptyList();
    private static final List<PreviewListener> LISTENERS = new CopyOnWriteArrayList<>();

    private OneKeyMinerAPI() {
    }

    public static PreviewHudSettings getPreviewHudSettings() {
        return LegacyFabricConfig.get().previewHud;
    }

    public static void setPreviewHudSettings(PreviewHudSettings settings) {
        LegacyFabricConfig.setHud(Objects.requireNonNull(settings, "settings"));
    }

    public static void resetPreviewHudSettings() {
        setPreviewHudSettings(PreviewHudSettings.defaults());
    }

    public static List<BlockPos> getPreviewBlocks() {
        return preview;
    }

    public static void addPreviewListener(PreviewListener listener) {
        LISTENERS.add(Objects.requireNonNull(listener, "listener"));
    }

    public static boolean removePreviewListener(PreviewListener listener) {
        return LISTENERS.remove(listener);
    }

    public static void updatePreview(List<BlockPos> blocks) {
        List<BlockPos> snapshot = Collections.unmodifiableList(new ArrayList<>(blocks));
        if (snapshot.equals(preview)) return;
        preview = snapshot;
        for (PreviewListener listener : LISTENERS) {
            try {
                listener.onPreviewChanged(snapshot);
            } catch (RuntimeException exception) {
                org.xiyu.onekeyminer.legacy.fabric.LegacyFabricMiner.LOGGER.warn("Preview listener failed", exception);
            }
        }
    }

    public interface PreviewListener {
        void onPreviewChanged(List<BlockPos> blocks);
    }

    public static final class PreviewHudSettings {
        private final boolean enabled;
        private final int x;
        private final int y;
        private final float scale;
        private final int backgroundAlpha;
        private final boolean showShape;
        private final boolean showCount;
        private final boolean showMax;

        public PreviewHudSettings(boolean enabled, int x, int y, float scale, int backgroundAlpha,
                                  boolean showShape, boolean showCount, boolean showMax) {
            this.enabled = enabled;
            this.x = Math.max(0, x);
            this.y = Math.max(0, y);
            this.scale = Float.isFinite(scale) ? Math.max(0.5f, Math.min(2, scale)) : 1;
            this.backgroundAlpha = Math.max(0, Math.min(255, backgroundAlpha));
            this.showShape = showShape;
            this.showCount = showCount;
            this.showMax = showMax;
        }

        public boolean enabled() { return enabled; }
        public int x() { return x; }
        public int y() { return y; }
        public float scale() { return scale; }
        public int backgroundAlpha() { return backgroundAlpha; }
        public boolean showShape() { return showShape; }
        public boolean showCount() { return showCount; }
        public boolean showMax() { return showMax; }

        public static PreviewHudSettings defaults() {
            return new PreviewHudSettings(true, 6, 6, 1, 128, true, true, true);
        }
    }
}
