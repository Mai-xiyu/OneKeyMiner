package org.xiyu.yee.onekeyminer.config;

import net.minecraftforge.common.config.Configuration;
import java.io.File;

/**
 * 客户端配置类 - 处理只在客户端生效的配置项
 */
public class ClientConfig {
    public static final ClientConfig INSTANCE = new ClientConfig();
    public static final Object SPEC = null; // 保持兼容性,但在1.12.2中不使用
    
    private static Configuration config;
    
    // 是否需要按住键来激活连锁模式
    public final BooleanValue requireKeyHold = new BooleanValue();
    
    // 是否显示连锁方块数量
    public final BooleanValue showBlockCount = new BooleanValue();
    
    // 调试模式配置
    public final BooleanValue Debug = new BooleanValue();
    
    // 消息显示样式(操作栏或聊天栏)
    public final ConfigValue<String> messageStyle = new ConfigValue<>();
    
    // 按住模式关闭时的冻结计时器
    public final IntValue frozentimer = new IntValue();
    
    public static void init(File configFile) {
        config = new Configuration(configFile);
        config.load();
        
        INSTANCE.requireKeyHold.value = config.getBoolean(
            "requireKeyHold", "client", false,
            "是否需要按住键来激活连锁模式||Whether to hold the key to activate the chain mode"
        );
        
        INSTANCE.showBlockCount.value = config.getBoolean(
            "showBlockCount", "client", true,
            "是否显示连锁方块数量||Whether to show the number of chain blocks"
        );
        
        INSTANCE.Debug.value = config.getBoolean(
            "Debug", "client", false,
            "是否启用调试模式||Whether to enable debug mode"
        );
        
        INSTANCE.messageStyle.value = config.getString(
            "messageStyle", "client", "actionbar",
            "消息显示样式 (actionbar 或 chat)||Message display style (actionbar or chat)"
        );
        
        INSTANCE.frozentimer.value = config.getInt(
            "frozentimer", "client", 60, 1, 32767,
            "按住模式关闭时，连锁状态重置时间(秒)||Chain state reset time when hold mode is off (seconds)"
        );
        
        if (config.hasChanged()) {
            config.save();
        }
    }
    
    public static void save() {
        if (config != null && config.hasChanged()) {
            config.save();
        }
    }
    
    // 包装类,用于保持API兼容性
    public static class BooleanValue {
        private boolean value;
        public boolean get() { return value; }
        public void set(boolean value) { 
            this.value = value;
            if (config != null) {
                save();
            }
        }
    }
    
    public static class IntValue {
        private int value;
        public int get() { return value; }
        public void set(int value) { 
            this.value = value;
            if (config != null) {
                save();
            }
        }
    }
    
    public static class ConfigValue<T> {
        private T value;
        public T get() { return value; }
        public void set(T value) { 
            this.value = value;
            if (config != null) {
                save();
            }
        }
    }
    
    public static Configuration getConfig() {
        return config;
    }
} 