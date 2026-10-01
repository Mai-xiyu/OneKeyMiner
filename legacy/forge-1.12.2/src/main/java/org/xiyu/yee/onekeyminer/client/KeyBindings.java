package org.xiyu.yee.onekeyminer.client;


import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.Minecraft;


import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.xiyu.yee.onekeyminer.Onekeyminer;
import org.xiyu.yee.onekeyminer.config.ClientConfig;
import org.xiyu.yee.onekeyminer.network.ChainModePacket;
import org.xiyu.yee.onekeyminer.network.NetworkHandler;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 按键绑定管理类
 * 处理连锁模式的按键绑定、状态切换和定时发送网络包
 */
@Mod.EventBusSubscriber(modid = Onekeyminer.MODID, value = Side.CLIENT)
public class KeyBindings {
    // 连锁模式按键绑定
        public static final KeyBinding CHAIN_KEY = new KeyBinding(
            "key.onekeyminer.chain",
            KeyConflictContext.IN_GAME, // ★ 关键修复
            org.lwjgl.input.Keyboard.KEY_GRAVE,
            "key.categories.onekeyminer"
        );


    // 连锁模式状态
    private static boolean chainModeActive = false;
    // 数据包发送计时器（按住模式）
    private static int packetCounter = 0;
    // 连锁模式自动关闭计时器（非按住模式）
    private static int frozenTimerCounter = 0;
    // 上次检查时间
    private static long lastCheckTime = 0;
    // 计时器
    private static Timer timer = null;

    /**
     * 注册按键绑定 - 1.12.2版本
     */
    public static void registerKeyMappings() {
        // 在1.12.2中使用ClientRegistry直接注册按键
        ClientRegistry.registerKeyBinding(CHAIN_KEY);
    }

    /**
     * 注册客户端处理器，由主类调用
     * 使用Java Timer替代Tick事件
     */
    public static void registerClientTick() {
        if(ClientConfig.INSTANCE.requireKeyHold.get())  Onekeyminer.LOGGER.info("初始化客户端定时器");

        // 取消旧定时器（如果存在）
        if (timer != null) {
            timer.cancel();
        }

        // 创建新的定时器，每100毫秒执行一次检查
        timer = new Timer("ChainModeTimer", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    handleTimerTick();
                } catch (Throwable t) {
                    // 容错，避免定时器线程因异常停止
                }
            }
        }, 0, 100); // 每100毫秒执行一次

        lastCheckTime = System.currentTimeMillis();
    }

    /**
     * 处理按键输入 - 1.12.2版本使用ClientTickEvent
     */
    @SubscribeEvent
    public static void onKeyInput(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        if (CHAIN_KEY.isPressed()) {
            Boolean requireKeyHold = ClientConfig.INSTANCE.requireKeyHold.get();
            if (requireKeyHold == null) {
                if(ClientConfig.INSTANCE.requireKeyHold.get()) Onekeyminer.LOGGER.error("配置项 requireKeyHold 未正确初始化，使用默认值 false");
                requireKeyHold = false;
            }
            if (requireKeyHold) {
                // 按住键才能触发连锁（每次按下发送一次激活消息）
                chainModeActive = true;
                NetworkHandler.sendToServer(new ChainModePacket(true));
                packetCounter = 0; // 重置计时器
                if(ClientConfig.INSTANCE.requireKeyHold.get()) Onekeyminer.LOGGER.debug("Key pressed, activating chain mode (hold mode)");
            } else {
                // 点击切换连锁模式
                chainModeActive = !chainModeActive;
                NetworkHandler.sendToServer(new ChainModePacket(chainModeActive));
                if(ClientConfig.INSTANCE.requireKeyHold.get()) Onekeyminer.LOGGER.debug("Key pressed, toggling chain mode: {}", chainModeActive);

                // 重置冻结计时器
                if (chainModeActive) {
                    frozenTimerCounter = ClientConfig.INSTANCE.frozentimer.get() * 1000;
                }

                // 显示状态消息
                ClientUtils.showStatusMessage(chainModeActive ?
                        "message.onekeyminer.mode_enabled" :
                        "message.onekeyminer.mode_disabled");
            }

            // 添加调试日志，检查客户端状态
            if(ClientConfig.INSTANCE.requireKeyHold.get()) Onekeyminer.LOGGER.debug("Client chain mode state after key press: {}", chainModeActive);
        }
    }

    /**
     * 处理定时器触发事件
     * 用于周期性发送状态包和处理倒计时
     * 在按住模式下每1000毫秒发送一次状态包
     * 在非按住模式下处理自动关闭倒计时
     */
    public static void handleTimerTick() {
        // 获取Minecraft实例
        Minecraft minecraft = Minecraft.getMinecraft();
        // 安全检查 - 确保在游戏主线程中执行UI相关操作
    if (minecraft == null || minecraft.player == null) return;

        // 计算自上次检查以来经过的时间
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastCheckTime;
        lastCheckTime = currentTime;

        // 防止意外的大跳变（例如游戏暂停后恢复）
        if (elapsed <= 0 || elapsed > 5000) return;

        if (ClientConfig.INSTANCE.requireKeyHold.get()) {
            // 按住模式：每1000毫秒发送一次状态
            if (CHAIN_KEY.isKeyDown()) {
                packetCounter += elapsed;
                if (packetCounter >= 500) {
                    // 在游戏主线程中执行
                    minecraft. addScheduledTask(() -> {
                        NetworkHandler.sendToServer(new ChainModePacket(true));
                    });
                    packetCounter = 0;
                }
            } else if (chainModeActive) {
                // 如果键被释放且模式仍为激活，发送停用消息
                chainModeActive = false;
                // 在游戏主线程中执行
                minecraft. addScheduledTask(() -> {
                    NetworkHandler.sendToServer(new ChainModePacket(false));
                });
            }
        } else if (chainModeActive) {
            // 非按住模式：处理冻结计时器
            if (frozenTimerCounter > 0) {
                frozenTimerCounter -= elapsed;
                // 仅在调试模式下每秒记录一次倒计时

                if (frozenTimerCounter <= 0) {
                    // 计时结束，关闭连锁模式
                    chainModeActive = false;
                    // 在游戏主线程中执行
                    minecraft. addScheduledTask(() -> {
                        NetworkHandler.sendToServer(new ChainModePacket(false));
                        ClientUtils.showStatusMessage("message.onekeyminer.mode_disabled");
                    });
                }
            }
        }
    }

    /**
     * 获取当前连锁模式状态
     * @return 连锁模式是否激活
     */
//    public static boolean isChainModeActive() {
//        return chainModeActive;
//    }
}

