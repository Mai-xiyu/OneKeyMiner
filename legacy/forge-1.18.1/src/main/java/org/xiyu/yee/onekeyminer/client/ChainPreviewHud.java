package org.xiyu.yee.onekeyminer.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraftforge.fml.common.Mod;
import org.xiyu.yee.onekeyminer.Onekeyminer;
import org.xiyu.yee.onekeyminer.api.ChainAPI;
import org.xiyu.yee.onekeyminer.api.ChainPositionStore;
import org.xiyu.yee.onekeyminer.config.CommonConfig;
import org.xiyu.onekeyminer.legacy.core.LegacyHudSettings;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Onekeyminer.MODID, value = Dist.CLIENT)
public final class ChainPreviewHud {
    private ChainPreviewHud() {
    }

    @SubscribeEvent
    public static void render(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) return;
        String playerId = minecraft.player.getUUID().toString();
        if (!ChainPositionStore.isChainModeActive(playerId)) return;
        int count = Math.max(ChainPositionStore.getMiningPositions(playerId).size(),
                Math.max(ChainPositionStore.getInteractionPositions(playerId).size(),
                        ChainPositionStore.getPlantingPositions(playerId).size()));
        if (count <= 0) return;
        LegacyHudSettings settings;
        try {
            settings = ChainAPI.getPreviewHudSettings();
        } catch (RuntimeException exception) {
            settings = LegacyHudSettings.defaults();
        }
        if (!settings.enabled || !settings.showShape && !settings.showCount && !settings.showMax) return;
        List<String> lines = new ArrayList<>();
        lines.add(net.minecraft.client.resources.language.I18n.get("onekeyminer.hud.preview_title"));
        if (settings.showShape) lines.add(net.minecraft.client.resources.language.I18n.get("onekeyminer.hud.shape",
                net.minecraft.client.resources.language.I18n.get("onekeyminer.shape.amorphous")));
        if (settings.showCount) lines.add(net.minecraft.client.resources.language.I18n.get("onekeyminer.hud.count", count));
        if (settings.showMax) lines.add(net.minecraft.client.resources.language.I18n.get("onekeyminer.hud.max", minecraft.player.isCreative()
                ? CommonConfig.INSTANCE.maxBlocksInChainCreative.get() : CommonConfig.INSTANCE.maxBlocksInChain.get()));
        int width = 0;
        for (String line : lines) width = Math.max(width, minecraft.font.width(line));
        width += 10;
        int height = lines.size() * 12 + 6;
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();
        float scale = Math.min(settings.scale, Math.min(screenWidth / (float) (width + 4), screenHeight / (float) (height + 2)));
        int left = Math.max(2, Math.min(settings.x, (int) (screenWidth - (width + 2) * scale)));
        int top = Math.max(2, Math.min(settings.y, (int) (screenHeight - height * scale)));
        PoseStack matrices = event.getMatrixStack();
        matrices.pushPose();
        try {
            matrices.translate(left, top, 0);
            matrices.scale(scale, scale, 1);
            if (settings.backgroundAlpha > 0) GuiComponent.fill(matrices, -2, -2, width + 2, height, settings.backgroundAlpha << 24);
            for (int index = 0; index < lines.size(); index++) {
                minecraft.font.drawShadow(matrices, lines.get(index), 0, index * 12, index == 0 ? 0xFF55FFFF : 0xFFFFFFFF);
            }
        } finally {
            matrices.popPose();
        }
    }
}
