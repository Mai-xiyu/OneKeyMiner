package org.xiyu.onekeyminer.forge;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;
import org.xiyu.onekeyminer.network.ClientPreferenceRequest;
import org.xiyu.onekeyminer.network.ClientPreferenceSession;
import org.xiyu.onekeyminer.network.ClientPreferenceSyncTracker;
import org.xiyu.onekeyminer.preview.ChainPreviewHud;
import org.xiyu.onekeyminer.preview.ChainPreviewManager;

/** Forge physical-client key bindings and connection state. */
public final class ForgeKeyBindings {

    private static final String CATEGORY = "key.categories.onekeyminer";

    public static final KeyMapping CHAIN_MINING_KEY = new KeyMapping(
            "key.onekeyminer.hold",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_GRAVE_ACCENT),
            CATEGORY
    );
    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.onekeyminer.config",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_UNKNOWN),
            CATEGORY
    );

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

    private ForgeKeyBindings() {
    }

    public static void register() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CHAIN_MINING_KEY);
        event.register(OPEN_CONFIG);
    }

    public static boolean isChainKeyDown() {
        return CHAIN_MINING_KEY.isDown();
    }

    public static void resetConnectionState() {
        wasKeyDown = false;
        wasConnected = false;
        SYNC_TRACKER.reset();
        markPreferencesDirty(true);
        policyRefreshDelay = 0;
    }

    public static void sendCurrentPreferences() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            SYNC_TRACKER.invalidatePendingAttempt();
            minecraft.execute(ForgeKeyBindings::sendCurrentPreferences);
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
        boolean sent = ForgeClientNetworking.trySyncPreferences(
                sequence,
                pendingRequest
        );
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

    @Mod.EventBusSubscriber(modid = OneKeyMiner.MOD_ID, value = Dist.CLIENT)
    public static final class Events {

        private Events() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            boolean connected = minecraft.player != null
                    && minecraft.getConnection() != null;
            if (!connected) {
                if (wasConnected) {
                    SYNC_TRACKER.reset();
                }
                wasConnected = false;
                wasKeyDown = false;
                markPreferencesDirty(true);
                policyRefreshDelay = 0;
                return;
            }

            if (OPEN_CONFIG.consumeClick()) {
                minecraft.setScreen(ForgeConfigScreen.createConfigScreen(minecraft.screen));
            }

            boolean keyDown = CHAIN_MINING_KEY.isDown();
            if (!wasConnected) {
                wasConnected = true;
                markPreferencesDirty(true);
                policyRefreshDelay = 0;
            }
            if (keyDown != wasKeyDown) {
                wasKeyDown = keyDown;
                markPreferencesDirty(false);
            }
            if (!syncPending && --policyRefreshDelay <= 0) {
                markPreferencesDirty(false);
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
            Direction facing = minecraft.player.getDirection();
            ChainPreviewManager.getInstance().tick(
                    minecraft.level,
                    lookingAt,
                    facing,
                    minecraft.player.getXRot(),
                    keyDown
            );
        }

        @SubscribeEvent
        public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
            if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
                ChainPreviewHud.render(event.getGuiGraphics());
            }
        }

        @SubscribeEvent
        public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            resetConnectionState();
        }

        @SubscribeEvent
        public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            resetConnectionState();
        }
    }
}
