package org.xiyu.onekeyminer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xiyu.onekeyminer.api.OneKeyMinerAPI;
import org.xiyu.onekeyminer.config.ConfigManager;

/**
 * OneKeyMiner 模组主类
 * 
 * <p>一键连锁挖矿模组的核心入口点，负责初始化模组的所有核心组件。
 * 本模组采用多加载器架构，支持 Fabric、NeoForge 和 Forge 平台。</p>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.0
 * @since Minecraft 1.21.9
 */
public class OneKeyMiner {
    
    /** 模组唯一标识符 */
    public static final String MOD_ID = "onekeyminer";
    
    /** 模组名称 */
    public static final String MOD_NAME = "OneKeyMiner";
    
    /** 模组版本 */
    public static final String VERSION = "1.0.0";
    
    /** 模组日志记录器 */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
    
    /** 模组是否已初始化 */
    private static boolean initialized = false;
    
    /**
     * 初始化模组
     * 
     * <p>该方法由各平台加载器调用，负责：</p>
     * <ul>
     *   <li>加载配置文件</li>
     *   <li>初始化 API 系统</li>
     *   <li>注册默认的方块白名单</li>
     * </ul>
     */
    public static void init() {
        if (initialized) {
            LOGGER.warn("OneKeyMiner 已经初始化，跳过重复初始化");
            return;
        }
        
        LOGGER.info("正在初始化 {} v{}", MOD_NAME, VERSION);
        
        // 加载配置
        ConfigManager.load();
        
        // 初始化 API
        OneKeyMinerAPI.init();
        
        // 注册默认方块白名单
        registerDefaultBlocks();
        
        initialized = true;
        LOGGER.info("{} 初始化完成！", MOD_NAME);
    }
    
    /**
     * 注册默认的可连锁挖掘方块
     * 
     * <p>包含常见的矿石、原木等方块类型</p>
     */
    private static void registerDefaultBlocks() {
        // 矿石类 - 使用标签注册
        OneKeyMinerAPI.registerBlockTag("minecraft:coal_ores");
        OneKeyMinerAPI.registerBlockTag("minecraft:iron_ores");
        OneKeyMinerAPI.registerBlockTag("minecraft:gold_ores");
        OneKeyMinerAPI.registerBlockTag("minecraft:diamond_ores");
        OneKeyMinerAPI.registerBlockTag("minecraft:emerald_ores");
        OneKeyMinerAPI.registerBlockTag("minecraft:lapis_ores");
        OneKeyMinerAPI.registerBlockTag("minecraft:redstone_ores");
        OneKeyMinerAPI.registerBlockTag("minecraft:copper_ores");
        
        // 原木类
        OneKeyMinerAPI.registerBlockTag("minecraft:logs");
        
        // 树叶类（可选）
        OneKeyMinerAPI.registerBlockTag("minecraft:leaves");
        
        // 下界矿石
        OneKeyMinerAPI.registerBlockTag("minecraft:nether_gold_ore");
        OneKeyMinerAPI.registerBlock("minecraft:ancient_debris");
        OneKeyMinerAPI.registerBlock("minecraft:nether_quartz_ore");
        
        LOGGER.debug("已注册默认方块白名单");
    }
    
    /**
     * 获取模组是否已初始化
     * 
     * @return 如果模组已初始化返回 true
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
