package org.xiyu.onekeyminer.preview;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.shape.ChainShape;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

import java.util.List;

/**
 * 连锁预览 HUD 渲染器
 * 
 * <p>在游戏画面左上角显示当前形状名称和预计挖掘方块数量。
 * 仅在连锁按键按下且有预览方块时显示。</p>
 * 
 * <p>各平台（Fabric/Forge）需在 HUD 渲染回调中调用 {@link #render(GuiGraphics)}。</p>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.0
 * @since Minecraft 1.20.1
 */
public class ChainPreviewHud {
    
    /** HUD 左上角 X 偏移 */
    private static final int HUD_X = 6;
    
    /** HUD Y 起始偏移 */
    private static final int HUD_Y = 6;
    
    /** 行间距 */
    private static final int LINE_SPACING = 12;
    
    /** 背景色（半透明黑色） */
    private static final int BG_COLOR = 0x80000000;
    
    /** 标题文字颜色 */
    private static final int TITLE_COLOR = 0xFF55FFFF;
    
    /** 正文文字颜色 */
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    
    /** 数量文字颜色 */
    private static final int COUNT_COLOR = 0xFF55FF55;
    
    /**
     * 渲染 HUD 覆盖层
     * 
     * <p>应在各平台的 HUD 渲染事件中调用此方法。</p>
     * 
     * @param guiGraphics 渲染上下文
     */
    public static void render(GuiGraphics guiGraphics) {
        ChainPreviewManager manager = ChainPreviewManager.getInstance();
        int count = manager.getPreviewCount();
        if (count <= 0) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        
        // 获取形状信息
        String shapeKey = manager.getCurrentShapeTranslationKey();
        Component shapeName;
        if (shapeKey != null && !shapeKey.isEmpty()) {
            shapeName = Component.translatable(shapeKey);
        } else {
            shapeName = Component.literal("?");
        }
        
        // 组装显示文本
        Component titleLine = Component.translatable("onekeyminer.hud.preview_title");
        Component shapeLine = Component.translatable("onekeyminer.hud.shape", shapeName);
        Component countLine = Component.translatable("onekeyminer.hud.count", count);
        Component maxLine = Component.translatable("onekeyminer.hud.max", ConfigManager.getConfig().maxBlocks);
        
        // 计算面板大小
        int titleWidth = font.width(titleLine);
        int shapeWidth = font.width(shapeLine);
        int countWidth = font.width(countLine);
        int maxWidth = font.width(maxLine);
        int panelWidth = Math.max(Math.max(titleWidth, shapeWidth), Math.max(countWidth, maxWidth)) + 10;
        int panelHeight = LINE_SPACING * 4 + 6;
        
        // 绘制半透明背景
        guiGraphics.fill(HUD_X - 2, HUD_Y - 2, HUD_X + panelWidth + 2, HUD_Y + panelHeight, BG_COLOR);
        
        // 绘制文本
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
