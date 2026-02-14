package org.xiyu.onekeyminer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.platform.PlatformServices;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 配置管理器
 * 
 * <p>负责加载、保存和管理模组的所有配置选项。
 * 支持热重载，配置文件使用 JSON 格式存储。</p>
 * 
 * <h2>配置文件位置</h2>
 * <ul>
 *   <li>Fabric: {@code .minecraft/config/onekeyminer.json}</li>
 *   <li>NeoForge/Forge: {@code .minecraft/config/onekeyminer.json}</li>
 * </ul>
 * 
 * @author Mai_xiyu
 * @version 2.0.0
 * @since Minecraft 1.21.9
 */
public class ConfigManager {
    
    /** 配置文件名 */
    private static final String CONFIG_FILE_NAME = "onekeyminer.json";
    
    /** JSON 序列化器 */
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    
    /** 当前配置实例（线程安全） */
    private static final AtomicReference<MinerConfig> CONFIG = new AtomicReference<>(new MinerConfig());
    
    /** 配置变更监听器列表 */
    private static final List<ConfigChangeListener> LISTENERS = new ArrayList<>();
    
    /**
     * 加载配置文件
     * 
     * <p>如果配置文件不存在，将创建默认配置并保存。</p>
     */
    public static void load() {
        Path configPath = getConfigPath();
        
        try {
            if (Files.exists(configPath)) {
                String json = Files.readString(configPath);
                MinerConfig loaded = GSON.fromJson(json, MinerConfig.class);
                if (loaded != null) {
                    CONFIG.set(loaded);
                    OneKeyMiner.LOGGER.info("配置文件加载成功: {}", configPath);
                }
            } else {
                // 创建默认配置
                save();
                OneKeyMiner.LOGGER.info("已创建默认配置文件: {}", configPath);
            }
        } catch (IOException e) {
            OneKeyMiner.LOGGER.error("加载配置文件失败: {}", e.getMessage());
        }
        
        // 验证配置值
        validateConfig();
    }
    
    /**
     * 保存当前配置到文件
     */
    public static void save() {
        Path configPath = getConfigPath();
        
        try {
            // 确保目录存在
            Files.createDirectories(configPath.getParent());
            
            String json = GSON.toJson(CONFIG.get());
            Files.writeString(configPath, json);
            OneKeyMiner.LOGGER.debug("配置文件已保存: {}", configPath);
        } catch (IOException e) {
            OneKeyMiner.LOGGER.error("保存配置文件失败: {}", e.getMessage());
        }
    }
    
