package org.xiyu.onekeyminer.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;
import org.xiyu.onekeyminer.network.ClientPreferenceRequest;
import org.xiyu.onekeyminer.network.ClientPreferenceSession;
import org.xiyu.onekeyminer.network.ClientPreferenceSyncTracker;

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
    private static boolean preferencesDirty = true;
    private static int syncRetryDelay;
    private static int policyRefreshDelay;
    private static ClientPreferenceRequest pendingRequest;
    private static final int TRANSPORT_RETRY_TICKS = 20;
    private static final int ACK_RETRY_TICKS = 100;
    private static final int POLICY_REFRESH_TICKS = 600;
    private static final ClientPreferenceSyncTracker SYNC_TRACKER =
            new ClientPreferenceSyncTracker();

    private KeyBindings() {
    }

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

    public static void sendCurrentPreferences() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(KeyBindings::sendCurrentPreferences);
            return;
        }
        ClientPreferenceSession.clear();
        preferencesDirty = true;
        syncPending = true;
        syncRetryDelay = 0;
    }

    private static boolean trySendCurrentPreferences(
            int sequence,
            ClientPreferenceRequest request
    ) {
        try {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.getConnection() == null
                    || !ClientPlayNetworking.canSend(FabricPayloads.ClientPreferencesPayload.TYPE)) {
                return false;
            }

            ClientPlayNetworking.send(new FabricPayloads.ClientPreferencesPayload(
                    FabricPayloads.WIRE_VERSION,
                    sequence,
                    request.holding(),
                    request.shapeId(),
                    request.teleportDrops(),
                    request.teleportExp()
            ));
            return true;
        } catch (RuntimeException e) {
            OneKeyMiner.LOGGER.debug("Failed to send Fabric client preferences: {}", e.getMessage());
            return false;
        }
    }

    private static void attemptSynchronization() {
        int sequence;
        if (preferencesDirty || !SYNC_TRACKER.hasPendingAttempt()) {
            sequence = SYNC_TRACKER.beginAttempt();
            pendingRequest = ClientPreferenceRequest.capture(
                    CHAIN_MINING_KEY != null && CHAIN_MINING_KEY.isDown()
            );
            preferencesDirty = false;
        } else {
            sequence = SYNC_TRACKER.pendingSequence();
        }
        if (pendingRequest == null) {
            return;
        }
        boolean sent = trySendCurrentPreferences(sequence, pendingRequest);
        syncPending = true;
        syncRetryDelay = sent ? ACK_RETRY_TICKS : TRANSPORT_RETRY_TICKS;
    }

    static void handlePreferencesAck(ClientPreferenceAck ack) {
        if (SYNC_TRACKER.confirm(ack)) {
            ClientPreferenceSession.accept(ack);
            pendingRequest = null;
            syncPending = false;
            syncRetryDelay = 0;
            policyRefreshDelay = POLICY_REFRESH_TICKS;
        }
    }

    private static void registerKeyHandler() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean connected = client.getConnection() != null && client.player != null;
            if (!connected) {
                if (wasConnected) {
                    SYNC_TRACKER.reset();
                }
                wasConnected = false;
                wasKeyDown = false;
                syncPending = true;
                preferencesDirty = true;
                syncRetryDelay = 0;
                policyRefreshDelay = 0;
                pendingRequest = null;
                ClientPreferenceSession.clear();
                return;
            }

            boolean isKeyDown = CHAIN_MINING_KEY.isDown();
            if (!wasConnected) {
                wasConnected = true;
                syncPending = true;
                preferencesDirty = true;
                syncRetryDelay = 0;
                policyRefreshDelay = 0;
                pendingRequest = null;
                ClientPreferenceSession.clear();
            }

            if (isKeyDown != wasKeyDown) {
                wasKeyDown = isKeyDown;
                preferencesDirty = true;
                syncPending = true;
                syncRetryDelay = 0;
            }
            if (!syncPending && --policyRefreshDelay <= 0) {
                preferencesDirty = true;
                syncPending = true;
                syncRetryDelay = 0;
            }
            if (syncPending) {
                if (syncRetryDelay > 0) {
                    syncRetryDelay--;
                } else {
                    attemptSynchronization();
                }
            }

            while (OPEN_CONFIG.consumeClick()) {
                client.gui.setScreen(new FabricConfigScreen(client.gui.screen()));
            }
        });
    }
}
