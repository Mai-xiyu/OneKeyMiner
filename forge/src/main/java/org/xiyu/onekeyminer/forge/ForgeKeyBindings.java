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

/** Forge physical-client key bindings and acknowledged preference synchronization. */
public final class ForgeKeyBindings {
    private static final String CATEGORY = "key.categories.onekeyminer";
    private static final int TRANSPORT_RETRY_TICKS = 20;
    private static final int ACK_RETRY_TICKS = 100;
    private static final int POLICY_REFRESH_TICKS = 600;
    private static final ClientPreferenceSyncTracker SYNC_TRACKER =
            new ClientPreferenceSyncTracker();

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
    private static boolean connected;
    private static boolean syncPending = true;
    private static boolean preferencesDirty = true;
    private static int retryDelay;
    private static int refreshDelay;
    private static ClientPreferenceRequest pendingRequest;

    private ForgeKeyBindings() {
    }

    public static void register() {
        OneKeyMiner.LOGGER.debug("Initialized Forge key bindings");
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CHAIN_MINING_KEY);
        event.register(OPEN_CONFIG);
    }

    public static boolean isChainKeyDown() {
        return CHAIN_MINING_KEY.isDown();
    }

    public static void sendCurrentState() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            SYNC_TRACKER.invalidatePendingAttempt();
            minecraft.execute(ForgeKeyBindings::sendCurrentState);
            return;
        }
        markDirty(true);
    }

    private static void beginSession() {
        connected = true;
        wasKeyDown = CHAIN_MINING_KEY.isDown();
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

    private static void tickSynchronization(Minecraft minecraft) {
        if (!connected || minecraft.player == null || minecraft.getConnection() == null) {
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
            attemptSynchronization(minecraft);
        }
    }

    private static void attemptSynchronization(Minecraft minecraft) {
        int sequence;
        if (preferencesDirty || !SYNC_TRACKER.hasPendingAttempt()) {
            sequence = SYNC_TRACKER.beginAttempt();
            pendingRequest = ClientPreferenceRequest.capture(CHAIN_MINING_KEY.isDown());
            preferencesDirty = false;
        } else {
            sequence = SYNC_TRACKER.pendingSequence();
        }
        if (pendingRequest == null || minecraft.getConnection() == null) {
            return;
        }
        boolean sent = ForgeNetworking.trySendClientPreferences(
                minecraft.getConnection().getConnection(),
                sequence,
                pendingRequest.holding(),
                pendingRequest.shapeId(),
                pendingRequest.teleportDrops(),
                pendingRequest.teleportExp()
        );
        syncPending = true;
        retryDelay = sent ? ACK_RETRY_TICKS : TRANSPORT_RETRY_TICKS;
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
            if (OPEN_CONFIG.consumeClick() && minecraft.player != null) {
                minecraft.setScreen(ForgeConfigScreen.createConfigScreen(minecraft.screen));
            }
            tickSynchronization(minecraft);
            if (minecraft.player == null || minecraft.level == null) {
                return;
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
                    CHAIN_MINING_KEY.isDown()
            );
        }

        @SubscribeEvent
        public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
            if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
                ChainPreviewHud.render(event.getGuiGraphics());
            }
        }

        @SubscribeEvent
        public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            beginSession();
        }

        @SubscribeEvent
        public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            endSession();
        }
    }
}
