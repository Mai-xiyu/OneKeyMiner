package org.xiyu.onekeyminer.legacy.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import org.xiyu.onekeyminer.api.OneKeyMinerAPI;
import org.xiyu.onekeyminer.api.OneKeyMinerAPI.PreviewHudSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LegacyFabricClient implements ClientModInitializer {
    private static KeyBinding activation;
    private static boolean sentActive;
    private static int ticks;
    private static int previewLimit;
    private static int previewDistance;
    private static boolean previewDiagonal;
    private static boolean policyReady;

    @Override
    public void onInitializeClient() {
        activation = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.onekeyminer.hold",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT, "category.onekeyminer"));
        KeyBinding settingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.onekeyminer.config",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.onekeyminer"));
        ClientPlayNetworking.registerGlobalReceiver(LegacyFabricMiner.POLICY_CHANNEL, (client, handler, buffer, responseSender) -> {
            if (buffer.readableBytes() != 9) return;
            int limit = buffer.readInt();
            int distance = buffer.readInt();
            boolean diagonal = buffer.readBoolean();
            if (limit < 0 || limit > 10240 || distance < 1 || distance > 128) return;
            client.execute(() -> {
                previewLimit = limit;
                previewDistance = distance;
                previewDiagonal = diagonal;
                policyReady = limit > 0;
            });
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            policyReady = false;
            sentActive = false;
            OneKeyMinerAPI.updatePreview(Collections.emptyList());
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ticks++;
            while (settingsKey.wasPressed()) client.openScreen(new LegacyHudScreen(client.currentScreen));
            boolean active = client.player != null && client.world != null && client.currentScreen == null && activation.isPressed();
            if (ClientPlayNetworking.canSend(LegacyFabricMiner.KEY_CHANNEL) && (active != sentActive || ticks % 40 == 0)) {
                PacketByteBuf buffer = PacketByteBufs.create();
                buffer.writeBoolean(active);
                ClientPlayNetworking.send(LegacyFabricMiner.KEY_CHANNEL, buffer);
                sentActive = active;
            }
            if (!active || !policyReady || !(client.crosshairTarget instanceof BlockHitResult)) {
                OneKeyMinerAPI.updatePreview(Collections.emptyList());
            } else if (ticks % 4 == 0) {
                BlockPos origin = ((BlockHitResult) client.crosshairTarget).getBlockPos();
                if (client.world.getBlockState(origin).isAir()) {
                    OneKeyMinerAPI.updatePreview(Collections.emptyList());
                } else {
                    OneKeyMinerAPI.updatePreview(LegacyFabricMiner.targets(client.world, origin,
                            client.world.getBlockState(origin), previewLimit, previewDistance, previewDiagonal));
                }
            }
        });
        HudRenderCallback.EVENT.register((matrices, tickDelta) -> render(matrices));
    }

    private static void render(MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();
        PreviewHudSettings settings = OneKeyMinerAPI.getPreviewHudSettings();
        if (!settings.enabled() || client.options.hudHidden || OneKeyMinerAPI.getPreviewBlocks().isEmpty()) return;
        List<String> lines = new ArrayList<>();
        lines.add(new TranslatableText("onekeyminer.hud.preview_title").getString());
        if (settings.showShape()) lines.add(new TranslatableText("onekeyminer.hud.shape", new TranslatableText("onekeyminer.shape.amorphous")).getString());
        if (settings.showCount()) lines.add(new TranslatableText("onekeyminer.hud.count", OneKeyMinerAPI.getPreviewBlocks().size()).getString());
        if (settings.showMax()) lines.add(new TranslatableText("onekeyminer.hud.max", previewLimit).getString());
        if (lines.size() == 1) return;
        int width = 0;
        for (String line : lines) width = Math.max(width, client.textRenderer.getWidth(line));
        width += 10;
        int height = lines.size() * 12 + 6;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        float scale = Math.min(settings.scale(), Math.min(screenWidth / (float) (width + 4), screenHeight / (float) (height + 2)));
        int left = Math.max(2, Math.min(settings.x(), (int) (screenWidth - (width + 2) * scale)));
        int top = Math.max(2, Math.min(settings.y(), (int) (screenHeight - height * scale)));
        matrices.push();
        try {
            matrices.translate(left, top, 0);
            matrices.scale(scale, scale, 1);
            if (settings.backgroundAlpha() > 0) DrawableHelper.fill(matrices, -2, -2, width + 2, height, settings.backgroundAlpha() << 24);
            for (int index = 0; index < lines.size(); index++) {
                client.textRenderer.drawWithShadow(matrices, lines.get(index), 0, index * 12, index == 0 ? 0xFF55FFFF : 0xFFFFFFFF);
            }
        } finally {
            matrices.pop();
        }
    }
}
