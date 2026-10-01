package org.xiyu.yee.onekeyminer.config;

import net.minecraftforge.common.config.Configuration;
import java.io.File;

public class ServerConfig {
    public static final Object SPEC = null;
    public static final ServerConfig INSTANCE = new ServerConfig();
    
    private static Configuration config;
    
    // 工具保护模式
    public final BooleanValue toolProtectionEnabled = new BooleanValue();
    public final DoubleValue toolDurabilityThreshold = new DoubleValue();
    
    // 饱食度保护
    public final BooleanValue hungerProtectionEnabled = new BooleanValue();
    public final DoubleValue hungerThreshold = new DoubleValue();
    
    public static void init(File configFile) {
        config = new Configuration(configFile);
        config.load();
        
        String category = "server";
        
        INSTANCE.toolProtectionEnabled.value = config.getBoolean(
            "toolProtectionEnabled", category, true,
            "启用工具保护模式(防止工具耐久过低导致损坏)||Enable tool protection mode (prevent tool durability from being too low)"
        );
        
        INSTANCE.toolDurabilityThreshold.value = config.getFloat(
            "toolDurabilityThreshold", category, 0.1f, 0.01f, 1000.0f,
            "工具耐久阈值 (如果是小于等于1.0的值则表示百分比，大于1.0表示固定值)||Tool durability threshold (if the value is less than or equal to 1.0, it represents a percentage, otherwise it represents a fixed value)"
        );
        
        INSTANCE.hungerProtectionEnabled.value = config.getBoolean(
            "hungerProtectionEnabled", category, true,
            "启用饥饿度保护(防止饥饿值过低时无法挖掘)||Enable hunger protection (prevent mining when hunger is too low)"
        );
        
        INSTANCE.hungerThreshold.value = config.getFloat(
            "hungerThreshold", category, 3.0f, 0.0f, 20.0f,
            "饥饿度阈值 (最小值为0，最大值为20)||Hunger threshold (minimum value is 0, maximum value is 20)"
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
    
    // 包装类
    public static class BooleanValue {
        private boolean value;
        public boolean get() { return value; }
        public void set(boolean value) { this.value = value; }
    }
    
    public static class DoubleValue {
        private double value;
        public double get() { return value; }
        public void set(double value) { this.value = value; }
    }
    
    public static Configuration getConfig() {
        return config;
    }
} 