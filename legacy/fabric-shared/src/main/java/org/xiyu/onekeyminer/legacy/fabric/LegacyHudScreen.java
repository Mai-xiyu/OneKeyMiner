package org.xiyu.onekeyminer.legacy.fabric;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import org.xiyu.onekeyminer.api.OneKeyMinerAPI;
import org.xiyu.onekeyminer.api.OneKeyMinerAPI.PreviewHudSettings;

public final class LegacyHudScreen extends Screen {
    private final Screen parent;
    private boolean enabled;
    private int left;
    private int top;
    private float scale;
    private int alpha;
    private boolean shape;
    private boolean count;
    private boolean max;

    public LegacyHudScreen(Screen parent) {
        super(new TranslatableText("onekeyminer.hud.preview_title"));
        this.parent = parent;
        setDraft(OneKeyMinerAPI.getPreviewHudSettings());
    }

    private void setDraft(PreviewHudSettings settings) {
        enabled = settings.enabled(); left = settings.x(); top = settings.y(); scale = settings.scale();
        alpha = settings.backgroundAlpha(); shape = settings.showShape(); count = settings.showCount(); max = settings.showMax();
    }

    @Override
    protected void init() {
        int columnWidth = Math.min(200, (width - 30) / 2);
        int first = width / 2 - columnWidth - 5;
        int second = width / 2 + 5;
        button(first, 40, columnWidth, "enabled", enabled, () -> enabled = !enabled);
        button(second, 40, columnWidth, "shape", shape, () -> shape = !shape);
        button(first, 64, columnWidth, "count", count, () -> count = !count);
        button(second, 64, columnWidth, "max", max, () -> max = !max);
        button(first, 88, columnWidth, "x", left, () -> left = left >= width - 16 ? 0 : left + 8);
        button(second, 88, columnWidth, "y", top, () -> top = top >= height - 16 ? 0 : top + 8);
        button(first, 112, columnWidth, "scale", Math.round(scale * 100) + "%", () -> scale = scale >= 2 ? 0.5f : scale + 0.25f);
        button(second, 112, columnWidth, "alpha", alpha, () -> alpha = alpha >= 255 ? 0 : Math.min(255, alpha + 32));
        addButton(new ButtonWidget(first, 136, columnWidth * 2 + 10, 20,
                new TranslatableText("config.onekeyminer.option.preview_hud_reset"), button -> {
                    setDraft(PreviewHudSettings.defaults()); clearAndInit();
                }));
        addButton(new ButtonWidget(first, height - 30, columnWidth, 20, new TranslatableText("gui.done"), button -> {
            OneKeyMinerAPI.setPreviewHudSettings(new PreviewHudSettings(enabled, left, top, scale, alpha, shape, count, max));
            client.openScreen(parent);
        }));
        addButton(new ButtonWidget(second, height - 30, columnWidth, 20, new TranslatableText("gui.cancel"), button -> client.openScreen(parent)));
    }

    private void button(int left, int top, int width, String key, Object value, Runnable action) {
        addButton(new ButtonWidget(left, top, width, 20,
                new TranslatableText("config.onekeyminer.option.preview_hud_" + key).append(new LiteralText(": " + value)), button -> {
                    action.run(); clearAndInit();
                }));
    }

    private void clearAndInit() {
        this.init(client, width, height);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        drawCenteredText(matrices, textRenderer, title, width / 2, 12, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
    }
}
