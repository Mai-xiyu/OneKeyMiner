package org.xiyu.onekeyminer.forge;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.preview.ChainPreviewHud;
import org.xiyu.onekeyminer.preview.ChainPreviewManager;

import java.lang.reflect.Method;

/**
 * Forge 按键绑定注册
 * <p>注册模组使用的快捷键，并通过客户端 tick 事件处理按键状态同步。</p>
 * 
 * @author OneKeyMiner Team
 * @version 1.1.0
 * @since Minecraft 1.20.1
 */
public class ForgeKeyBindings {
    
    /** 按键分类 */
    private static final String CATEGORY = "key.categories.onekeyminer";
    
    /** 连锁挖矿激活按键（默认：波浪键/反引号键）- 静态初始化 */
    public static final KeyMapping CHAIN_MINING_KEY = new KeyMapping(
            "key.onekeyminer.hold",
            (IKeyConflictContext) KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_GRAVE_ACCENT),
            CATEGORY
    );
    
    /** 打开配置界面的按键（默认：无）- 静态初始化 */
    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.onekeyminer.config",
            (IKeyConflictContext) KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_UNKNOWN),
            CATEGORY
    );
    
    /** 上一帧按键是否被按下 */
    private static boolean wasKeyDown = false;
    
    /**
     * 注册所有按键绑定
     */
    public static void register() {
        OneKeyMiner.LOGGER.debug("初始化 Forge 按键绑定");
    }
    
    /**
     * 尝试打开配置界面
     */
    private static void openConfigScreen(Minecraft minecraft) {
        try {
            Method createMethod = ForgeConfigScreen.class.getDeclaredMethod("createConfigScreen", Screen.class);
            createMethod.setAccessible(true);
            Screen configScreen = (Screen) createMethod.invoke(null, minecraft.screen);
            minecraft.setScreen(configScreen);
        } catch (Exception e) {
            OneKeyMiner.LOGGER.error("无法打开配置界面: {}", e.getMessage());
        }
    }
    
    /**
     * 注册按键映射（MOD 事件总线）
     */
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CHAIN_MINING_KEY);
        event.register(OPEN_CONFIG);
        OneKeyMiner.LOGGER.info("已注册 Forge 按键映射");
    }

    // ============ FORGE 事件总线事件 ============

    @Mod.EventBusSubscriber(modid = OneKeyMiner.MOD_ID, value = Dist.CLIENT)
    public static class Events {

        /**
         * 客户端 tick 事件处理 - 在主线程上检测按键状态
         */
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;
            
            // 处理配置键
            if (OPEN_CONFIG.consumeClick()) {
                openConfigScreen(minecraft);
            }
            
            // 处理连锁挖矿按键
            boolean isKeyDown = CHAIN_MINING_KEY.isDown();
            
            if (isKeyDown != wasKeyDown) {
                wasKeyDown = isKeyDown;
                if (minecraft.getConnection() != null) {
                    try {
                        ForgeNetworking.sendKeyState(isKeyDown, ConfigManager.getConfig().selectedShape);
                        OneKeyMiner.LOGGER.debug("连锁按键状态变化: {}", isKeyDown ? "按下" : "释放");
                    } catch (Exception e) {
                        OneKeyMiner.LOGGER.debug("发送按键状态失败: {}", e.getMessage());
                    }
                }
            }
            
            // 更新连锁预览
            BlockPos lookingAt = null;
            if (minecraft.hitResult != null && minecraft.hitResult.getType() == HitResult.Type.BLOCK) {
                lookingAt = ((BlockHitResult) minecraft.hitResult).getBlockPos();
            }
            Direction playerFacing = minecraft.player.getDirection();
            float playerPitch = minecraft.player.getXRot();
            
            ChainPreviewManager.getInstance().tick(
                    minecraft.level, lookingAt, playerFacing, playerPitch, isKeyDown
            );
        }
        
        /**
         * HUD 渲染事件 - 绘制连锁预览覆盖层
         */
        @SubscribeEvent
        public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
            if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
                ChainPreviewHud.render(event.getGuiGraphics());
            }
        }
    }
}
