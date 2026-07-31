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
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.preview.ChainPreviewManager;

import java.lang.reflect.Method;

@OnlyIn(Dist.CLIENT)
public class ForgeKeyBindings {
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

    private static boolean wasKeyDown = false;
    private static boolean wasConnected = false;
    private static boolean syncPending = true;
    private static int syncRetryDelay;

    public static void register() {
        OneKeyMiner.LOGGER.debug("Forge key bindings initialized");
    }

    private static void openConfigScreen(Minecraft minecraft) {
        try {
            Method createMethod = ForgeConfigScreen.class.getDeclaredMethod("createConfigScreen", Screen.class);
            createMethod.setAccessible(true);
            Screen configScreen = (Screen) createMethod.invoke(null, minecraft.screen);
            minecraft.setScreen(configScreen);
        } catch (Exception e) {
            OneKeyMiner.LOGGER.error("Failed to open Forge config screen: {}", e.getMessage());
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
            minecraft.execute(ForgeKeyBindings::sendCurrentPreferences);
            return;
        }
        boolean sent = ForgeClientNetworking.trySyncPreferences(CHAIN_MINING_KEY.isDown());
        syncPending = !sent;
        syncRetryDelay = sent ? 0 : 20;
        if (sent) {
            wasKeyDown = CHAIN_MINING_KEY.isDown();
        }
    }

    @Mod.EventBusSubscriber(modid = OneKeyMiner.MOD_ID, value = Dist.CLIENT)
    public static class Events {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            registerKeyMappings(event);
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            boolean connected = minecraft.player != null && minecraft.getConnection() != null;
            if (!connected) {
                wasConnected = false;
                wasKeyDown = false;
                syncPending = true;
                syncRetryDelay = 0;
                return;
            }

            if (OPEN_CONFIG.consumeClick()) {
                openConfigScreen(minecraft);
            }

            boolean isKeyDown = CHAIN_MINING_KEY.isDown();

            if (!wasConnected) {
                wasConnected = true;
                syncPending = true;
                syncRetryDelay = 0;
            }

            if (syncPending) {
                if (syncRetryDelay > 0) {
                    syncRetryDelay--;
                } else {
                    sendCurrentPreferences();
                }
            } else if (isKeyDown != wasKeyDown) {
                if (ForgeClientNetworking.trySyncPreferences(isKeyDown)) {
                    wasKeyDown = isKeyDown;
                } else {
                    syncPending = true;
                    syncRetryDelay = 20;
                }
            }

            BlockPos lookingAt = null;
            if (minecraft.hitResult != null && minecraft.hitResult.getType() == HitResult.Type.BLOCK) {
                lookingAt = ((BlockHitResult) minecraft.hitResult).getBlockPos();
            }

            Direction playerFacing = minecraft.player.getDirection();
            float playerPitch = minecraft.player.getXRot();
            ChainPreviewManager.getInstance().tick(minecraft.level, lookingAt, playerFacing, playerPitch, isKeyDown);
        }

    }
}
