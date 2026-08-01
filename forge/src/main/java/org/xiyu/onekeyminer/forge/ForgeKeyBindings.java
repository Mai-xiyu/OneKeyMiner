package org.xiyu.onekeyminer.forge;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;
import org.xiyu.onekeyminer.network.ClientPreferenceRequest;
import org.xiyu.onekeyminer.network.ClientPreferenceSession;
import org.xiyu.onekeyminer.network.ClientPreferenceSyncTracker;
import org.xiyu.onekeyminer.preview.ChainPreviewManager;

import java.lang.reflect.Method;

@OnlyIn(Dist.CLIENT)
public final class ForgeKeyBindings {
    public static final KeyMapping CHAIN_MINING_KEY = new KeyMapping(
            "key.onekeyminer.hold",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            KeyMapping.Category.GAMEPLAY
    );

    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.onekeyminer.config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            KeyMapping.Category.GAMEPLAY
    );

    static {
        CHAIN_MINING_KEY.setKeyConflictContext(KeyConflictContext.IN_GAME);
        OPEN_CONFIG.setKeyConflictContext(KeyConflictContext.IN_GAME);
    }

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
        OneKeyMiner.LOGGER.debug("Forge key bindings initialized");
    }

    private static void openConfigScreen(Minecraft minecraft) {
        try {
            Method createMethod = ForgeConfigScreen.class.getDeclaredMethod(
                    "createConfigScreen",
                    Screen.class
            );
            createMethod.setAccessible(true);
            Screen configScreen = (Screen) createMethod.invoke(
                    null,
                    minecraft.gui.screen()
            );
            minecraft.gui.setScreen(configScreen);
        } catch (Exception e) {
            OneKeyMiner.LOGGER.error("Failed to open Forge config screen", e);
        }
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CHAIN_MINING_KEY);
        event.register(OPEN_CONFIG);
        OneKeyMiner.LOGGER.info("Registered Forge key mappings");
    }

    public static void sendCurrentPreferences() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            // Reject an already queued stale ACK before main-thread coalescing runs.
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
        boolean sent = ForgeClientNetworking.trySyncPreferences(sequence, pendingRequest);
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

    public static final class Events {
        private Events() {
        }

        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            registerKeyMappings(event);
        }

        public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
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
                openConfigScreen(minecraft);
            }

            boolean isKeyDown = CHAIN_MINING_KEY.isDown();

            if (!wasConnected) {
                wasConnected = true;
                markPreferencesDirty(true);
                policyRefreshDelay = 0;
            }

            if (isKeyDown != wasKeyDown) {
                wasKeyDown = isKeyDown;
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

            Direction playerFacing = minecraft.player.getDirection();
            float playerPitch = minecraft.player.getXRot();
            ChainPreviewManager.getInstance().tick(
                    minecraft.level,
                    lookingAt,
                    playerFacing,
                    playerPitch,
                    isKeyDown
            );
        }
    }
}
