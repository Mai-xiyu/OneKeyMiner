package org.xiyu.onekeyminer.api;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.api.event.MiningEvents;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.MinerConfig;

import java.util.*;

/**
 * OneKeyMiner 公共 API
 * 
 * <p>提供给其他模组开发者使用的 API 接口，用于：</p>
 * <ul>
 *   <li>注册/注销方块白名单和黑名单</li>
 *   <li>注册/注销工具白名单和黑名单</li>
 *   <li>监听挖矿事件（PreMineEvent, PostMineEvent）</li>
 *   <li>查询和修改运行时状态</li>
 * </ul>
 * 
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 注册自定义矿石到白名单
 * OneKeyMinerAPI.registerBlock("mymod:custom_ore");
 * 
 * // 注册方块标签
 * OneKeyMinerAPI.registerBlockTag("mymod:custom_ores");
 * 
 * // 监听挖矿事件
 * MiningEvents.registerPreMineListener(event -> {
 *     if (isProtectedArea(event.getOriginPos())) {
 *         event.cancel();
 *     }
 * });
 * }</pre>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.0
 * @since Minecraft 1.21.9
 * @see MiningEvents
 */
public final class OneKeyMinerAPI {
    
    /** 运行时方块白名单 */
    private static final Set<ResourceLocation> BLOCK_WHITELIST = new HashSet<>();
    
    /** 运行时方块黑名单 */
    private static final Set<ResourceLocation> BLOCK_BLACKLIST = new HashSet<>();
    
    /** 运行时方块标签白名单 */
    private static final Set<TagKey<Block>> BLOCK_TAG_WHITELIST = new HashSet<>();
    
    /** 运行时工具白名单 */
    private static final Set<ResourceLocation> TOOL_WHITELIST = new HashSet<>();
    
    /** 运行时工具黑名单 */
    private static final Set<ResourceLocation> TOOL_BLACKLIST = new HashSet<>();
    
    /** 方块分组映射（用于宽松匹配） */
    private static final Map<ResourceLocation, String> BLOCK_GROUPS = new HashMap<>();
    
    private OneKeyMinerAPI() {
        // 私有构造函数，防止实例化
    }
    
    /**
     * 初始化 API
     * 
     * <p>由模组主类调用，加载配置中的白名单/黑名单</p>
     */
    public static void init() {
        loadFromConfig();
        OneKeyMiner.LOGGER.debug("OneKeyMiner API 已初始化");
    }
    
    /**
     * 从配置文件加载白名单/黑名单
     */
    public static void loadFromConfig() {
        MinerConfig config = ConfigManager.getConfig();
        
        // 加载自定义白名单
        for (String entry : config.customWhitelist) {
            if (entry.startsWith("#")) {
                registerBlockTag(entry.substring(1));
            } else {
                registerBlock(entry);
            }
        }
        
        // 加载黑名单
        for (String entry : config.blacklist) {
            if (entry.startsWith("#")) {
                // 标签黑名单暂不支持，记录日志
                OneKeyMiner.LOGGER.warn("方块标签黑名单暂不支持: {}", entry);
            } else {
                blacklistBlock(entry);
            }
        }
        
        // 加载工具白名单
        for (String entry : config.toolWhitelist) {
            whitelistTool(entry);
        }
        
        // 加载工具黑名单
        for (String entry : config.toolBlacklist) {
            blacklistTool(entry);
        }
    }
    
    // ==================== 方块白名单 API ====================
    
    /**
     * 注册方块到白名单
     * 
     * @param blockId 方块 ID，格式为 "namespace:path"（如 "minecraft:diamond_ore"）
     * @return 如果注册成功返回 true，如果已存在返回 false
     */
    public static boolean registerBlock(String blockId) {
        ResourceLocation loc = ResourceLocation.tryParse(blockId);
        if (loc == null) {
            OneKeyMiner.LOGGER.warn("无效的方块 ID: {}", blockId);
            return false;
        }
        return BLOCK_WHITELIST.add(loc);
    }
    
    /**
     * 注册方块到白名单
     * 
     * @param block 方块实例
     * @return 如果注册成功返回 true
     */
    public static boolean registerBlock(Block block) {
        ResourceLocation loc = BuiltInRegistries.BLOCK.getKey(block);
        return BLOCK_WHITELIST.add(loc);
    }
    
