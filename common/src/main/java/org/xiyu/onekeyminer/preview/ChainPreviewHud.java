package org.xiyu.onekeyminer.preview;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.network.ClientPreferenceSession;

/**
 * Lightweight HUD panel for the current chain preview.
 */
public class ChainPreviewHud {
    private static final int LINE_SPACING = 12;
    private static final int TITLE_COLOR = 0xFF55FFFF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int COUNT_COLOR = 0xFF55FF55;

    public static void render(GuiGraphics guiGraphics) {
        var config = ConfigManager.getConfig();
        if (!config.previewHudEnabled) {
            return;
        }
        ChainPreviewManager manager = ChainPreviewManager.getInstance();
        int count = manager.getPreviewCount();
        if (count <= 0) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        String shapeKey = manager.getCurrentShapeTranslationKey();
        Component shapeName = shapeKey != null && !shapeKey.isEmpty()
                ? Component.translatable(shapeKey)
                : Component.literal("?");

        Component titleLine = Component.translatable("onekeyminer.hud.preview_title");
        Component shapeLine = Component.translatable("onekeyminer.hud.shape", shapeName);
        Component countLine = Component.translatable("onekeyminer.hud.count", count);
        int effectiveLimit = ClientPreferenceSession.resolvePreviewPolicy(
                config
        ).maxBlocks() + 1;
        Component maxLine = Component.translatable(
                "onekeyminer.hud.max",
                effectiveLimit
        );

        java.util.List<Component> lines = new java.util.ArrayList<>();
        lines.add(titleLine);
        if (config.previewHudShowShape) lines.add(shapeLine);
        if (config.previewHudShowCount) lines.add(countLine);
        if (config.previewHudShowMax) lines.add(maxLine);
        if (lines.size() == 1) return;

        int panelWidth = lines.stream().mapToInt(font::width).max().orElse(0) + 10;
        int panelHeight = LINE_SPACING * lines.size() + 6;
        float requestedScale = Float.isFinite(config.previewHudScale) ? config.previewHudScale : 1.0f;
        float scale = Math.max(0.5f, Math.min(2.0f, requestedScale));
        scale = Math.min(scale, Math.min(guiGraphics.guiWidth() / (float) (panelWidth + 4),
                guiGraphics.guiHeight() / (float) (panelHeight + 2)));
        int x = Math.max(2, Math.min(config.previewHudX, (int) (guiGraphics.guiWidth() - (panelWidth + 2) * scale)));
        int y = Math.max(2, Math.min(config.previewHudY, (int) (guiGraphics.guiHeight() - panelHeight * scale)));

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1);
        int backgroundAlpha = Math.max(0, Math.min(255, config.previewHudBackgroundAlpha));
        if (backgroundAlpha > 0) guiGraphics.fill(-2, -2, panelWidth + 2, panelHeight, backgroundAlpha << 24);
        int lineY = 0;
        for (int i = 0; i < lines.size(); i++) {
            int color = i == 0 ? TITLE_COLOR : (config.previewHudShowCount && lines.get(i).equals(countLine) ? COUNT_COLOR : TEXT_COLOR);
            guiGraphics.drawString(font, lines.get(i), 0, lineY, color);
            lineY += LINE_SPACING;
        }
        guiGraphics.pose().popPose();
    }
}
