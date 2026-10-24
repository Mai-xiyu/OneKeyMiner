package org.xiyu.yee.onekeyminer.client;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.TranslationTextComponent;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.settings.KeyBinding;
import org.xiyu.yee.onekeyminer.api.ChainAPI;
import org.xiyu.onekeyminer.legacy.core.LegacyHudSettings;

public final class HudConfigScreen extends Screen {
    public static final KeyBinding KEY = new KeyBinding("key.onekeyminer.config", org.lwjgl.glfw.GLFW.GLFW_KEY_K, "key.categories.onekeyminer");
    private final Screen parent;
    private LegacyHudSettings draft;
    public HudConfigScreen(Screen parent) {
        super(new TranslationTextComponent("onekeyminer.hud.preview_title"));
        this.parent = parent;
        this.draft = ChainAPI.getPreviewHudSettings();
    }
    @Override
    protected void init() {
        int buttonWidth = Math.min(200, (width - 30) / 2);
        for (int index = 0; index < 8; index++) {
            final int option = index;
            addButton(new Button(width / 2 + (index % 2 == 0 ? -buttonWidth - 5 : 5), 40 + index / 2 * 24,
                    buttonWidth, 20, label(index), button -> { change(option); this.init(minecraft, width, height); }));
        }
        addButton(new Button(width / 2 - buttonWidth - 5, 136, buttonWidth * 2 + 10, 20,
                new TranslationTextComponent("config.onekeyminer.option.preview_hud_reset"), button -> {
                    draft = LegacyHudSettings.defaults(); this.init(minecraft, width, height);
                }));
        addButton(new Button(width / 2 - buttonWidth - 5, height - 30, buttonWidth, 20,
                new TranslationTextComponent("gui.done"), button -> {
                    ChainAPI.setPreviewHudSettings(draft); minecraft.setScreen(parent);
                }));
        addButton(new Button(width / 2 + 5, height - 30, buttonWidth, 20,
                new TranslationTextComponent("gui.cancel"), button -> minecraft.setScreen(parent)));
    }
    private net.minecraft.util.text.ITextComponent label(int index) {
        String[] keys = {"enabled", "shape", "count", "max", "x", "y", "scale", "alpha"};
        Object[] values = {draft.enabled, draft.showShape, draft.showCount, draft.showMax, draft.x, draft.y,
                Math.round(draft.scale * 100) + "%", draft.backgroundAlpha};
        return new TranslationTextComponent("config.onekeyminer.option.preview_hud_" + keys[index]).append(": " + values[index]);
    }
    private void change(int option) {
        draft = new LegacyHudSettings(option == 0 ? !draft.enabled : draft.enabled,
                option == 4 ? (draft.x >= width - 16 ? 0 : draft.x + 8) : draft.x,
                option == 5 ? (draft.y >= height - 16 ? 0 : draft.y + 8) : draft.y,
                option == 6 ? (draft.scale >= 2 ? 0.5f : draft.scale + 0.25f) : draft.scale,
                option == 7 ? (draft.backgroundAlpha >= 255 ? 0 : Math.min(255, draft.backgroundAlpha + 32)) : draft.backgroundAlpha,
                option == 1 ? !draft.showShape : draft.showShape, option == 2 ? !draft.showCount : draft.showCount,
                option == 3 ? !draft.showMax : draft.showMax);
    }
    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTick) {
        renderBackground(matrices);
        drawCenteredString(matrices, font, title, width / 2, 12, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, partialTick);
    }
}
