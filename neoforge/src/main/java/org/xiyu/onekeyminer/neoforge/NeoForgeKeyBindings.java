package org.xiyu.onekeyminer.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.preview.ChainPreviewHud;
import org.xiyu.onekeyminer.preview.ChainPreviewManager;

public class NeoForgeKeyBindings {
    public static KeyMapping CHAIN_MINING_KEY;
    public static KeyMapping OPEN_CONFIG;

    private static boolean wasKeyDown = false;
    private static boolean wasConnected = false;
    private static boolean syncPending = true;
    private static int syncRetryDelay;

    public static void register() {
        if (CHAIN_MINING_KEY != null) {
            return;
        }

        CHAIN_MINING_KEY = new KeyMapping(
                "key.onekeyminer.hold",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                KeyMapping.Category.GAMEPLAY
        );
        OPEN_CONFIG = new KeyMapping(
                "key.onekeyminer.config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KeyMapping.Category.GAMEPLAY
        );

        NeoForge.EVENT_BUS.addListener(NeoForgeKeyBindings::onClientTick);
        OneKeyMiner.LOGGER.debug("Registered NeoForge key bindings");
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        if (CHAIN_MINING_KEY == null) {
            register();
        }
        event.register(CHAIN_MINING_KEY);
        event.register(OPEN_CONFIG);
        OneKeyMiner.LOGGER.debug("Registered NeoForge key mappings");
    }

    public static void registerGuiLayer(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                Identifier.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "chain_preview"),
                (guiGraphics, deltaTracker) -> ChainPreviewHud.render(guiGraphics)
        );
    }

    public static void sendCurrentPreferences() {
        if (CHAIN_MINING_KEY == null) {
            syncPending = true;
            syncRetryDelay = 20;
            return;
        }
        boolean sent = NeoForgeClientNetworking.trySyncPreferences(CHAIN_MINING_KEY.isDown());
        syncPending = !sent;
        syncRetryDelay = sent ? 0 : 20;
        if (sent) {
            wasKeyDown = CHAIN_MINING_KEY.isDown();
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean connected = minecraft.player != null && minecraft.getConnection() != null;
        if (!connected) {
            wasConnected = false;
            wasKeyDown = false;
            syncPending = true;
            syncRetryDelay = 0;
            return;
        }

        if (OPEN_CONFIG != null && OPEN_CONFIG.consumeClick()) {
            minecraft.setScreen(NeoForgeConfigScreen.createConfigScreen(minecraft.screen));
        }

        if (CHAIN_MINING_KEY == null) {
            return;
        }

        boolean isKeyDown = CHAIN_MINING_KEY.isDown();
        if (!wasConnected) {
            wasConnected = true;
            syncPending = true;
            syncRetryDelay = 0;
        }

        if (syncPending) {
            if (syncRetryDelay > 0) {
                syncRetryDelay--;
            } else {
                sendCurrentPreferences();
            }
        } else if (isKeyDown != wasKeyDown) {
            if (NeoForgeClientNetworking.trySyncPreferences(isKeyDown)) {
                wasKeyDown = isKeyDown;
            } else {
                syncPending = true;
                syncRetryDelay = 20;
            }
        }

        BlockPos lookingAt = null;
        if (minecraft.hitResult != null && minecraft.hitResult.getType() == HitResult.Type.BLOCK) {
            lookingAt = ((BlockHitResult) minecraft.hitResult).getBlockPos();
        }

        Direction playerFacing = minecraft.player.getDirection();
        float playerPitch = minecraft.player.getXRot();
        ChainPreviewManager.getInstance().tick(minecraft.level, lookingAt, playerFacing, playerPitch, isKeyDown);
    }

}
