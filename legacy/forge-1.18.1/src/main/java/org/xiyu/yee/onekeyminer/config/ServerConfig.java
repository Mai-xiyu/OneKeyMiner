package org.xiyu.yee.onekeyminer.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ServerConfig {
        public static final ForgeConfigSpec SPEC;
    public static final ServerConfig INSTANCE;
    
    static {
                final Pair<ServerConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder()
                .configure(ServerConfig::new);
        SPEC = specPair.getRight();
        INSTANCE = specPair.getLeft();
    }
    
    // 工具保护模式
        public final ForgeConfigSpec.BooleanValue toolProtectionEnabled;
        public final ForgeConfigSpec.DoubleValue toolDurabilityThreshold;
    
    // 饱食度保护
        public final ForgeConfigSpec.BooleanValue hungerProtectionEnabled;
        public final ForgeConfigSpec.DoubleValue hungerThreshold;
    

    
        private ServerConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("服务器配置||Server Config").push("server");
        
        toolProtectionEnabled = builder
                .comment("启用工具保护模式（防止工具耐久过低导致损坏）||Enable tool protection mode (prevent tool durability from being too low)")
                .translation("config.onekeyminer.toolProtectionEnabled")
                .define("toolProtectionEnabled", true);
        
        toolDurabilityThreshold = builder
                .comment("工具耐久阈值 (如果是小于等于1.0的值则表示百分比，大于1.0表示固定值)||Tool durability threshold (if the value is less than or equal to 1.0, it represents a percentage, otherwise it represents a fixed value)")
                .translation("config.onekeyminer.toolDurabilityThreshold")
                .defineInRange("toolDurabilityThreshold", 0.1, 0.01, 1000.0);
        
        hungerProtectionEnabled = builder
                .comment("启用饥饿度保护（防止饥饿值过低时无法挖掘）||Enable hunger protection (prevent mining when hunger is too low)")
                .translation("config.onekeyminer.hungerProtectionEnabled")
                .define("hungerProtectionEnabled", true);
        
        hungerThreshold = builder
                .comment("饥饿度阈值 (最小值为0，最大值为20)||Hunger threshold (minimum value is 0, maximum value is 20)")
                .translation("config.onekeyminer.hungerThreshold")
                .defineInRange("hungerThreshold", 3.0, 0.0, 20.0);
        

        
        builder.pop();
    }
} 