    /**
     * 注册方块标签到白名单
     * 
     * <p>标签中的所有方块都将被允许连锁挖矿</p>
     * 
     * @param tagId 标签 ID，格式为 "namespace:path"（如 "minecraft:coal_ores"）
     * @return 如果注册成功返回 true
     */
    public static boolean registerBlockTag(String tagId) {
        ResourceLocation loc = ResourceLocation.tryParse(tagId);
        if (loc == null) {
            OneKeyMiner.LOGGER.warn("无效的标签 ID: {}", tagId);
            return false;
        }
        TagKey<Block> tag = TagKey.create(BuiltInRegistries.BLOCK.key(), loc);
        return BLOCK_TAG_WHITELIST.add(tag);
    }
    
    /**
     * 从白名单移除方块
     * 
     * @param blockId 方块 ID
     * @return 如果移除成功返回 true
     */
    public static boolean unregisterBlock(String blockId) {
        ResourceLocation loc = ResourceLocation.tryParse(blockId);
        if (loc == null) return false;
        return BLOCK_WHITELIST.remove(loc);
    }
    
    /**
     * 从白名单移除方块标签
     * 
     * @param tagId 标签 ID
     * @return 如果移除成功返回 true
     */
    public static boolean unregisterBlockTag(String tagId) {
        ResourceLocation loc = ResourceLocation.tryParse(tagId);
        if (loc == null) return false;
        TagKey<Block> tag = TagKey.create(BuiltInRegistries.BLOCK.key(), loc);
        return BLOCK_TAG_WHITELIST.remove(tag);
    }
    
    // ==================== 方块黑名单 API ====================
    
    /**
     * 将方块添加到黑名单
     * 
     * <p>黑名单优先级高于白名单，被黑名单的方块不会被连锁挖矿</p>
     * 
     * @param blockId 方块 ID
     * @return 如果添加成功返回 true
     */
    public static boolean blacklistBlock(String blockId) {
        ResourceLocation loc = ResourceLocation.tryParse(blockId);
        if (loc == null) {
            OneKeyMiner.LOGGER.warn("无效的方块 ID: {}", blockId);
            return false;
        }
        return BLOCK_BLACKLIST.add(loc);
    }
    
    /**
     * 将方块添加到黑名单
     * 
     * @param block 方块实例
     * @return 如果添加成功返回 true
     */
    public static boolean blacklistBlock(Block block) {
        ResourceLocation loc = BuiltInRegistries.BLOCK.getKey(block);
        return BLOCK_BLACKLIST.add(loc);
    }
    
    /**
     * 从黑名单移除方块
     * 
     * @param blockId 方块 ID
     * @return 如果移除成功返回 true
     */
    public static boolean unblacklistBlock(String blockId) {
        ResourceLocation loc = ResourceLocation.tryParse(blockId);
        if (loc == null) return false;
        return BLOCK_BLACKLIST.remove(loc);
    }
    
    // ==================== 工具白名单/黑名单 API ====================
    
    /**
     * 将工具添加到白名单
     * 
     * @param itemId 物品 ID
     * @return 如果添加成功返回 true
     */
    public static boolean whitelistTool(String itemId) {
        ResourceLocation loc = ResourceLocation.tryParse(itemId);
        if (loc == null) {
            OneKeyMiner.LOGGER.warn("无效的物品 ID: {}", itemId);
            return false;
        }
        return TOOL_WHITELIST.add(loc);
    }
    
    /**
     * 将工具添加到黑名单
     * 
     * @param itemId 物品 ID
     * @return 如果添加成功返回 true
     */
    public static boolean blacklistTool(String itemId) {
        ResourceLocation loc = ResourceLocation.tryParse(itemId);
        if (loc == null) {
            OneKeyMiner.LOGGER.warn("无效的物品 ID: {}", itemId);
            return false;
        }
        return TOOL_BLACKLIST.add(loc);
    }
    
    // ==================== 方块分组 API ====================
    
    /**
     * 将方块添加到指定分组
     * 
     * <p>同一分组内的方块在宽松匹配模式下可以连锁</p>
     * 
     * @param blockId 方块 ID
     * @param groupId 分组 ID（任意字符串）
     */
    public static void addBlockToGroup(String blockId, String groupId) {
        ResourceLocation loc = ResourceLocation.tryParse(blockId);
        if (loc != null) {
            BLOCK_GROUPS.put(loc, groupId);
        }
    }
    
