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
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.preview.ChainPreviewHud;
import org.xiyu.onekeyminer.preview.ChainPreviewManager;

import java.lang.reflect.Method;

@OnlyIn(Dist.CLIENT)
public class ForgeKeyBindings {
    private static final String CATEGORY = "key.categories.onekeyminer";

    public static final KeyMapping CHAIN_MINING_KEY = new KeyMapping(
            "key.onekeyminer.hold",
            (IKeyConflictContext) KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_GRAVE_ACCENT),
            CATEGORY
    );

    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.onekeyminer.config",
            (IKeyConflictContext) KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_UNKNOWN),
            CATEGORY
    );

    private static boolean wasKeyDown = false;

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

    public static void renderPreviewHud(CustomizeGuiOverlayEvent.Chat event) {
        ChainPreviewHud.render(event.getGuiGraphics());
    }

    @Mod.EventBusSubscriber(modid = OneKeyMiner.MOD_ID, value = Dist.CLIENT)
    public static class Events {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return;
            }

            if (OPEN_CONFIG.consumeClick()) {
                openConfigScreen(minecraft);
            }

            boolean isKeyDown = CHAIN_MINING_KEY.isDown();

            if (isKeyDown != wasKeyDown) {
                wasKeyDown = isKeyDown;
                if (minecraft.getConnection() != null) {
                    try {
                        ForgeNetworking.sendKeyState(isKeyDown, ConfigManager.getConfig().selectedShape);
                    } catch (Exception e) {
                        OneKeyMiner.LOGGER.debug("Failed to send Forge key state: {}", e.getMessage());
                    }
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
