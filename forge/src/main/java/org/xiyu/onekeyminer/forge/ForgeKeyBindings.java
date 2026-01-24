package org.xiyu.onekeyminer.forge;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.xiyu.onekeyminer.OneKeyMiner;

import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Forge 按键绑定注册
 * <p>注册模组使用的快捷键，并处理按键事件和定时状态同步。</p>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.3
 * @since Minecraft 1.21.9
 */
public class ForgeKeyBindings {
    
    /** 按键分类 */
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            ResourceLocation.parse("key.categories.onekeyminer"));
    
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
    
    /** 连锁模式状态（客户端记录） */
    private static boolean chainModeActive = false;
    
    /** 数据包发送计时器 */
    private static long packetCounter = 0;
    
    /** 上次检查时间 */
    private static long lastCheckTime = 0;
    
    /** 定时器 */
    private static Timer timer = null;
    
    /**
     * 注册所有按键绑定并初始化定时器
     */
    public static void register() {
        OneKeyMiner.LOGGER.debug("初始化 Forge 按键绑定和定时器");
        initTimer();
    }
    
    /**
     * 初始化定时器
     */
    private static void initTimer() {
        if (timer != null) {
            timer.cancel();
        }
        
        timer = new Timer("ChainModeTimer-Forge", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handleTimerTick();
            }
        }, 0, 100);
        
        lastCheckTime = System.currentTimeMillis();
    }
    
    /**
     * 处理定时器触发事件
     * 用于处理按住模式下的持续心跳包和释放检测
     */
    private static void handleTimerTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || minecraft.level == null) return;
        
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastCheckTime;
        lastCheckTime = currentTime;
        
        if (elapsed <= 0 || elapsed > 5000) return;
        
        // 始终使用按住按键激活模式
        boolean isKeyDown = CHAIN_MINING_KEY.isDown();
        
        if (isKeyDown) {
            if (!chainModeActive) {
                chainModeActive = true;
                // 立即发送激活包
                minecraft.execute(() -> {
                    try {
                        ForgeNetworking.sendKeyState(true);
                        OneKeyMiner.LOGGER.info("连锁按键按下");
                    } catch (Exception e) {
                        OneKeyMiner.LOGGER.error("发送按键状态失败", e);
                    }
                });
            }
            
            // 发送心跳包 (每500ms)
            packetCounter += elapsed;
            if (packetCounter >= 500) {
                minecraft.execute(() -> {
                    try {
                        ForgeNetworking.sendKeyState(true);
                    } catch (Exception e) {
                        // 忽略
                    }
                });
                packetCounter = 0;
            }
        } else if (chainModeActive) {
            chainModeActive = false;
            minecraft.execute(() -> {
                try {
                    ForgeNetworking.sendKeyState(false);
                    OneKeyMiner.LOGGER.info("连锁按键释放");
                } catch (Exception e) {
                    OneKeyMiner.LOGGER.error("发送按键释放包失败", e);
                }
            });
        }
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
    
    // ============ FORGE 事件总线事件 ============
    
    /**
     * 事件处理器 - 注册到 FORGE 事件总线
     * 在 Forge 1.21.9 中，RegisterKeyMappingsEvent 也在 FORGE 事件总线上
     */
    @Mod.EventBusSubscriber(modid = OneKeyMiner.MOD_ID, value = Dist.CLIENT)
    public static class Events {
        
        /**
         * 注册按键映射
         */
        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(CHAIN_MINING_KEY);
            event.register(OPEN_CONFIG);
            OneKeyMiner.LOGGER.info("已注册 Forge 按键映射");
        }
        
        /**
         * 处理按键输入
         */
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;

            // 处理配置键
            if (OPEN_CONFIG.consumeClick()) {
                openConfigScreen(minecraft);
            }
        }
    }
}