    /**
     * 热重载配置
     * 
     * <p>从磁盘重新加载配置文件，进行逐字段验证。
     * 有效的字段被接受，无效字段回退到内存中的旧值。</p>
     */
    public static void reload() {
        MinerConfig oldConfig = CONFIG.get();
        
        Path configPath = getConfigPath();
        MinerConfig diskConfig = null;
        
        try {
            if (Files.exists(configPath)) {
                String json = Files.readString(configPath);
                diskConfig = GSON.fromJson(json, MinerConfig.class);
            }
        } catch (Exception e) {
            OneKeyMiner.LOGGER.error("重载配置文件失败: {}", e.getMessage());
            return; // 解析完全失败，保留旧配置
        }
        
        if (diskConfig == null) {
            OneKeyMiner.LOGGER.warn("配置文件解析结果为 null，保留当前配置");
            return;
        }
        
        // 逐字段验证并合并
        MinerConfig merged = oldConfig.copy();
        List<String> rejected = new ArrayList<>();
        
        // 基础设置
        merged.enabled = diskConfig.enabled;
        
        // selectedShape: 验证是否为已注册形状
        if (diskConfig.selectedShape != null && ShapeRegistry.isValidShapeId(diskConfig.selectedShape)) {
            merged.selectedShape = diskConfig.selectedShape;
        } else if (diskConfig.selectedShape != null) {
            rejected.add("selectedShape (无效的形状 ID: " + diskConfig.selectedShape + ")");
        }
        
        // maxBlocks: 1 - 10240
        if (diskConfig.maxBlocks >= 1 && diskConfig.maxBlocks <= 10240) {
            merged.maxBlocks = diskConfig.maxBlocks;
        } else {
            rejected.add("maxBlocks (" + diskConfig.maxBlocks + " 超出范围 1-10240)");
        }
        
        // maxDistance: 1 - 128
        if (diskConfig.maxDistance >= 1 && diskConfig.maxDistance <= 128) {
            merged.maxDistance = diskConfig.maxDistance;
        } else {
            rejected.add("maxDistance (" + diskConfig.maxDistance + " 超出范围 1-128)");
        }
        
        // 布尔值字段直接接受
        merged.allowDiagonal = diskConfig.allowDiagonal;
        merged.consumeDurability = diskConfig.consumeDurability;
        merged.stopOnLowDurability = diskConfig.stopOnLowDurability;
        merged.consumeHunger = diskConfig.consumeHunger;
        merged.mineAllBlocks = diskConfig.mineAllBlocks;
        merged.allowBareHand = diskConfig.allowBareHand;
        merged.teleportDrops = diskConfig.teleportDrops;
        merged.teleportExp = diskConfig.teleportExp;
        merged.requireExactMatch = diskConfig.requireExactMatch;
        merged.playSound = diskConfig.playSound;
        merged.showStats = diskConfig.showStats;
        merged.enableInteraction = diskConfig.enableInteraction;
        merged.enablePlanting = diskConfig.enablePlanting;
        merged.enableHarvesting = diskConfig.enableHarvesting;
        merged.harvestReplant = diskConfig.harvestReplant;
        merged.strictBlockMatching = diskConfig.strictBlockMatching;
        
        // preserveDurability: >= 0
        if (diskConfig.preserveDurability >= 0) {
            merged.preserveDurability = diskConfig.preserveDurability;
        } else {
            rejected.add("preserveDurability (" + diskConfig.preserveDurability + " < 0)");
        }
        
        // hungerMultiplier: 0 - 10
        if (diskConfig.hungerMultiplier >= 0 && diskConfig.hungerMultiplier <= 10) {
            merged.hungerMultiplier = diskConfig.hungerMultiplier;
        } else {
            rejected.add("hungerMultiplier (" + diskConfig.hungerMultiplier + " 超出范围 0-10)");
        }
        
        // minHungerLevel: 0 - 20
        if (diskConfig.minHungerLevel >= 0 && diskConfig.minHungerLevel <= 20) {
            merged.minHungerLevel = diskConfig.minHungerLevel;
        } else {
            rejected.add("minHungerLevel (" + diskConfig.minHungerLevel + " 超出范围 0-20)");
        }
        
        // hungerPerBlock: >= 0
        if (diskConfig.hungerPerBlock >= 0) {
            merged.hungerPerBlock = diskConfig.hungerPerBlock;
        } else {
            rejected.add("hungerPerBlock (" + diskConfig.hungerPerBlock + " < 0)");
        }
        
        // maxBlocksCreative: >= 1
        if (diskConfig.maxBlocksCreative >= 1) {
            merged.maxBlocksCreative = diskConfig.maxBlocksCreative;
        } else {
            rejected.add("maxBlocksCreative (" + diskConfig.maxBlocksCreative + " < 1)");
        }
        
        // 列表字段直接接受（null 时保留旧值）
        if (diskConfig.customWhitelist != null) merged.customWhitelist = new ArrayList<>(diskConfig.customWhitelist);
        if (diskConfig.blacklist != null) merged.blacklist = new ArrayList<>(diskConfig.blacklist);
        if (diskConfig.toolWhitelist != null) merged.toolWhitelist = new ArrayList<>(diskConfig.toolWhitelist);
        if (diskConfig.toolBlacklist != null) merged.toolBlacklist = new ArrayList<>(diskConfig.toolBlacklist);
        if (diskConfig.interactionToolWhitelist != null) merged.interactionToolWhitelist = new ArrayList<>(diskConfig.interactionToolWhitelist);
        if (diskConfig.interactionToolBlacklist != null) merged.interactionToolBlacklist = new ArrayList<>(diskConfig.interactionToolBlacklist);
        if (diskConfig.seedWhitelist != null) merged.seedWhitelist = new ArrayList<>(diskConfig.seedWhitelist);
        if (diskConfig.seedBlacklist != null) merged.seedBlacklist = new ArrayList<>(diskConfig.seedBlacklist);
        if (diskConfig.farmlandWhitelist != null) merged.farmlandWhitelist = new ArrayList<>(diskConfig.farmlandWhitelist);
        if (diskConfig.interactiveItemWhitelist != null) merged.interactiveItemWhitelist = new ArrayList<>(diskConfig.interactiveItemWhitelist);
        if (diskConfig.interactiveItemBlacklist != null) merged.interactiveItemBlacklist = new ArrayList<>(diskConfig.interactiveItemBlacklist);
        
        // 应用合并后的配置
        CONFIG.set(merged);
        
        // 报告结果
        if (!rejected.isEmpty()) {
            OneKeyMiner.LOGGER.warn("配置热重载：以下字段验证失败，已回退旧值: {}", String.join(", ", rejected));
        }
        
        // 通知所有监听器
        MinerConfig newConfig = CONFIG.get();
        LISTENERS.forEach(listener -> listener.onConfigChanged(oldConfig, newConfig));
        
        OneKeyMiner.LOGGER.info("配置已热重载 (接受: {}, 拒绝: {})", 
                "大部分字段", rejected.isEmpty() ? "无" : rejected.size() + " 个");
    }
    
