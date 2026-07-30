package org.xiyu.onekeyminer.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigManager;

/**
 * Fabric client key bindings and negotiated C2S preference sync.
 */
@Environment(EnvType.CLIENT)
public final class KeyBindings {
    public static KeyMapping CHAIN_MINING_KEY;
    public static KeyMapping OPEN_CONFIG;

    private static boolean wasKeyDown;
    private static boolean wasConnected;
    private static boolean syncPending = true;
    private static int syncRetryDelay;

    private KeyBindings() {
    }

    public static void register() {
        CHAIN_MINING_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.onekeyminer.hold",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                "key.categories.onekeyminer"
        ));

        OPEN_CONFIG = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.onekeyminer.config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "key.categories.onekeyminer"
        ));

        registerKeyHandler();
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
        try {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.getConnection() == null
                    || !ClientPlayNetworking.canSend(FabricPayloads.ClientPreferencesPayload.TYPE)) {
                return false;
            }

            var config = ConfigManager.getConfigSnapshot();
            boolean holding = CHAIN_MINING_KEY != null && CHAIN_MINING_KEY.isDown();
            ClientPlayNetworking.send(new FabricPayloads.ClientPreferencesPayload(
                    FabricPayloads.WIRE_VERSION,
                    holding,
                    config.selectedShape,
                    config.teleportDrops,
                    config.teleportExp
            ));
            return true;
        } catch (RuntimeException e) {
            OneKeyMiner.LOGGER.debug("Failed to send Fabric client preferences: {}", e.getMessage());
            return false;
        }
    }

    private static void registerKeyHandler() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean connected = client.getConnection() != null && client.player != null;
            if (!connected) {
                wasConnected = false;
                wasKeyDown = false;
                syncPending = true;
                syncRetryDelay = 0;
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

            while (OPEN_CONFIG.consumeClick()) {
                client.setScreen(new FabricConfigScreen(client.screen));
            }
        });
    }
}
