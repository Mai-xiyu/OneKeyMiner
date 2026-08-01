package org.xiyu.onekeyminer.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;
import org.xiyu.onekeyminer.network.ClientPreferenceRequest;
import org.xiyu.onekeyminer.network.ClientPreferenceSession;
import org.xiyu.onekeyminer.network.ClientPreferenceSyncTracker;
import org.xiyu.onekeyminer.preview.ChainPreviewHud;
import org.xiyu.onekeyminer.preview.ChainPreviewManager;

@OnlyIn(Dist.CLIENT)
public final class NeoForgeKeyBindings {
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

    /** Coalesces config callbacks; the next client tick captures one snapshot. */
    public static void sendCurrentPreferences() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            SYNC_TRACKER.invalidatePendingAttempt();
            minecraft.execute(NeoForgeKeyBindings::sendCurrentPreferences);
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

    private static void attemptSynchronization() {
        if (CHAIN_MINING_KEY == null) {
            syncRetryDelay = TRANSPORT_RETRY_TICKS;
            return;
        }
        int sequence;
        if (preferencesDirty || !SYNC_TRACKER.hasPendingAttempt()) {
            sequence = SYNC_TRACKER.beginAttempt();
            pendingRequest = ClientPreferenceRequest.capture(CHAIN_MINING_KEY.isDown());
            preferencesDirty = false;
        } else {
            sequence = SYNC_TRACKER.pendingSequence();
        }
        if (pendingRequest == null) {
            return;
        }
        boolean sent = NeoForgeClientNetworking.trySyncPreferences(sequence, pendingRequest);
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

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean connected = minecraft.player != null && minecraft.getConnection() != null;
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

        if (OPEN_CONFIG != null && OPEN_CONFIG.consumeClick()) {
            minecraft.setScreen(NeoForgeConfigScreen.createConfigScreen(minecraft.screen));
        }
        if (CHAIN_MINING_KEY == null) {
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

        BlockPos lookingAt = null;
        if (minecraft.hitResult != null
                && minecraft.hitResult.getType() == HitResult.Type.BLOCK) {
            lookingAt = ((BlockHitResult) minecraft.hitResult).getBlockPos();
        }
        Direction playerFacing = minecraft.player.getDirection();
        float playerPitch = minecraft.player.getXRot();
        ChainPreviewManager.getInstance().tick(
                minecraft.level,
                lookingAt,
                playerFacing,
                playerPitch,
                keyDown
        );
    }
}
