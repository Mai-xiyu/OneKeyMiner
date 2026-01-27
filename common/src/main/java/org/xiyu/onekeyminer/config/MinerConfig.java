package org.xiyu.onekeyminer.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 模组配置数据类
 * 
 * <p>包含所有可配置的选项，使用 GSON 进行序列化。
 * 所有字段都有合理的默认值。</p>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.0
 * @since Minecraft 1.21.9
 */
public class MinerConfig {
    
    // ==================== 基础设置 ====================
    
    /**
     * 是否启用模组
     * <p>设为 false 时完全禁用连锁挖矿功能</p>
     */
    public boolean enabled = true;
    
    // 激活模式已简化：始终使用按住按键激活
    
    // ==================== 挖矿限制 ====================
    
    /**
     * 单次连锁挖矿的最大方块数量
     * <p>范围: 1 - 1000，默认: 64</p>
     */
    public int maxBlocks = 64;
    
    /**
     * 连锁挖矿的最大距离（从起始点算起）
     * <p>范围: 1 - 64，默认: 16，单位: 方块</p>
     */
    public int maxDistance = 16;
    
    /**
     * 是否允许对角线连锁（26 向搜索 vs 6 向搜索）
     * <p>启用后会包括斜向相邻的方块</p>
     */
    public boolean allowDiagonal = true;
    
    /**
     * 搜索形状模式
     */
    public ShapeMode shapeMode = ShapeMode.CONNECTED;
    
    // ==================== 消耗设置 ====================
    
    /**
     * 是否消耗工具耐久度
     * <p>每个连锁挖掘的方块都会消耗耐久</p>
     */
    public boolean consumeDurability = true;
    
    /**
     * 当工具耐久不足时是否停止挖矿
     * <p>设为 true 可防止工具意外损坏</p>
     */
    public boolean stopOnLowDurability = true;
    
    /**
     * 保留工具的最低耐久值
     * <p>当工具耐久度达到此值时停止挖矿</p>
     */
    public int preserveDurability = 1;
    
    /**
     * 是否消耗饥饿值
     */
    public boolean consumeHunger = true;
    
    /**
     * 饥饿消耗倍率
     * <p>每个方块的额外饥饿消耗 = 基础消耗 × 此倍率</p>
     */
    public float hungerMultiplier = 1.0f;
    
    /**
     * 最低饥饿值限制
     * <p>当玩家饥饿值低于此值时停止挖矿</p>
     */
    public int minHungerLevel = 1;
    
    // ==================== 交互功能设置 ====================
    
    /**
     * 是否启用连锁交互功能
     * <p>支持剪羊毛、耕地、剥皮原木等右键交互</p>
     */
    public boolean enableInteraction = true;
    
    /**
     * 交互工具白名单
     * <p>支持标签格式如 "#c:shears"、"#c:hoes"</p>
     */
    public List<String> interactionToolWhitelist = new ArrayList<>();
    
    /**
     * 交互工具黑名单
     */
    public List<String> interactionToolBlacklist = new ArrayList<>();
    
    // ==================== 种植功能设置 ====================
    
    /**
     * 是否启用连锁种植功能
     * <p>批量种植作物、树苗等</p>
     */
    public boolean enablePlanting = true;
    
    /**
     * 种子/树苗白名单
     * <p>支持标签格式如 "#c:seeds"、"#minecraft:saplings"</p>
     */
    public List<String> seedWhitelist = new ArrayList<>();
    
    /**
     * 种子/树苗黑名单
     */
    public List<String> seedBlacklist = new ArrayList<>();
    
    /**
     * 可种植耕地白名单
     * <p>支持标签格式如 "#c:farmland"</p>
     */
    public List<String> farmlandWhitelist = new ArrayList<>();
    
    // ==================== 高级设置 ====================
    
    /**
     * 每个方块消耗的饥饿值
     */
    public float hungerPerBlock = 0.025f;
    
    /**
     * 创造模式最大方块数
     */
    public int maxBlocksCreative = 256;
    
    // requireSneak 已移除
    
    /**
     * 是否严格匹配方块类型
     * <p>设为 true 只匹配完全相同的方块</p>
     */
    public boolean strictBlockMatching = false;
    
