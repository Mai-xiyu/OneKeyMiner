package org.xiyu.onekeyminer.preview;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.xiyu.onekeyminer.config.MinerConfig;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PreviewHudControls {
    private PreviewHudControls() {
    }

    public static void addWidgets(MinerConfig config, int screenWidth, int startY,
                                  Consumer<AbstractWidget> addWidget, Runnable refresh) {
        int width = Math.min(200, (screenWidth - 30) / 2);
        int left = screenWidth / 2 - width - 5;
        int right = screenWidth / 2 + 5;
        addToggle(addWidget, left, startY, width, "enabled", () -> config.previewHudEnabled, value -> config.previewHudEnabled = value);
        addToggle(addWidget, right, startY, width, "shape", () -> config.previewHudShowShape, value -> config.previewHudShowShape = value);
        addToggle(addWidget, left, startY + 24, width, "count", () -> config.previewHudShowCount, value -> config.previewHudShowCount = value);
        addToggle(addWidget, right, startY + 24, width, "max", () -> config.previewHudShowMax, value -> config.previewHudShowMax = value);
        addValue(addWidget, left, startY + 48, width, "x", () -> config.previewHudX,
                () -> config.previewHudX = next(config.previewHudX, new int[]{0, 6, 16, 32, 64, 128, 256, 512}));
        addValue(addWidget, right, startY + 48, width, "y", () -> config.previewHudY,
                () -> config.previewHudY = next(config.previewHudY, new int[]{0, 6, 16, 32, 64, 128, 256}));
        addValue(addWidget, left, startY + 72, width, "scale", () -> Math.round(config.previewHudScale * 100) + "%",
                () -> config.previewHudScale = next(Math.round(config.previewHudScale * 100), new int[]{50, 75, 100, 125, 150, 200}) / 100.0f);
        addValue(addWidget, right, startY + 72, width, "alpha", () -> config.previewHudBackgroundAlpha,
                () -> config.previewHudBackgroundAlpha = next(config.previewHudBackgroundAlpha, new int[]{0, 64, 128, 192, 255}));
        addWidget.accept(Button.builder(Component.translatable("config.onekeyminer.option.preview_hud_reset"), button -> {
            config.copyPreviewHudFrom(new MinerConfig());
            refresh.run();
        }).bounds(left, startY + 96, width * 2 + 10, 20).build());
    }

    private static void addToggle(Consumer<AbstractWidget> addWidget, int left, int top, int width,
                                   String key, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        addValue(addWidget, left, top, width, key,
                () -> Component.translatable(getter.get() ? "options.on" : "options.off").getString(),
                () -> setter.accept(!getter.get()));
    }

    private static void addValue(Consumer<AbstractWidget> addWidget, int left, int top, int width,
                                 String key, Supplier<?> getter, Runnable change) {
        addWidget.accept(Button.builder(label(key, getter.get()), button -> {
            change.run();
            button.setMessage(label(key, getter.get()));
        }).bounds(left, top, width, 20).build());
    }

    private static Component label(String key, Object value) {
        return Component.translatable("config.onekeyminer.option.preview_hud_" + key).append(": " + value);
    }

    private static int next(int current, int[] values) {
        for (int value : values) {
            if (value > current) return value;
        }
        return values[0];
    }
}
