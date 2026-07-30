package org.xiyu.onekeyminer.forge;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.preview.ChainPreviewHud;
import org.xiyu.onekeyminer.preview.ChainPreviewManager;

import java.lang.reflect.Method;

public class ForgeKeyBindings {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.parse("key.categories.onekeyminer"));

    public static final KeyMapping CHAIN_MINING_KEY = new KeyMapping(
            "key.onekeyminer.hold",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            CATEGORY
    );

    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.onekeyminer.config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    static {
        CHAIN_MINING_KEY.setKeyConflictContext(KeyConflictContext.IN_GAME);
        OPEN_CONFIG.setKeyConflictContext(KeyConflictContext.IN_GAME);
    }

    private static boolean wasKeyDown = false;
    private static boolean syncPending = true;
    private static int syncRetryDelay = 0;

    public static void register() {
        OneKeyMiner.LOGGER.debug("Forge key bindings initialized");
    }

    private static void openConfigScreen(Minecraft minecraft) {
        try {
            Method createMethod = ForgeConfigScreen.class.getDeclaredMethod("createConfigScreen", Screen.class);
            createMethod.setAccessible(true);
            Screen configScreen = (Screen) createMethod.invoke(null, minecraft.gui.screen());
            minecraft.gui.setScreen(configScreen);
        } catch (Exception e) {
            OneKeyMiner.LOGGER.error("Failed to open Forge config screen: {}", e.getMessage());
        }
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CHAIN_MINING_KEY);
        event.register(OPEN_CONFIG);
        OneKeyMiner.LOGGER.info("Registered Forge key mappings");
    }

    public static void renderPreviewHud(CustomizeGuiOverlayEvent.Chat event) {
        ChainPreviewHud.render(event.getGuiGraphics());
    }

    public static class Events {
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            registerKeyMappings(event);
        }

        public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return;
            }

            if (OPEN_CONFIG.consumeClick()) {
                openConfigScreen(minecraft);
            }

            boolean isKeyDown = CHAIN_MINING_KEY.isDown();

            if (syncPending && minecraft.getConnection() != null) {
                if (syncRetryDelay > 0) {
                    syncRetryDelay--;
                } else {
                    syncCurrentState();
                }
            }

            if (!syncPending && isKeyDown != wasKeyDown && minecraft.getConnection() != null) {
                if (ForgeClientNetworking.trySendKeyState(
                        isKeyDown,
                        ConfigManager.getConfig().selectedShape
                )) {
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

    public static void syncCurrentState() {
        MinerConfigSnapshot snapshot = MinerConfigSnapshot.read();
        boolean holding = CHAIN_MINING_KEY.isDown();
        boolean keySent = ForgeClientNetworking.trySendKeyState(holding, snapshot.shapeId());
        boolean settingsSent = ForgeClientNetworking.trySendTeleportSettings(
                snapshot.teleportDrops(),
                snapshot.teleportExp()
        );
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

    private record MinerConfigSnapshot(
            String shapeId,
            boolean teleportDrops,
            boolean teleportExp
    ) {
        private static MinerConfigSnapshot read() {
            var config = ConfigManager.getConfig();
            return new MinerConfigSnapshot(
                    config.selectedShape,
                    config.teleportDrops,
                    config.teleportExp
            );
        }
    }
}
