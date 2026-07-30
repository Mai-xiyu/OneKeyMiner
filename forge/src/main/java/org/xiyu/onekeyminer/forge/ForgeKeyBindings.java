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

    public static void resetConnectionState() {
        wasKeyDown = false;
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
            if (minecraft.player == null) {
                return;
            }

            if (OPEN_CONFIG.consumeClick()) {
                minecraft.setScreen(ForgeConfigScreen.createConfigScreen(minecraft.screen));
            }

            boolean keyDown = CHAIN_MINING_KEY.isDown();
            if (keyDown != wasKeyDown) {
                wasKeyDown = keyDown;
                ForgeClientSetup.sendCurrentState();
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

        @SubscribeEvent
        public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
            if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
                ChainPreviewHud.render(event.getGuiGraphics());
            }
        }

        @SubscribeEvent
        public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            resetConnectionState();
            ForgeClientSetup.sendCurrentState();
        }

        @SubscribeEvent
        public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            resetConnectionState();
        }
    }
}
