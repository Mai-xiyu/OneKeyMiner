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
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.preview.ChainPreviewHud;
import org.xiyu.onekeyminer.preview.ChainPreviewManager;

@OnlyIn(Dist.CLIENT)
public class NeoForgeKeyBindings {
    public static KeyMapping CHAIN_MINING_KEY;
    public static KeyMapping OPEN_CONFIG;

    private static boolean wasKeyDown;
    private static boolean wasConnected;
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
                "key.categories.onekeyminer"
        );
        OPEN_CONFIG = new KeyMapping(
                "key.onekeyminer.config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "key.categories.onekeyminer"
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
                ResourceLocation.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "chain_preview"),
                (guiGraphics, deltaTracker) -> ChainPreviewHud.render(guiGraphics)
        );
    }

    public static void sendCurrentPreferences() {
        boolean sent = trySendCurrentPreferences();
        syncPending = !sent;
        syncRetryDelay = sent ? 0 : 20;
        if (sent && CHAIN_MINING_KEY != null) {
            wasKeyDown = CHAIN_MINING_KEY.isDown();
        }
    }

    private static boolean trySendCurrentPreferences() {
        if (CHAIN_MINING_KEY == null) {
            return false;
        }

        try {
            Minecraft minecraft = Minecraft.getInstance();
            var connection = minecraft.getConnection();
            if (connection == null
                    || !NetworkRegistry.hasChannel(
                            connection,
                            NeoForgeNetworking.ClientPreferencesPayload.ID
                    )) {
                return false;
            }

            var config = ConfigManager.getConfig();
            PacketDistributor.sendToServer(new NeoForgeNetworking.ClientPreferencesPayload(
                    NeoForgeNetworking.WIRE_VERSION,
                    CHAIN_MINING_KEY.isDown(),
                    config.selectedShape,
                    config.teleportDrops,
                    config.teleportExp
            ));
            return true;
        } catch (RuntimeException ignored) {
            return false;
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
            if (trySendCurrentPreferences()) {
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