    /**
     * 获取当前配置实例
     * 
     * @return 当前配置（不可变副本）
     */
    public static MinerConfig getConfig() {
        return CONFIG.get();
    }
    
    /**
     * 更新配置并保存
     * 
     * @param newConfig 新的配置实例
     */
    public static void updateConfig(MinerConfig newConfig) {
        MinerConfig oldConfig = CONFIG.get();
        CONFIG.set(newConfig);
        validateConfig();
        save();
        
        // 通知所有监听器
        LISTENERS.forEach(listener -> listener.onConfigChanged(oldConfig, newConfig));
    }
    
    /**
     * 注册配置变更监听器
     * 
     * @param listener 监听器实例
     */
    public static void addListener(ConfigChangeListener listener) {
        LISTENERS.add(listener);
    }
    
    /**
     * 移除配置变更监听器
     * 
     * @param listener 监听器实例
     */
    public static void removeListener(ConfigChangeListener listener) {
        LISTENERS.remove(listener);
    }
    
    /**
     * 获取配置文件路径
     */
    private static Path getConfigPath() {
        return PlatformServices.getInstance().getConfigDirectory().resolve(CONFIG_FILE_NAME);
    }
    
    /**
     * 验证配置值，确保在有效范围内
     */
    private static void validateConfig() {
        MinerConfig config = CONFIG.get();
        boolean changed = false;

        if (config.selectedShape == null || config.selectedShape.isEmpty()) {
            config.selectedShape = "onekeyminer:amorphous";
            changed = true;
            OneKeyMiner.LOGGER.warn("selectedShape 为空，已重置为默认值 onekeyminer:amorphous");
        }
        
        // 限制最大方块数量
        if (config.maxBlocks < 1) {
            config.maxBlocks = 1;
            changed = true;
        } else if (config.maxBlocks > 10240) {
            config.maxBlocks = 10240;
            changed = true;
        }
        
        // 限制最大距离
        if (config.maxDistance < 1) {
            config.maxDistance = 1;
            changed = true;
        } else if (config.maxDistance > 128) {
            config.maxDistance = 128;
            changed = true;
        }
        
        // 限制饥饿消耗倍率
        if (config.hungerMultiplier < 0) {
            config.hungerMultiplier = 0;
            changed = true;
        } else if (config.hungerMultiplier > 10) {
            config.hungerMultiplier = 10;
            changed = true;
        }
        
        if (changed) {
            OneKeyMiner.LOGGER.warn("部分配置值超出有效范围，已自动修正");
        }
    }
    
    /**
     * 配置变更监听器接口
     */
    @FunctionalInterface
    public interface ConfigChangeListener {
        /**
         * 当配置发生变更时调用
         * 
         * @param oldConfig 旧配置
         * @param newConfig 新配置
         */
        void onConfigChanged(MinerConfig oldConfig, MinerConfig newConfig);
    }
}
