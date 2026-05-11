package org.xiyu.onekeyminer.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigManager;

/**
 * Fabric client key bindings and C2S sync.
 */
@Environment(EnvType.CLIENT)
public class KeyBindings {
    public static KeyMapping CHAIN_MINING_KEY;
    public static KeyMapping OPEN_CONFIG;

    private static boolean wasKeyDown = false;

    public static final ResourceLocation CHAIN_KEY_STATE_ID = FabricNetworkingIds.CHAIN_KEY_STATE_ID;
    public static final ResourceLocation TELEPORT_SETTINGS_ID = FabricNetworkingIds.TELEPORT_SETTINGS_ID;

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

    public static void sendTeleportSettings(boolean teleportDrops, boolean teleportExp) {
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.getConnection() != null) {
                FriendlyByteBuf buf = PacketByteBufs.create();
                buf.writeBoolean(teleportDrops);
                buf.writeBoolean(teleportExp);
                ClientPlayNetworking.send(TELEPORT_SETTINGS_ID, buf);
            }
        } catch (Exception e) {
            OneKeyMiner.LOGGER.debug("Failed to send teleport settings: {}", e.getMessage());
        }
    }

    private static void registerKeyHandler() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean isKeyDown = CHAIN_MINING_KEY.isDown();
            if (isKeyDown != wasKeyDown) {
                wasKeyDown = isKeyDown;
                if (client.getConnection() != null) {
                    try {
                        FriendlyByteBuf buf = PacketByteBufs.create();
                        buf.writeBoolean(isKeyDown);
                        buf.writeUtf(ConfigManager.getConfig().selectedShape);
                        ClientPlayNetworking.send(CHAIN_KEY_STATE_ID, buf);
                    } catch (Exception e) {
                        OneKeyMiner.LOGGER.error("Failed to send chain key state: {}", e.getMessage());
                    }
                }
            }

            while (OPEN_CONFIG.consumeClick()) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                mc.setScreen(new FabricConfigScreen(mc.screen));
            }
        });
    }
}
