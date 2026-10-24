package org.xiyu.yee.onekeyminer.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import org.xiyu.yee.onekeyminer.api.ChainAPI;
import org.xiyu.onekeyminer.legacy.core.LegacyHudSettings;

public final class HudConfigScreen extends GuiScreen {
    public static final KeyBinding KEY = new KeyBinding("key.onekeyminer.config", org.lwjgl.input.Keyboard.KEY_K, "key.categories.onekeyminer");
    private final GuiScreen parent;
    private LegacyHudSettings draft;

    public HudConfigScreen(GuiScreen parent) {
        this.parent = parent;
        this.draft = ChainAPI.getPreviewHudSettings();
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int buttonWidth = Math.min(200, (width - 30) / 2);
        String[] keys = {"enabled", "shape", "count", "max", "x", "y", "scale", "alpha"};
        Object[] values = {draft.enabled, draft.showShape, draft.showCount, draft.showMax,
                draft.x, draft.y, Math.round(draft.scale * 100) + "%", draft.backgroundAlpha};
        for (int index = 0; index < keys.length; index++) {
            buttonList.add(new GuiButton(index, width / 2 + (index % 2 == 0 ? -buttonWidth - 5 : 5),
                    40 + index / 2 * 24, buttonWidth, 20,
                    I18n.format("config.onekeyminer.option.preview_hud_" + keys[index]) + ": " + values[index]));
        }
        buttonList.add(new GuiButton(8, width / 2 - buttonWidth - 5, 136, buttonWidth * 2 + 10, 20,
                I18n.format("config.onekeyminer.option.preview_hud_reset")));
        buttonList.add(new GuiButton(9, width / 2 - buttonWidth - 5, height - 30, buttonWidth, 20, I18n.format("gui.done")));
        buttonList.add(new GuiButton(10, width / 2 + 5, height - 30, buttonWidth, 20, I18n.format("gui.cancel")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        int option = button.id;
        if (option == 10) { mc.displayGuiScreen(parent); return; }
        if (option == 9) { ChainAPI.setPreviewHudSettings(draft); mc.displayGuiScreen(parent); return; }
        if (option == 8) {
            draft = LegacyHudSettings.defaults();
        } else {
            draft = new LegacyHudSettings(option == 0 ? !draft.enabled : draft.enabled,
                    option == 4 ? (draft.x >= width - 16 ? 0 : draft.x + 8) : draft.x,
                    option == 5 ? (draft.y >= height - 16 ? 0 : draft.y + 8) : draft.y,
                    option == 6 ? (draft.scale >= 2 ? 0.5f : draft.scale + 0.25f) : draft.scale,
                    option == 7 ? (draft.backgroundAlpha >= 255 ? 0 : Math.min(255, draft.backgroundAlpha + 32)) : draft.backgroundAlpha,
                    option == 1 ? !draft.showShape : draft.showShape, option == 2 ? !draft.showCount : draft.showCount,
                    option == 3 ? !draft.showMax : draft.showMax);
        }
        initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, I18n.format("onekeyminer.hud.preview_title"), width / 2, 12, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
