package org.xiyu.yee.onekeyminer.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * 客户端配置类 - 处理只在客户端生效的配置项
 */
public class ClientConfig {
        public static final ClientConfig INSTANCE;
        public static final ForgeConfigSpec SPEC;
    
    static {
                Pair<ClientConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        INSTANCE = specPair.getLeft();
        SPEC = specPair.getRight();
    }
    
    // 是否需要按住键来激活连锁模式
        public ForgeConfigSpec.BooleanValue requireKeyHold;
    
    // 是否显示连锁方块数量
        public ForgeConfigSpec.BooleanValue showBlockCount;
        public ForgeConfigSpec.BooleanValue Debug;
    // 消息显示样式（操作栏或聊天栏）
        public ForgeConfigSpec.ConfigValue<String> messageStyle;
        public  ForgeConfigSpec.IntValue frozentimer;
        private ClientConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("一键连锁 - 客户端配置||OneKeyMiner Client Config").push("client");
        
        // 按键模式配置
        requireKeyHold = builder
                .comment("是否需要按住键来激活连锁模式||Whether to hold the key to activate the chain mode")
                .translation("config.onekeyminer.requireKeyHold")
                .define("requireKeyHold", false);
        // 调试模式配置
        Debug = builder
                .comment("是否启用调试模式||Whether to enable debug mode")
                .translation("config.onekeyminer.Debug")
                .define("Debug", false);
        
        // 显示数量配置
        showBlockCount = builder
                .comment("是否显示连锁方块数量||Whether to show the number of chain blocks")
                .translation("config.onekeyminer.showBlockCount")
                .define("showBlockCount", true);
        
        // 消息样式配置
        messageStyle = builder
                .comment("消息显示样式 (actionbar 或 chat)||Message display style (actionbar or chat)")
                .translation("config.onekeyminer.messageStyle")
                .define("messageStyle", "actionbar");
        frozentimer = builder
                .comment("按住模式关闭时，连锁状态重置时间（秒）||Chain state reset time when hold mode is off (seconds)")
                .translation("config.onekeyminer.frozentimer")
                .defineInRange("frozen timer", 60, 1, 32767);
        builder.pop();
    }
} 