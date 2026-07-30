package org.xiyu.onekeyminer.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiOverlayEvent;
import net.neoforged.neoforge.client.gui.overlay.VanillaGuiOverlay;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;
import org.xiyu.onekeyminer.preview.ChainPreviewHud;
import org.xiyu.onekeyminer.preview.ChainPreviewManager;

/** NeoForge physical-client key bindings and connection state. */
@OnlyIn(Dist.CLIENT)
public final class NeoForgeKeyBindings {

    public static KeyMapping CHAIN_MINING_KEY;
    public static KeyMapping OPEN_CONFIG;

    private static boolean wasKeyDown;

    private NeoForgeKeyBindings() {
    }

    public static void register() {
        if (CHAIN_MINING_KEY != null) {
            return;
        }
        CHAIN_MINING_KEY = new KeyMapping(
                "key.onekeyminer.hold",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                "key.categories.onekeyminer"
        );
        OPEN_CONFIG = new KeyMapping(
                "key.onekeyminer.config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "key.categories.onekeyminer"
        );

        NeoForge.EVENT_BUS.addListener(NeoForgeKeyBindings::onClientTick);
        NeoForge.EVENT_BUS.addListener(NeoForgeKeyBindings::onRenderGui);
        NeoForge.EVENT_BUS.addListener(NeoForgeKeyBindings::onClientLogin);
        NeoForge.EVENT_BUS.addListener(NeoForgeKeyBindings::onClientLogout);
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        register();
        event.register(CHAIN_MINING_KEY);
        event.register(OPEN_CONFIG);
    }

    public static boolean isChainKeyDown() {
        return CHAIN_MINING_KEY != null && CHAIN_MINING_KEY.isDown();
    }

    public static void resetConnectionState() {
        wasKeyDown = false;
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        if (OPEN_CONFIG.consumeClick()) {
            minecraft.setScreen(NeoForgeConfigScreen.createConfigScreen(minecraft.screen));
        }

        boolean keyDown = CHAIN_MINING_KEY.isDown();
        if (keyDown != wasKeyDown) {
            wasKeyDown = keyDown;
            NeoForgeClientSetup.sendCurrentState();
        }

        BlockPos lookingAt = null;
        if (minecraft.hitResult != null
                && minecraft.hitResult.getType() == HitResult.Type.BLOCK) {
            lookingAt = ((BlockHitResult) minecraft.hitResult).getBlockPos();
        }
        Direction facing = minecraft.player.getDirection();
        ChainPreviewManager.getInstance().tick(
                minecraft.level,
                lookingAt,
                facing,
                minecraft.player.getXRot(),
                keyDown
        );
    }

    private static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            ChainPreviewHud.render(event.getGuiGraphics());
        }
    }

    private static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        resetConnectionState();
        NeoForgeClientSetup.sendCurrentState();
    }

    private static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        resetConnectionState();
    }
}
