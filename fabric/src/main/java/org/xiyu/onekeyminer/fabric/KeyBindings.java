package org.xiyu.onekeyminer.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
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
    private static boolean syncPending = true;
    private static int syncRetryDelay = 0;

    public static void register() {
        // 连锁挖矿激活按键（按住模式）
        CHAIN_MINING_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.onekeyminer.hold",             // 翻译键
                InputConstants.Type.KEYSYM,         // 输入类型
                GLFW.GLFW_KEY_GRAVE_ACCENT,        // 默认按键（`键）
                KeyMapping.Category.GAMEPLAY       // 使用游戏玩法分类
        ));
        
        // 打开配置界面（默认未绑定）
        OPEN_CONFIG = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.onekeyminer.config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KeyMapping.Category.GAMEPLAY
        ));

        registerKeyHandler();
    }

    public static void sendTeleportSettings(boolean teleportDrops, boolean teleportExp) {
        trySendTeleportSettings(teleportDrops, teleportExp);
    }

    private static boolean trySendTeleportSettings(boolean teleportDrops, boolean teleportExp) {
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.getConnection() != null
                    && ClientPlayNetworking.canSend(FabricPayloads.TeleportSettings.TYPE)) {
                ClientPlayNetworking.send(
                        new FabricPayloads.TeleportSettings(teleportDrops, teleportExp)
                );
                return true;
            }
        } catch (Exception e) {
            OneKeyMiner.LOGGER.debug("Failed to send teleport settings: {}", e.getMessage());
        }
        return false;
    }

    public static void syncCurrentState() {
        var config = ConfigManager.getConfig();
        boolean holding = CHAIN_MINING_KEY != null && CHAIN_MINING_KEY.isDown();
        boolean keySent = trySendChainKeyState(holding, config.selectedShape);
        boolean settingsSent = trySendTeleportSettings(config.teleportDrops, config.teleportExp);
        syncPending = !(keySent && settingsSent);
        syncRetryDelay = syncPending ? 20 : 0;
        if (!syncPending) {
            wasKeyDown = holding;
        }
    }

    public static void resetConnectionState() {
        wasKeyDown = false;
        syncPending = true;
        syncRetryDelay = 0;
    }

    private static boolean trySendChainKeyState(boolean holding, String shapeId) {
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.getConnection() != null
                    && ClientPlayNetworking.canSend(FabricPayloads.ChainKeyState.TYPE)) {
                ClientPlayNetworking.send(new FabricPayloads.ChainKeyState(holding, shapeId));
                return true;
            }
        } catch (Exception e) {
            OneKeyMiner.LOGGER.debug("Failed to send chain key state: {}", e.getMessage());
        }
        return false;
    }

    private static void registerKeyHandler() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (syncPending && client.getConnection() != null) {
                if (syncRetryDelay > 0) {
                    syncRetryDelay--;
                } else {
                    syncCurrentState();
                }
            }

            boolean isKeyDown = CHAIN_MINING_KEY.isDown();
            if (!syncPending && isKeyDown != wasKeyDown && client.getConnection() != null) {
                if (trySendChainKeyState(isKeyDown, ConfigManager.getConfig().selectedShape)) {
                    wasKeyDown = isKeyDown;
                } else {
                    syncPending = true;
                    syncRetryDelay = 20;
                }
            }

            while (OPEN_CONFIG.consumeClick()) {
                client.gui.setScreen(new FabricConfigScreen(client.gui.screen()));
            }
        });
    }

}