    // ==================== 连锁范围设置 ====================
    
    /**
     * 是否允许对所有方块连锁（除黑名单外）
     * <p>设为 true 时，白名单不工作，除黑名单外所有方块都可以连锁</p>
     * <p>设为 false 时，只有白名单中的方块才能连锁</p>
     */
    public boolean mineAllBlocks = true;
    
    /**
     * 是否允许空手连锁
     * <p>设为 true 时，即使不持有工具也可以触发连锁</p>
     */
    public boolean allowBareHand = true;
    
    // ==================== 掉落物设置 ====================
    
    /**
     * 是否将掉落物直接传送到玩家
     * <p>设为 true 时，连锁挖掘的掉落物会直接进入玩家背包</p>
     */
    public boolean teleportDrops = false;
    
    /**
     * 是否将经验直接传送到玩家
     * <p>设为 true 时，经验会合并成一个经验球并直接给予玩家</p>
     */
    public boolean teleportExp = false;
    
    // ==================== 高级设置 ====================
    
    /**
     * 是否只在相同方块间连锁
     * <p>设为 false 时，同一矿石标签的不同变体可以连锁</p>
     */
    public boolean requireExactMatch = false;
    
    /**
     * 是否播放连锁挖矿音效
     */
    public boolean playSound = true;
    
    /**
     * 是否显示挖矿统计提示
     */
    public boolean showStats = true;
    
    // ==================== 方块过滤 ====================
    
    /**
     * 自定义方块白名单
     * <p>格式: "minecraft:block_id" 或 "#minecraft:tag_id"</p>
     */
    public List<String> customWhitelist = new ArrayList<>();
    
    /**
     * 方块黑名单
     * <p>优先级高于白名单，格式同上</p>
     */
    public List<String> blacklist = new ArrayList<>();
    
    /**
     * 工具白名单（留空表示允许所有工具）
     * <p>格式: "minecraft:item_id"</p>
     */
    public List<String> toolWhitelist = new ArrayList<>();
    
    /**
     * 工具黑名单
     */
    public List<String> toolBlacklist = new ArrayList<>();
    
    // ==================== 枚举定义 ====================
    
    // ActivationMode 枚举已移除，现在始终使用按住按键激活模式
    
    /**
     * 搜索形状模式枚举
     */
    public enum ShapeMode {
        /** 相连的同类方块 */
        CONNECTED("connected", "相邻连接"),
        /** 以起始点为中心的立方体范围 */
        CUBE("cube", "立方体范围"),
        /** 垂直方向的柱状范围 */
        COLUMN("column", "垂直柱状");
        
        private final String id;
        private final String displayName;
        
        ShapeMode(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
        
        public String getId() {
            return id;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 创建配置的深拷贝
     * 
     * @return 配置副本
     */
    public MinerConfig copy() {
        MinerConfig copy = new MinerConfig();
        copy.enabled = this.enabled;
        copy.maxBlocks = this.maxBlocks;
        copy.maxDistance = this.maxDistance;
        copy.allowDiagonal = this.allowDiagonal;
        copy.shapeMode = this.shapeMode;
        copy.consumeDurability = this.consumeDurability;
        copy.stopOnLowDurability = this.stopOnLowDurability;
        copy.preserveDurability = this.preserveDurability;
        copy.consumeHunger = this.consumeHunger;
        copy.hungerMultiplier = this.hungerMultiplier;
        copy.minHungerLevel = this.minHungerLevel;
        copy.mineAllBlocks = this.mineAllBlocks;
        copy.allowBareHand = this.allowBareHand;
        copy.teleportDrops = this.teleportDrops;
        copy.teleportExp = this.teleportExp;
        copy.requireExactMatch = this.requireExactMatch;
        copy.playSound = this.playSound;
        copy.showStats = this.showStats;
        copy.customWhitelist = new ArrayList<>(this.customWhitelist);
        copy.blacklist = new ArrayList<>(this.blacklist);
        copy.toolWhitelist = new ArrayList<>(this.toolWhitelist);
        copy.toolBlacklist = new ArrayList<>(this.toolBlacklist);
        return copy;
    }
}
