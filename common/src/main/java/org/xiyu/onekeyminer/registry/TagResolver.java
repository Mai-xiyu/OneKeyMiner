package org.xiyu.onekeyminer.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.onekeyminer.OneKeyMiner;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 标签解析器
 * 
 * <p>统一处理方块和物品的标签解析，支持以下格式：</p>
 * <ul>
 *   <li>单个 ID: {@code minecraft:diamond_ore}</li>
 *   <li>标签格式 1: {@code #minecraft:logs}</li>
 *   <li>标签格式 2: {@code #c:shears}（Fabric/NeoForge 通用标签）</li>
 *   <li>标签格式 3: {@code #forge:seeds}（Forge 通用标签）</li>
 *   <li>通配符: {@code minecraft:*_ore}（匹配所有矿石）</li>
 * </ul>
 * 
 * <p>不同平台的共用标签命名空间不同：Fabric/NeoForge 使用 {@code c}，
 * Forge 使用 {@code forge}。可通过 {@code PlatformServices.getInstance().getConventionalTagPrefix()}
 * 获取当前平台的正确前缀。</p>
 *
 * <p>使用缓存机制优化性能，避免频繁的标签查询。</p>
 * 
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.21.9
 */
public final class TagResolver {
    
    /** 标签前缀 */
    private static final String TAG_PREFIX = "#";
    
    /** 通配符字符 */
    private static final String WILDCARD = "*";
    
    /** 通配符正则模式缓存 */
    private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();
    
    /** 方块匹配结果缓存 */
    private static final Map<String, Set<Identifier>> BLOCK_CACHE = new ConcurrentHashMap<>();
    
    /** 物品匹配结果缓存 */
    private static final Map<String, Set<Identifier>> ITEM_CACHE = new ConcurrentHashMap<>();
    
    private TagResolver() {
        // 工具类，禁止实例化
    }
    
    /**
     * 清除所有缓存
     * 
     * <p>应在资源包重载或配置变更时调用</p>
     */
    public static void clearCache() {
        PATTERN_CACHE.clear();
        BLOCK_CACHE.clear();
        ITEM_CACHE.clear();
        OneKeyMiner.LOGGER.debug("标签解析器缓存已清除");
    }
    
    // ==================== 方块标签解析 ====================
    
    /**
     * 解析方块标签/ID 字符串
     * 
     * @param entry 配置条目（如 "#minecraft:logs" 或 "minecraft:oak_log"）
     * @return 解析出的方块 TagKey，如果是单个方块则返回 null
     */
    public static TagKey<Block> parseBlockTag(String entry) {
        if (entry == null || entry.isEmpty()) {
            return null;
        }
        
        if (entry.startsWith(TAG_PREFIX)) {
            String tagId = entry.substring(1);
            Identifier loc = Identifier.tryParse(tagId);
            if (loc != null) {
                return TagKey.create(Registries.BLOCK, loc);
            }
        }
        
        return null;
    }
    
    /**
     * 解析单个方块 ID
     * 
     * @param entry 配置条目
    * @return 方块的 Identifier，如果是标签则返回 null
     */
    public static Identifier parseBlockId(String entry) {
        if (entry == null || entry.isEmpty() || entry.startsWith(TAG_PREFIX)) {
            return null;
        }
        
        return Identifier.tryParse(entry);
    }
    
    /**
     * 检查方块是否匹配配置条目
     * 
     * @param block 要检查的方块
     * @param entry 配置条目（标签或单个ID）
     * @return 如果匹配返回 true
     */
    public static boolean matchesBlock(Block block, String entry) {
        if (entry == null || entry.isEmpty()) {
            return false;
        }
        
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        
        // 检查是否是标签
        if (entry.startsWith(TAG_PREFIX)) {
            TagKey<Block> tag = parseBlockTag(entry);
            if (tag != null) {
                return block.builtInRegistryHolder().is(tag);
            }
            return false;
        }
        
        // 检查是否包含通配符
        if (entry.contains(WILDCARD)) {
            Pattern pattern = getOrCreatePattern(entry);
            return pattern.matcher(blockId.toString()).matches();
        }
        
        // 精确匹配
        Identifier entryLoc = Identifier.tryParse(entry);
        return entryLoc != null && entryLoc.equals(blockId);
    }
    
    /**
     * 检查方块状态是否匹配任一配置条目
     * 
     * @param state 方块状态
     * @param entries 配置条目列表
     * @return 如果匹配任一条目返回 true
     */
    public static boolean matchesAnyBlock(BlockState state, List<String> entries) {
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        
        Block block = state.getBlock();
        for (String entry : entries) {
            if (matchesBlock(block, entry)) {
                return true;
            }
        }
        
        return false;
    }
    
    // ==================== 物品标签解析 ====================
    
    /**
     * 解析物品标签字符串
     * 
     * @param entry 配置条目（如 "#c:shears"）
     * @return 解析出的物品 TagKey，如果是单个物品则返回 null
     */
    public static TagKey<Item> parseItemTag(String entry) {
        if (entry == null || entry.isEmpty()) {
            return null;
        }
        
        if (entry.startsWith(TAG_PREFIX)) {
            String tagId = entry.substring(1);
            Identifier loc = Identifier.tryParse(tagId);
            if (loc != null) {
                return TagKey.create(Registries.ITEM, loc);
            }
        }
        
        return null;
    }
    
    /**
     * 解析单个物品 ID
     * 
     * @param entry 配置条目
    * @return 物品的 Identifier，如果是标签则返回 null
     */
    public static Identifier parseItemId(String entry) {
        if (entry == null || entry.isEmpty() || entry.startsWith(TAG_PREFIX)) {
            return null;
        }
        
        return Identifier.tryParse(entry);
    }
    
    /**
     * 检查物品是否匹配配置条目
     * 
     * @param item 要检查的物品
     * @param entry 配置条目（标签或单个ID）
     * @return 如果匹配返回 true
     */
    public static boolean matchesItem(Item item, String entry) {
        if (entry == null || entry.isEmpty()) {
            return false;
        }
        
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        
        // 检查是否是标签
        if (entry.startsWith(TAG_PREFIX)) {
            TagKey<Item> tag = parseItemTag(entry);
            if (tag != null) {
                return item.builtInRegistryHolder().is(tag);
            }
            return false;
        }
        
        // 检查是否包含通配符
        if (entry.contains(WILDCARD)) {
            Pattern pattern = getOrCreatePattern(entry);
            return pattern.matcher(itemId.toString()).matches();
        }
        
        // 精确匹配
        Identifier entryLoc = Identifier.tryParse(entry);
        return entryLoc != null && entryLoc.equals(itemId);
    }
    
    /**
     * 检查物品是否匹配任一配置条目
     * 
     * @param item 物品
     * @param entries 配置条目列表
     * @return 如果匹配任一条目返回 true
     */
    public static boolean matchesAnyItem(Item item, List<String> entries) {
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        
        for (String entry : entries) {
            if (matchesItem(item, entry)) {
                return true;
            }
        }
        
        return false;
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 获取或创建通配符正则表达式
     * 
     * @param pattern 通配符模式
     * @return 编译后的正则表达式
     */
    private static Pattern getOrCreatePattern(String pattern) {
        return PATTERN_CACHE.computeIfAbsent(pattern, p -> {
            // 将通配符转换为正则表达式
            String regex = p
                    .replace(".", "\\.")
                    .replace("*", ".*")
                    .replace("?", ".");
            return Pattern.compile(regex);
        });
    }
    
    /**
     * 验证配置条目格式是否正确
     * 
     * @param entry 配置条目
     * @return 如果格式正确返回 true
     */
    public static boolean isValidEntry(String entry) {
        if (entry == null || entry.isEmpty()) {
            return false;
        }
        
        // 去除标签前缀后检查
        String checkEntry = entry.startsWith(TAG_PREFIX) ? entry.substring(1) : entry;
        
        // 必须包含命名空间分隔符
        if (!checkEntry.contains(":")) {
            return false;
        }
        
        // 尝试解析为 Identifier
        return Identifier.tryParse(checkEntry.replace("*", "a")) != null;
    }
    
    /**
     * 规范化配置条目
     * 
     * <p>如果条目不包含命名空间，添加 minecraft: 前缀</p>
     * 
     * @param entry 原始条目
     * @return 规范化后的条目
     */
    public static String normalizeEntry(String entry) {
        if (entry == null || entry.isEmpty()) {
            return entry;
        }
        
        String work = entry.startsWith(TAG_PREFIX) ? entry.substring(1) : entry;
        
        if (!work.contains(":")) {
            work = "minecraft:" + work;
        }
        
        return entry.startsWith(TAG_PREFIX) ? TAG_PREFIX + work : work;
    }
    
    /**
     * 获取所有匹配指定标签的方块
     * 
     * @param tagEntry 标签条目（必须以 # 开头）
    * @return 匹配的方块 Identifier 集合
     */
    public static Set<Identifier> getBlocksInTag(String tagEntry) {
        return BLOCK_CACHE.computeIfAbsent(tagEntry, entry -> {
            Set<Identifier> result = new HashSet<>();
            
            TagKey<Block> tag = parseBlockTag(entry);
            if (tag != null) {
                for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(tag)) {
                    result.add(holder.unwrapKey().orElseThrow().identifier());
                }
            }
            
            return Collections.unmodifiableSet(result);
        });
    }
    
    /**
     * 获取所有匹配指定标签的物品
     * 
     * @param tagEntry 标签条目（必须以 # 开头）
    * @return 匹配的物品 Identifier 集合
     */
    public static Set<Identifier> getItemsInTag(String tagEntry) {
        return ITEM_CACHE.computeIfAbsent(tagEntry, entry -> {
            Set<Identifier> result = new HashSet<>();
            
            TagKey<Item> tag = parseItemTag(entry);
            if (tag != null) {
                for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                    result.add(holder.unwrapKey().orElseThrow().identifier());
                }
            }
            
            return Collections.unmodifiableSet(result);
        });
    }
}
