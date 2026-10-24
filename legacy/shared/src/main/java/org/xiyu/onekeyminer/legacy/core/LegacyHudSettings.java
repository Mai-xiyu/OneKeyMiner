package org.xiyu.onekeyminer.legacy.core;

public final class LegacyHudSettings {
    public final boolean enabled;
    public final int x;
    public final int y;
    public final float scale;
    public final int backgroundAlpha;
    public final boolean showShape;
    public final boolean showCount;
    public final boolean showMax;

    public LegacyHudSettings(boolean enabled, int x, int y, float scale, int backgroundAlpha,
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

    public static LegacyHudSettings defaults() {
        return new LegacyHudSettings(true, 6, 6, 1, 128, true, true, true);
    }
}