    /**
     * 检查两个方块是否在同一分组
     * 
     * @param block1 方块 1
     * @param block2 方块 2
     * @return 如果在同一分组返回 true
     */
    public static boolean areBlocksInSameGroup(Block block1, Block block2) {
        ResourceLocation loc1 = BuiltInRegistries.BLOCK.getKey(block1);
        ResourceLocation loc2 = BuiltInRegistries.BLOCK.getKey(block2);
        
        String group1 = BLOCK_GROUPS.get(loc1);
        String group2 = BLOCK_GROUPS.get(loc2);
        
        if (group1 != null && group1.equals(group2)) {
            return true;
        }
        
        // 检查是否在同一标签中
        for (TagKey<Block> tag : BLOCK_TAG_WHITELIST) {
            boolean inTag1 = block1.defaultBlockState().is(tag);
            boolean inTag2 = block2.defaultBlockState().is(tag);
            if (inTag1 && inTag2) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查两个方块是否共享同一个标签
     * 
     * @param block1 方块 1
     * @param block2 方块 2
     * @return 如果共享标签返回 true
     */
    public static boolean blocksShareTag(Block block1, Block block2) {
        // 检查是否在同一标签中
        for (TagKey<Block> tag : BLOCK_TAG_WHITELIST) {
            boolean inTag1 = block1.defaultBlockState().is(tag);
            boolean inTag2 = block2.defaultBlockState().is(tag);
            if (inTag1 && inTag2) {
                return true;
            }
        }
        return false;
    }
    
    // ==================== 交互工具 API ====================
    
    /** 交互工具白名单 */
    private static final Set<ResourceLocation> INTERACTION_TOOL_WHITELIST = new HashSet<>();
    
    /** 交互工具黑名单 */
    private static final Set<ResourceLocation> INTERACTION_TOOL_BLACKLIST = new HashSet<>();
    
    /**
     * 注册交互工具到白名单
     * 
     * @param itemId 物品 ID（支持标签格式 "#c:shears"）
     * @return 如果注册成功返回 true
     */
    public static boolean registerInteractionTool(String itemId) {
        ResourceLocation loc = ResourceLocation.tryParse(itemId.startsWith("#") ? itemId.substring(1) : itemId);
        if (loc == null) {
            OneKeyMiner.LOGGER.warn("无效的交互工具 ID: {}", itemId);
            return false;
        }
        return INTERACTION_TOOL_WHITELIST.add(loc);
    }
    
    /**
     * 检查物品是否为允许的交互工具
     * 
     * @param stack 物品栈
     * @return 如果允许返回 true
     */
    public static boolean isInteractionToolAllowed(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        
        ResourceLocation loc = BuiltInRegistries.ITEM.getKey(stack.getItem());
        
        // 检查黑名单
        if (INTERACTION_TOOL_BLACKLIST.contains(loc)) {
            return false;
        }
        
        // 检查白名单
        if (!INTERACTION_TOOL_WHITELIST.isEmpty()) {
            return INTERACTION_TOOL_WHITELIST.contains(loc);
        }
        
        return false;
    }
    
    // ==================== 种植物品 API ====================
    
    /** 种植物品白名单 */
    private static final Set<ResourceLocation> PLANTABLE_WHITELIST = new HashSet<>();
    
    /** 种植物品黑名单 */
    private static final Set<ResourceLocation> PLANTABLE_BLACKLIST = new HashSet<>();
    
    /**
     * 注册可种植物品
     * 
     * @param itemId 物品 ID（支持标签格式）
     * @return 如果注册成功返回 true
     */
    public static boolean registerPlantableItem(String itemId) {
        ResourceLocation loc = ResourceLocation.tryParse(itemId.startsWith("#") ? itemId.substring(1) : itemId);
        if (loc == null) {
            OneKeyMiner.LOGGER.warn("无效的种植物品 ID: {}", itemId);
            return false;
        }
        return PLANTABLE_WHITELIST.add(loc);
    }
    
    /**
     * 将种子添加到黑名单
     * 
     * @param itemId 物品 ID
     * @return 如果添加成功返回 true
     */
    public static boolean blacklistSeed(String itemId) {
        ResourceLocation loc = ResourceLocation.tryParse(itemId);
        if (loc == null) {
            OneKeyMiner.LOGGER.warn("无效的种子 ID: {}", itemId);
            return false;
        }
        return PLANTABLE_BLACKLIST.add(loc);
    }
    
    /**
     * 检查种子是否在黑名单中
     * 
     * @param item 物品
     * @return 如果在黑名单中返回 true
     */
    public static boolean isSeedBlacklisted(Item item) {
        ResourceLocation loc = BuiltInRegistries.ITEM.getKey(item);
        return PLANTABLE_BLACKLIST.contains(loc);
    }
    
    /**
     * 检查物品是否为允许的可种植物品
     * 
     * @param stack 物品栈
     * @return 如果允许返回 true
     */
    public static boolean isPlantableItemAllowed(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        
        ResourceLocation loc = BuiltInRegistries.ITEM.getKey(stack.getItem());
        
        // 检查黑名单
        if (PLANTABLE_BLACKLIST.contains(loc)) {
            return false;
        }
        
        // 检查白名单
        if (!PLANTABLE_WHITELIST.isEmpty()) {
            return PLANTABLE_WHITELIST.contains(loc);
        }
        
        return false;
    }
    
    // ==================== 查询 API ====================
    
    /**
     * 检查方块是否允许连锁挖矿
     * 
     * @param block 方块实例
     * @return 如果允许返回 true
     */
    public static boolean isBlockAllowed(Block block) {
        ResourceLocation loc = BuiltInRegistries.BLOCK.getKey(block);
        MinerConfig config = ConfigManager.getConfig();
        
        // 检查黑名单（优先）
        if (BLOCK_BLACKLIST.contains(loc)) {
            return false;
        }
        
        // 如果开启了"挖掘所有方块"模式，不在黑名单中的方块都允许
        if (config.mineAllBlocks) {
            return true;
        }
        
        // 否则检查白名单
        if (BLOCK_WHITELIST.contains(loc)) {
            return true;
        }
        
        // 检查标签白名单
        for (TagKey<Block> tag : BLOCK_TAG_WHITELIST) {
            if (block.defaultBlockState().is(tag)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查方块是否在黑名单中
     * 
     * @param block 方块实例
     * @return 如果在黑名单中返回 true
     */
    public static boolean isBlockBlacklisted(Block block) {
        ResourceLocation loc = BuiltInRegistries.BLOCK.getKey(block);
        return BLOCK_BLACKLIST.contains(loc);
    }
    
    /**
     * 检查工具是否允许触发连锁挖矿
     * 
     * @param tool 工具物品栈
     * @return 如果允许返回 true
     */
    public static boolean isToolAllowed(ItemStack tool) {
        MinerConfig config = ConfigManager.getConfig();
        
        if (tool.isEmpty()) {
            // 空手是否允许？根据配置决定
            return config.allowBareHand;
        }
        
        ResourceLocation loc = BuiltInRegistries.ITEM.getKey(tool.getItem());
        
        // 检查黑名单（优先）
        if (TOOL_BLACKLIST.contains(loc)) {
            return false;
        }
        
        // 如果白名单为空，允许所有工具
        if (TOOL_WHITELIST.isEmpty()) {
            return true;
        }
        
        // 检查白名单
        return TOOL_WHITELIST.contains(loc);
    }
    
    /**
     * 获取所有已注册的方块白名单（只读）
     * 
     * @return 方块白名单的不可变集合
     */
    public static Set<ResourceLocation> getBlockWhitelist() {
        return Collections.unmodifiableSet(BLOCK_WHITELIST);
    }
    
    /**
     * 获取所有已注册的方块黑名单（只读）
     * 
     * @return 方块黑名单的不可变集合
     */
    public static Set<ResourceLocation> getBlockBlacklist() {
        return Collections.unmodifiableSet(BLOCK_BLACKLIST);
    }
    
    /**
     * 清除所有运行时注册的数据
     * 
     * <p>通常在配置重载时调用</p>
     */
    public static void clearAll() {
        BLOCK_WHITELIST.clear();
        BLOCK_BLACKLIST.clear();
        BLOCK_TAG_WHITELIST.clear();
        TOOL_WHITELIST.clear();
        TOOL_BLACKLIST.clear();
        BLOCK_GROUPS.clear();
    }
    
    /**
     * 重新加载 API 数据
     * 
     * <p>清除现有数据并从配置重新加载</p>
     */
    public static void reload() {
        clearAll();
        loadFromConfig();
        // 重新注册默认方块（由主类处理）
        OneKeyMiner.LOGGER.info("OneKeyMiner API 已重载");
    }
}
