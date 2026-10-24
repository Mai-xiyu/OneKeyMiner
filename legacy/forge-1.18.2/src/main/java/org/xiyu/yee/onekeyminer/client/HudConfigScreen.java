package org.xiyu.yee.onekeyminer.client;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.TranslatableComponent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.KeyMapping;
import org.xiyu.yee.onekeyminer.api.ChainAPI;
import org.xiyu.onekeyminer.legacy.core.LegacyHudSettings;

public final class HudConfigScreen extends Screen {
    public static final KeyMapping KEY = new KeyMapping("key.onekeyminer.config", org.lwjgl.glfw.GLFW.GLFW_KEY_K, "key.categories.onekeyminer");
    private final Screen parent;
    private LegacyHudSettings draft;
    public HudConfigScreen(Screen parent) {
        super(new TranslatableComponent("onekeyminer.hud.preview_title"));
        this.parent = parent;
        this.draft = ChainAPI.getPreviewHudSettings();
    }
    @Override
    protected void init() {
        int buttonWidth = Math.min(200, (width - 30) / 2);
        for (int index = 0; index < 8; index++) {
            final int option = index;
            addRenderableWidget(new Button(width / 2 + (index % 2 == 0 ? -buttonWidth - 5 : 5), 40 + index / 2 * 24,
                    buttonWidth, 20, label(index), button -> { change(option); this.init(minecraft, width, height); }));
        }
        addRenderableWidget(new Button(width / 2 - buttonWidth - 5, 136, buttonWidth * 2 + 10, 20,
                new TranslatableComponent("config.onekeyminer.option.preview_hud_reset"), button -> {
                    draft = LegacyHudSettings.defaults(); this.init(minecraft, width, height);
                }));
        addRenderableWidget(new Button(width / 2 - buttonWidth - 5, height - 30, buttonWidth, 20,
                new TranslatableComponent("gui.done"), button -> {
                    ChainAPI.setPreviewHudSettings(draft); minecraft.setScreen(parent);
                }));
        addRenderableWidget(new Button(width / 2 + 5, height - 30, buttonWidth, 20,
                new TranslatableComponent("gui.cancel"), button -> minecraft.setScreen(parent)));
    }
    private net.minecraft.network.chat.Component label(int index) {
        String[] keys = {"enabled", "shape", "count", "max", "x", "y", "scale", "alpha"};
        Object[] values = {draft.enabled, draft.showShape, draft.showCount, draft.showMax, draft.x, draft.y,
                Math.round(draft.scale * 100) + "%", draft.backgroundAlpha};
        return new TranslatableComponent("config.onekeyminer.option.preview_hud_" + keys[index]).append(": " + values[index]);
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
    public void render(PoseStack matrices, int mouseX, int mouseY, float partialTick) {
        renderBackground(matrices);
        drawCenteredString(matrices, font, title, width / 2, 12, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, partialTick);
    }
}
