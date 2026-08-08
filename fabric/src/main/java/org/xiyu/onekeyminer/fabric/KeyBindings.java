package org.xiyu.onekeyminer.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
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

    private static final int TRANSPORT_RETRY_TICKS = 20;
    private static final int ACK_RETRY_TICKS = 100;
    private static final int POLICY_REFRESH_TICKS = 600;
    private static final ClientPreferenceSyncTracker SYNC_TRACKER =
            new ClientPreferenceSyncTracker();

    private static boolean wasKeyDown;
    private static boolean wasConnected;
    private static boolean syncPending = true;
    private static boolean preferencesDirty = true;
    private static int syncRetryDelay;
    private static int policyRefreshDelay;
    private static ClientPreferenceRequest pendingRequest;

    private KeyBindings() {
    }

    public static void register() {
        CHAIN_MINING_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.onekeyminer.hold",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                KeyMapping.Category.GAMEPLAY
        ));

        OPEN_CONFIG = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.onekeyminer.config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KeyMapping.Category.GAMEPLAY
        ));

        registerKeyHandler();
    }

    /** Coalesces config callbacks; the next client tick captures one snapshot. */
    public static void sendCurrentPreferences() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            // Reject an already queued stale ACK before main-thread coalescing runs.
            SYNC_TRACKER.invalidatePendingAttempt();
            minecraft.execute(KeyBindings::sendCurrentPreferences);
            return;
        }
        markPreferencesDirty(true);
    }

    private static void markPreferencesDirty(boolean clearAcknowledgement) {
        SYNC_TRACKER.invalidatePendingAttempt();
        pendingRequest = null;
        preferencesDirty = true;
        syncPending = true;
        syncRetryDelay = 0;
        if (clearAcknowledgement) {
            ClientPreferenceSession.clear();
        }
    }

    private static boolean trySendCurrentPreferences(
            int sequence,
            ClientPreferenceRequest request
    ) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
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
        if (preferencesDirty) {
            return;
        }
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

            boolean keyDown = CHAIN_MINING_KEY.isDown();
            if (!wasConnected) {
                wasConnected = true;
                wasKeyDown = keyDown;
                SYNC_TRACKER.reset();
                syncPending = true;
                preferencesDirty = true;
                syncRetryDelay = 0;
                policyRefreshDelay = 0;
                pendingRequest = null;
                ClientPreferenceSession.clear();
            }

            if (keyDown != wasKeyDown) {
                wasKeyDown = keyDown;
                markPreferencesDirty(false);
            }
            if (!syncPending) {
                policyRefreshDelay--;
                if (policyRefreshDelay <= 0) {
                    markPreferencesDirty(false);
                }
            }
            if (syncPending) {
                if (syncRetryDelay > 0) {
                    syncRetryDelay--;
                } else {
                    attemptSynchronization();
                }
            }

            while (OPEN_CONFIG.consumeClick()) {
                client.setScreen(new FabricConfigScreen(client.screen));
            }
        });
    }
}
