package org.xiyu.onekeyminer.preview;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.xiyu.onekeyminer.config.ConfigManager;

/**
 * Lightweight HUD panel for the current chain preview.
 */
public class ChainPreviewHud {
    private static final int HUD_X = 6;
    private static final int HUD_Y = 6;
    private static final int LINE_SPACING = 12;
    private static final int BG_COLOR = 0x80000000;
    private static final int TITLE_COLOR = 0xFF55FFFF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int COUNT_COLOR = 0xFF55FF55;

    public static void render(GuiGraphics guiGraphics) {
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
        Component maxLine = Component.translatable("onekeyminer.hud.max", ConfigManager.getConfig().maxBlocks);

        int panelWidth = Math.max(
                Math.max(font.width(titleLine), font.width(shapeLine)),
                Math.max(font.width(countLine), font.width(maxLine))
        ) + 10;
        int panelHeight = LINE_SPACING * 4 + 6;

        guiGraphics.fill(HUD_X - 2, HUD_Y - 2, HUD_X + panelWidth + 2, HUD_Y + panelHeight, BG_COLOR);
        int y = HUD_Y;
        guiGraphics.drawString(font, titleLine, HUD_X, y, TITLE_COLOR);
        y += LINE_SPACING;
        guiGraphics.drawString(font, shapeLine, HUD_X, y, TEXT_COLOR);
        y += LINE_SPACING;
        guiGraphics.drawString(font, countLine, HUD_X, y, COUNT_COLOR);
        y += LINE_SPACING;
        guiGraphics.drawString(font, maxLine, HUD_X, y, TEXT_COLOR);
    }
}
