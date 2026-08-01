package org.xiyu.onekeyminer.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import org.lwjgl.glfw.GLFW;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;
import org.xiyu.onekeyminer.network.ClientPreferenceRequest;
import org.xiyu.onekeyminer.network.ClientPreferenceSession;
import org.xiyu.onekeyminer.network.ClientPreferenceSyncTracker;

/** Fabric client key bindings and acknowledged preference synchronization. */
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
    private static boolean connected;
    private static boolean syncPending = true;
    private static boolean preferencesDirty = true;
    private static int retryDelay;
    private static int refreshDelay;
    private static ClientPreferenceRequest pendingRequest;

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

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                client.execute(KeyBindings::beginSession));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                endSession());
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    /** Compatibility API: all preferences are always captured together. */
    public static void sendTeleportSettings(boolean ignoredDrops, boolean ignoredExp) {
        sendCurrentState();
    }

    public static void sendCurrentState() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            // Invalidate immediately so a stale ACK cannot win before queued work runs.
            SYNC_TRACKER.invalidatePendingAttempt();
            minecraft.execute(KeyBindings::sendCurrentState);
            return;
        }
        markDirty(true);
    }

    private static void beginSession() {
        connected = true;
        wasKeyDown = CHAIN_MINING_KEY != null && CHAIN_MINING_KEY.isDown();
        SYNC_TRACKER.reset();
        pendingRequest = null;
        preferencesDirty = true;
        syncPending = true;
        retryDelay = 0;
        refreshDelay = 0;
        ClientPreferenceSession.clear();
    }

    private static void endSession() {
        connected = false;
        wasKeyDown = false;
        SYNC_TRACKER.reset();
        pendingRequest = null;
        preferencesDirty = true;
        syncPending = true;
        retryDelay = 0;
        refreshDelay = 0;
        ClientPreferenceSession.clear();
    }

    private static void markDirty(boolean clearAcknowledgement) {
        SYNC_TRACKER.invalidatePendingAttempt();
        pendingRequest = null;
        preferencesDirty = true;
        syncPending = true;
        retryDelay = 0;
        if (clearAcknowledgement) {
            ClientPreferenceSession.clear();
        }
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!connected || minecraft.getConnection() == null || minecraft.player == null) {
            return;
        }
        boolean keyDown = CHAIN_MINING_KEY.isDown();
        if (keyDown != wasKeyDown) {
            wasKeyDown = keyDown;
            markDirty(false);
        }
        if (!syncPending && --refreshDelay <= 0) {
            markDirty(false);
        }
        if (syncPending && (retryDelay <= 0 || --retryDelay <= 0)) {
            attemptSynchronization();
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
        boolean sent = trySend(sequence, pendingRequest);
        syncPending = true;
        retryDelay = sent ? ACK_RETRY_TICKS : TRANSPORT_RETRY_TICKS;
    }

    private static boolean trySend(int sequence, ClientPreferenceRequest request) {
        try {
            if (!ClientPlayNetworking.canSend(FabricNetworkingIds.CLIENT_PREFERENCES)) {
                return false;
            }
            FriendlyByteBuf buffer = PacketByteBufs.create();
            FabricPreferenceCodec.writeRequest(buffer, sequence, request);
            ClientPlayNetworking.send(FabricNetworkingIds.CLIENT_PREFERENCES, buffer);
            return true;
        } catch (RuntimeException exception) {
            OneKeyMiner.LOGGER.debug("Failed to send Fabric preferences", exception);
            return false;
        }
    }

    static void handlePreferencesAck(ClientPreferenceAck ack) {
        if (!SYNC_TRACKER.confirm(ack)) {
            return;
        }
        pendingRequest = null;
        retryDelay = 0;
        if (preferencesDirty) {
            syncPending = true;
            return;
        }
        ClientPreferenceSession.accept(ack);
        syncPending = false;
        refreshDelay = POLICY_REFRESH_TICKS;
    }
}
