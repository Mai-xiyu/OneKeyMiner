package org.xiyu.onekeyminer.api;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.chain.ChainActionType;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.ConfigSyncHelper;
import org.xiyu.onekeyminer.config.MinerConfig;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;
import org.xiyu.onekeyminer.network.ClientPreferenceSession;
import org.xiyu.onekeyminer.registry.TagResolver;
import org.xiyu.onekeyminer.shape.ChainShape;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * OneKeyMiner 公共 API
 * 
 * <p>提供给其他模组开发者使用的 API 接口，用于：</p>
 * <ul>
 *   <li>注册/注销方块白名单和黑名单</li>
 *   <li>注册/注销工具白名单和黑名单</li>
 *   <li>通过 {@link org.xiyu.onekeyminer.api.event.ChainEvents} 监听链式操作事件</li>
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
 * // 监听链式操作事件
 * ChainEvents.registerPostActionListener(event -> {
 *     if (event.getActionType() == ChainActionType.MINING) {
 *         int count = event.getTotalCount();
 *         // 处理挖矿统计...
 *     }
 * });
 * }</pre>
 * 
 * @author OneKeyMiner Team
 * @version 1.6.8
 * @since Minecraft 1.20.1
 * @see org.xiyu.onekeyminer.api.event.ChainEvents
 */
public final class OneKeyMinerAPI {
    
    /** 运行时方块白名单 */
    private static final Set<ResourceLocation> BLOCK_WHITELIST = ConcurrentHashMap.newKeySet();
    
    /** 运行时方块黑名单 */
    private static final Set<ResourceLocation> BLOCK_BLACKLIST = ConcurrentHashMap.newKeySet();
    
    /** 运行时方块标签白名单 */
    private static final Set<TagKey<Block>> BLOCK_TAG_WHITELIST = ConcurrentHashMap.newKeySet();
    
    /** 运行时方块标签黑名单 */
    private static final Set<TagKey<Block>> BLOCK_TAG_BLACKLIST = ConcurrentHashMap.newKeySet();
    
    /** 运行时工具白名单 */
    private static final Set<ResourceLocation> TOOL_WHITELIST = ConcurrentHashMap.newKeySet();
    
    /** 运行时工具黑名单 */
    private static final Set<ResourceLocation> TOOL_BLACKLIST = ConcurrentHashMap.newKeySet();
    
    /** 运行时工具标签白名单 */
    private static final Set<TagKey<Item>> TOOL_TAG_WHITELIST = ConcurrentHashMap.newKeySet();
    
    /** 运行时工具标签黑名单 */
    private static final Set<TagKey<Item>> TOOL_TAG_BLACKLIST = ConcurrentHashMap.newKeySet();
    
    /** 方块分组映射（用于宽松匹配） */
    private static final Map<ResourceLocation, String> BLOCK_GROUPS = new ConcurrentHashMap<>();

    /** Config-derived selectors are replaced atomically on reload. */
    private record ConfigRegistrations(
            Set<ResourceLocation> blockWhitelist,
            Set<ResourceLocation> blockBlacklist,
            Set<TagKey<Block>> blockTagWhitelist,
            Set<TagKey<Block>> blockTagBlacklist,
            Set<ResourceLocation> toolWhitelist,
            Set<ResourceLocation> toolBlacklist,
            Set<TagKey<Item>> toolTagWhitelist,
            Set<TagKey<Item>> toolTagBlacklist,
            Set<ResourceLocation> interactionToolWhitelist,
            Set<ResourceLocation> interactionToolBlacklist,
            Set<TagKey<Item>> interactionToolTagWhitelist,
            Set<TagKey<Item>> interactionToolTagBlacklist,
            Set<ResourceLocation> interactiveItemWhitelist,
            Set<ResourceLocation> interactiveItemBlacklist,
            Set<ResourceLocation> plantableWhitelist,
            Set<ResourceLocation> plantableBlacklist,
            Set<TagKey<Item>> plantableTagWhitelist,
            Set<TagKey<Item>> plantableTagBlacklist
    ) {
        private static ConfigRegistrations empty() {
            return new ConfigRegistrations(
                    Set.of(), Set.of(), Set.of(), Set.of(),
                    Set.of(), Set.of(), Set.of(), Set.of(),
                    Set.of(), Set.of(), Set.of(), Set.of(),
                    Set.of(), Set.of(), Set.of(), Set.of(),
                    Set.of(), Set.of()
            );
        }
    }

    private static volatile ConfigRegistrations CONFIG_REGISTRATIONS =
            ConfigRegistrations.empty();
    
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
    public static synchronized void loadFromConfig() {
        MinerConfig config = ConfigManager.getConfig();

        Set<ResourceLocation> blockWhitelist = new HashSet<>();
        Set<ResourceLocation> blockBlacklist = new HashSet<>();
        Set<TagKey<Block>> blockTagWhitelist = new HashSet<>();
        Set<TagKey<Block>> blockTagBlacklist = new HashSet<>();
        Set<ResourceLocation> toolWhitelist = new HashSet<>();
        Set<ResourceLocation> toolBlacklist = new HashSet<>();
        Set<TagKey<Item>> toolTagWhitelist = new HashSet<>();
        Set<TagKey<Item>> toolTagBlacklist = new HashSet<>();
        Set<ResourceLocation> interactionToolWhitelist = new HashSet<>();
        Set<ResourceLocation> interactionToolBlacklist = new HashSet<>();
        Set<TagKey<Item>> interactionToolTagWhitelist = new HashSet<>();
        Set<TagKey<Item>> interactionToolTagBlacklist = new HashSet<>();
        Set<ResourceLocation> interactiveItemWhitelist = new HashSet<>();
        Set<ResourceLocation> interactiveItemBlacklist = new HashSet<>();
        Set<ResourceLocation> plantableWhitelist = new HashSet<>();
        Set<ResourceLocation> plantableBlacklist = new HashSet<>();
        Set<TagKey<Item>> plantableTagWhitelist = new HashSet<>();
        Set<TagKey<Item>> plantableTagBlacklist = new HashSet<>();

        addBlockSelectors(config.customWhitelist, blockWhitelist, blockTagWhitelist);
        addBlockSelectors(config.blacklist, blockBlacklist, blockTagBlacklist);
        addItemSelectors(config.toolWhitelist, toolWhitelist, toolTagWhitelist);
        addItemSelectors(config.toolBlacklist, toolBlacklist, toolTagBlacklist);
        addItemSelectors(
                config.interactionToolWhitelist,
                interactionToolWhitelist,
                interactionToolTagWhitelist
        );
        addItemSelectors(
                config.interactionToolBlacklist,
                interactionToolBlacklist,
                interactionToolTagBlacklist
        );
        addItemSelectors(config.seedWhitelist, plantableWhitelist, plantableTagWhitelist);
        addItemSelectors(config.seedBlacklist, plantableBlacklist, plantableTagBlacklist);
        addIds(config.interactiveItemWhitelist, interactiveItemWhitelist);
        addIds(config.interactiveItemBlacklist, interactiveItemBlacklist);

        CONFIG_REGISTRATIONS = new ConfigRegistrations(
                Set.copyOf(blockWhitelist),
                Set.copyOf(blockBlacklist),
                Set.copyOf(blockTagWhitelist),
                Set.copyOf(blockTagBlacklist),
                Set.copyOf(toolWhitelist),
                Set.copyOf(toolBlacklist),
                Set.copyOf(toolTagWhitelist),
                Set.copyOf(toolTagBlacklist),
                Set.copyOf(interactionToolWhitelist),
                Set.copyOf(interactionToolBlacklist),
                Set.copyOf(interactionToolTagWhitelist),
                Set.copyOf(interactionToolTagBlacklist),
                Set.copyOf(interactiveItemWhitelist),
                Set.copyOf(interactiveItemBlacklist),
                Set.copyOf(plantableWhitelist),
                Set.copyOf(plantableBlacklist),
                Set.copyOf(plantableTagWhitelist),
                Set.copyOf(plantableTagBlacklist)
        );
    }
    
    // ==================== 方块白名单 API ====================
    
    /**
     * 注册方块到白名单
     * 
     * @param blockId 方块 ID，格式为 "namespace:path"（如 "minecraft:diamond_ore"）
     * @return 如果注册成功返回 true，如果已存在返回 false
     */
    public static boolean registerBlock(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return false;
        }
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
        if (block == null) {
            return false;
        }
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
        if (tagId == null || tagId.isBlank()) {
            return false;
        }
        String normalized = tagId.trim();
        ResourceLocation loc = ResourceLocation.tryParse(
                normalized.startsWith("#") ? normalized.substring(1) : normalized
        );
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
        if (blockId == null || blockId.isBlank()) return false;
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
        if (tagId == null || tagId.isBlank()) return false;
        String normalized = tagId.trim();
        ResourceLocation loc = ResourceLocation.tryParse(
                normalized.startsWith("#") ? normalized.substring(1) : normalized
        );
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
        if (blockId == null || blockId.isBlank()) {
            return false;
        }
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
        if (block == null) {
            return false;
        }
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
        if (blockId == null || blockId.isBlank()) return false;
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
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        itemId = itemId.trim();
        if (itemId.startsWith("#")) {
            String tagId = itemId.substring(1);
            ResourceLocation loc = ResourceLocation.tryParse(tagId);
            if (loc == null) {
                OneKeyMiner.LOGGER.warn("无效的物品标签: {}", itemId);
                return false;
            }
            return TOOL_TAG_WHITELIST.add(TagKey.create(Registries.ITEM, loc));
        }
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
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        itemId = itemId.trim();
        if (itemId.startsWith("#")) {
            String tagId = itemId.substring(1);
            ResourceLocation loc = ResourceLocation.tryParse(tagId);
            if (loc == null) {
                OneKeyMiner.LOGGER.warn("无效的物品标签: {}", itemId);
                return false;
            }
            return TOOL_TAG_BLACKLIST.add(TagKey.create(Registries.ITEM, loc));
        }
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
        if (blockId == null || blockId.isBlank()
                || groupId == null || groupId.isBlank()) {
            return;
        }
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
        if (block1 == null || block2 == null) {
            return false;
        }
        ResourceLocation loc1 = BuiltInRegistries.BLOCK.getKey(block1);
        ResourceLocation loc2 = BuiltInRegistries.BLOCK.getKey(block2);
        
        String group1 = BLOCK_GROUPS.get(loc1);
        String group2 = BLOCK_GROUPS.get(loc2);
        
        if (group1 != null && group1.equals(group2)) {
            return true;
        }
        
        return sharesAnyBlockTag(block1, block2, BLOCK_TAG_WHITELIST)
                || sharesAnyBlockTag(
                block1,
                block2,
                CONFIG_REGISTRATIONS.blockTagWhitelist()
        );
    }
    
    /**
     * 检查两个方块是否共享同一个标签
     * 
     * @param block1 方块 1
     * @param block2 方块 2
     * @return 如果共享标签返回 true
     */
    public static boolean blocksShareTag(Block block1, Block block2) {
        if (block1 == null || block2 == null) {
            return false;
        }
        return sharesAnyBlockTag(block1, block2, BLOCK_TAG_WHITELIST)
                || sharesAnyBlockTag(
                block1,
                block2,
                CONFIG_REGISTRATIONS.blockTagWhitelist()
        );
    }
    
    // ==================== 交互工具 API ====================
    
    /** 交互工具白名单 */
    private static final Set<ResourceLocation> INTERACTION_TOOL_WHITELIST = ConcurrentHashMap.newKeySet();
    
    /** 交互工具黑名单 */
    private static final Set<ResourceLocation> INTERACTION_TOOL_BLACKLIST = ConcurrentHashMap.newKeySet();
    
    /** 交互工具标签白名单 */
    private static final Set<TagKey<Item>> INTERACTION_TOOL_TAG_WHITELIST = ConcurrentHashMap.newKeySet();
    
    /** 交互工具标签黑名单 */
    private static final Set<TagKey<Item>> INTERACTION_TOOL_TAG_BLACKLIST = ConcurrentHashMap.newKeySet();
    
    /** 通用交互物品白名单（骨粉、刷子等消耗型交互物品） */
    private static final Set<ResourceLocation> INTERACTIVE_ITEM_WHITELIST = ConcurrentHashMap.newKeySet();
    
    /** 通用交互物品黑名单 */
    private static final Set<ResourceLocation> INTERACTIVE_ITEM_BLACKLIST = ConcurrentHashMap.newKeySet();
    
    /** 自定义交互验证器 */
    private static final List<java.util.function.BiPredicate<ItemStack, BlockState>> INTERACTION_VALIDATORS =
            new CopyOnWriteArrayList<>();
    
    /**
     * 注册交互工具到白名单
     * 
     * @param itemId 物品 ID（支持标签格式 "#c:shears"）
     * @return 如果注册成功返回 true
     */
    public static boolean registerInteractionTool(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        itemId = itemId.trim();
        if (itemId.startsWith("#")) {
            String tagId = itemId.substring(1);
            ResourceLocation loc = ResourceLocation.tryParse(tagId);
            if (loc == null) {
                OneKeyMiner.LOGGER.warn("无效的交互工具标签: {}", itemId);
                return false;
            }
            return INTERACTION_TOOL_TAG_WHITELIST.add(TagKey.create(Registries.ITEM, loc));
        }
        ResourceLocation loc = ResourceLocation.tryParse(itemId);
        if (loc == null) {
            OneKeyMiner.LOGGER.warn("无效的交互工具 ID: {}", itemId);
            return false;
        }
        return INTERACTION_TOOL_WHITELIST.add(loc);
    }

    /**
     * 交互工具白名单（别名）
     */
    public static boolean whitelistInteractionTool(String itemId) {
        return registerInteractionTool(itemId);
    }

    /**
     * 将交互工具加入黑名单
     *
     * @param itemId 物品 ID（支持标签格式 "#c:shears"）
     * @return 如果添加成功返回 true
     */
    public static boolean blacklistInteractionTool(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        itemId = itemId.trim();
        if (itemId.startsWith("#")) {
            String tagId = itemId.substring(1);
            ResourceLocation loc = ResourceLocation.tryParse(tagId);
            if (loc == null) {
                OneKeyMiner.LOGGER.warn("无效的交互工具标签: {}", itemId);
                return false;
            }
            return INTERACTION_TOOL_TAG_BLACKLIST.add(TagKey.create(Registries.ITEM, loc));
        }
        ResourceLocation loc = ResourceLocation.tryParse(itemId);
        if (loc == null) {
            OneKeyMiner.LOGGER.warn("无效的交互工具 ID: {}", itemId);
            return false;
        }
        return INTERACTION_TOOL_BLACKLIST.add(loc);
    }

    // ==================== 自定义交互物品 API ====================
    
    /**
     * 注册交互物品到白名单
     * 
     * @param itemId 物品 ID（如 "minecraft:bone_meal"）
     * @return 如果注册成功返回 true
     */
    public static boolean registerInteractiveItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        ResourceLocation loc = ResourceLocation.tryParse(itemId);
        if (loc == null) {
            OneKeyMiner.LOGGER.warn("无效的交互物品 ID: {}", itemId);
            return false;
        }
        return INTERACTIVE_ITEM_WHITELIST.add(loc);
    }
    
    /**
     * 从交互物品白名单移除
     * 
     * @param itemId 物品 ID
     * @return 如果移除成功返回 true
     */
    public static boolean unregisterInteractiveItem(String itemId) {
        if (itemId == null || itemId.isBlank()) return false;
        ResourceLocation loc = ResourceLocation.tryParse(itemId);
        if (loc == null) return false;
        return INTERACTIVE_ITEM_WHITELIST.remove(loc);
    }
    
    /**
     * 将交互物品加入黑名单
     * 
     * @param itemId 物品 ID
     * @return 如果添加成功返回 true
     */
    public static boolean blacklistInteractiveItem(String itemId) {
        if (itemId == null || itemId.isBlank()) return false;
        ResourceLocation loc = ResourceLocation.tryParse(itemId);
        if (loc == null) return false;
        return INTERACTIVE_ITEM_BLACKLIST.add(loc);
    }
    
    /**
     * 检查物品是否为允许的交互物品（骨粉、刷子等消耗型）
     * 
     * @param stack 物品栈
     * @return 如果允许返回 true
     */
    public static boolean isInteractiveItemAllowed(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ConfigRegistrations configured = CONFIG_REGISTRATIONS;
        ResourceLocation loc = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (INTERACTIVE_ITEM_BLACKLIST.contains(loc)
                || configured.interactiveItemBlacklist().contains(loc)) {
            return false;
        }
        return INTERACTIVE_ITEM_WHITELIST.contains(loc)
                || configured.interactiveItemWhitelist().contains(loc);
    }
    
    /**
     * 注册自定义交互验证器
     * <p>允许 addon 模组注册自定义的物品-方块交互验证逻辑</p>
     * 
     * @param validator 验证函数，接受 (ItemStack, BlockState) 返回是否可交互
     */
    public static void registerInteractionValidator(java.util.function.BiPredicate<ItemStack, BlockState> validator) {
        INTERACTION_VALIDATORS.add(Objects.requireNonNull(validator, "validator"));
    }

    public static boolean unregisterInteractionValidator(
            java.util.function.BiPredicate<ItemStack, BlockState> validator
    ) {
        return validator != null && INTERACTION_VALIDATORS.remove(validator);
    }
    
    /**
     * 使用自定义验证器检查物品是否能与方块交互
     * 
     * @param stack 物品栈
     * @param state 方块状态
     * @return 所有验证器均允许时返回 true；异常或任一拒绝时返回 false
     */
    public static boolean checkCustomInteractionValidators(ItemStack stack, BlockState state) {
        if (stack == null || state == null) {
            return false;
        }
        for (var validator : INTERACTION_VALIDATORS) {
            try {
                if (!validator.test(stack, state)) {
                    return false;
                }
            } catch (RuntimeException exception) {
                OneKeyMiner.LOGGER.error("Custom interaction validator failed", exception);
                return false;
            }
        }
        return true;
    }

    public static boolean validateInteraction(ItemStack stack, BlockState state) {
        return checkCustomInteractionValidators(stack, state);
    }

    // ==================== 自定义工具动作规则 ====================

    /** 自定义工具动作规则 */
    private static final List<ToolActionRule> TOOL_ACTION_RULES = new CopyOnWriteArrayList<>();

    /**
     * 目标类型（方块或实体）
     */
    public enum ToolTargetType {
        BLOCK,
        ENTITY
    }

    /**
     * 交互动作类型（用于 INTERACTION）
     */
    public enum InteractionRule {
        SHEARING,
        TILLING,
        STRIPPING,
        PATH_MAKING,
        BRUSHING,
        ITEM_USE,
        GENERIC
    }

    /**
     * 自定义工具动作规则
     */
    public record ToolActionRule(
            ToolSelector toolSelector,
            ToolTargetType targetType,
            ChainActionType actionType,
            InteractionRule interactionRule,
            List<String> targets
    ) {
        public ToolActionRule {
            toolSelector = Objects.requireNonNull(toolSelector, "toolSelector");
            targetType = Objects.requireNonNull(targetType, "targetType");
            actionType = Objects.requireNonNull(actionType, "actionType");
            if (!isSupportedToolActionCombination(
                    targetType,
                    actionType,
                    interactionRule
            )) {
                throw new IllegalArgumentException("Unsupported tool action combination");
            }
            targets = targets == null ? List.of() : List.copyOf(targets);
        }
    }

    public record ToolSelector(ResourceLocation itemId, TagKey<Item> itemTag) {
        public ToolSelector {
            if ((itemId == null) == (itemTag == null)) {
                throw new IllegalArgumentException("Exactly one tool selector must be set");
            }
        }

        public boolean matches(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return false;
            }
            if (itemTag != null) {
                return stack.is(itemTag);
            }
            if (itemId != null) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                return itemId.equals(id);
            }
            return false;
        }
    }

    /**
     * 注册自定义工具动作规则
     *
     * @param toolSelector 工具 ID 或标签（如 "mymod:tool" 或 "#c:hoes"）
     * @param targetType 目标类型（方块/实体）
     * @param actionType 触发的连锁类型（MINING/INTERACTION/PLANTING）
     * @param interactionRule 交互类型（仅 INTERACTION 有效）
     * @param targets 目标列表（方块/实体 ID 或标签；为空表示任意目标）
     * @return 注册成功返回 true
     */
    public static boolean registerToolAction(
            String toolSelector,
            ToolTargetType targetType,
            ChainActionType actionType,
            InteractionRule interactionRule,
            List<String> targets
    ) {
        if (toolSelector == null || toolSelector.isBlank()) {
            OneKeyMiner.LOGGER.warn("无效的工具选择器: {}", toolSelector);
            return false;
        }

        ToolSelector selector = parseToolSelector(toolSelector);
        if (selector == null) {
            OneKeyMiner.LOGGER.warn("无效的工具选择器: {}", toolSelector);
            return false;
        }

        if (!isSupportedToolActionCombination(targetType, actionType, interactionRule)) {
            OneKeyMiner.LOGGER.warn(
                    "Incomplete or incompatible tool action rule: targetType={}, actionType={}, interactionRule={}",
                    targetType,
                    actionType,
                    interactionRule
            );
            return false;
        }
        List<String> normalizedTargets = targets == null
                ? List.of()
                : targets.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(target -> !target.isEmpty())
                        .distinct()
                        .toList();
        if (normalizedTargets.stream().anyMatch(target -> !isValidTargetSelector(target))) {
            OneKeyMiner.LOGGER.warn("Invalid target selector in tool action rule: {}", normalizedTargets);
            return false;
        }
        TOOL_ACTION_RULES.add(new ToolActionRule(selector, targetType, actionType, interactionRule, normalizedTargets));
        return true;
    }

    private static boolean isSupportedToolActionCombination(
            ToolTargetType targetType,
            ChainActionType actionType,
            InteractionRule interactionRule
    ) {
        if (targetType == null
                || actionType == null
                || actionType == ChainActionType.HARVESTING
                || actionType == ChainActionType.INTERACTION && interactionRule == null
                || actionType != ChainActionType.INTERACTION && interactionRule != null) {
            return false;
        }
        if (targetType == ToolTargetType.ENTITY) {
            return actionType == ChainActionType.INTERACTION
                    && interactionRule == InteractionRule.SHEARING;
        }
        return interactionRule != InteractionRule.SHEARING;
    }

    public static List<ToolActionRule> getToolActionRules() {
        return List.copyOf(TOOL_ACTION_RULES);
    }

    public static boolean unregisterToolAction(ToolActionRule rule) {
        return rule != null && TOOL_ACTION_RULES.remove(rule);
    }

    /**
     * 注册交互工具规则（方块/实体）
     */
    public static boolean registerInteractionToolRule(
            String toolSelector,
            ToolTargetType targetType,
            InteractionRule interactionRule,
            String... targets
    ) {
        return registerToolAction(toolSelector, targetType, ChainActionType.INTERACTION, interactionRule,
                targets == null ? List.of() : Arrays.asList(targets));
    }

    /**
     * 注册挖掘工具规则（方块）
     */
    public static boolean registerMiningToolRule(String toolSelector, String... targets) {
        return registerToolAction(toolSelector, ToolTargetType.BLOCK, ChainActionType.MINING, null,
                targets == null ? List.of() : Arrays.asList(targets));
    }

    /**
     * 注册实体剪羊毛规则
     */
    public static boolean registerEntityShearingRule(String toolSelector, String... targets) {
        return registerToolAction(toolSelector, ToolTargetType.ENTITY, ChainActionType.INTERACTION, InteractionRule.SHEARING,
                targets == null ? List.of() : Arrays.asList(targets));
    }

    /**
     * 查询工具在指定方块上的自定义规则
     */
    public static Optional<ToolActionRule> findToolActionForBlock(ItemStack stack, BlockState state) {
        if (stack == null || state == null) {
            return Optional.empty();
        }
        for (ToolActionRule rule : TOOL_ACTION_RULES) {
            if (rule.targetType != ToolTargetType.BLOCK) {
                continue;
            }
            if (!rule.toolSelector.matches(stack)) {
                continue;
            }
            if (matchesBlockTargets(state, rule.targets)) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    /**
     * 查询工具在指定实体上的自定义规则
     */
    public static Optional<ToolActionRule> findToolActionForEntity(ItemStack stack, Entity entity) {
        if (stack == null || entity == null) {
            return Optional.empty();
        }
        for (ToolActionRule rule : TOOL_ACTION_RULES) {
            if (rule.targetType != ToolTargetType.ENTITY) {
                continue;
            }
            if (!rule.toolSelector.matches(stack)) {
                continue;
            }
            if (matchesEntityTargets(entity, rule.targets)) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    /**
     * 检查工具是否存在任意自定义动作规则
     */
    public static boolean hasToolActionRule(ItemStack stack, ChainActionType actionType) {
        if (stack == null || stack.isEmpty() || actionType == null) {
            return false;
        }
        for (ToolActionRule rule : TOOL_ACTION_RULES) {
            if (rule.actionType != actionType) {
                continue;
            }
            if (rule.toolSelector.matches(stack)) {
                return true;
            }
        }
        return false;
    }

    private static ToolSelector parseToolSelector(String selector) {
        selector = selector.trim();
        if (selector.startsWith("#")) {
            ResourceLocation loc = ResourceLocation.tryParse(selector.substring(1));
            if (loc == null) {
                return null;
            }
            TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), loc);
            return new ToolSelector(null, tag);
        }
        ResourceLocation loc = ResourceLocation.tryParse(selector);
        if (loc == null) {
            return null;
        }
        return new ToolSelector(loc, null);
    }

    private static boolean matchesBlockTargets(BlockState state, List<String> targets) {
        if (targets == null || targets.isEmpty()) {
            return true;
        }
        Block block = state.getBlock();
        for (String entry : targets) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            if ("*".equals(entry)) {
                return true;
            }
            if (TagResolver.matchesBlock(block, entry)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesEntityTargets(Entity entity, List<String> targets) {
        if (targets == null || targets.isEmpty()) {
            return true;
        }
        EntityType<?> type = entity.getType();
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        for (String entry : targets) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            if ("*".equals(entry)) {
                return true;
            }
            if (entry.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(entry.substring(1));
                if (tagId != null) {
                    TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, tagId);
                    if (type.is(tag)) {
                        return true;
                    }
                }
                continue;
            }
            if (typeId != null && typeId.toString().equals(entry)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查物品是否为允许的交互工具
     * 
     * @param stack 物品栈
     * @return 如果允许返回 true
     */
    public static boolean isInteractionToolAllowed(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ConfigRegistrations configured = CONFIG_REGISTRATIONS;
        ResourceLocation loc = BuiltInRegistries.ITEM.getKey(stack.getItem());
        
        // 检查黑名单
        if (INTERACTION_TOOL_BLACKLIST.contains(loc)
                || configured.interactionToolBlacklist().contains(loc)) {
            return false;
        }
        if (matchesAnyItemTag(stack, INTERACTION_TOOL_TAG_BLACKLIST)
                || matchesAnyItemTag(stack, configured.interactionToolTagBlacklist())) {
            return false;
        }
        
        // 检查白名单（ID + 标签）
        return INTERACTION_TOOL_WHITELIST.contains(loc)
                || configured.interactionToolWhitelist().contains(loc)
                || matchesAnyItemTag(stack, INTERACTION_TOOL_TAG_WHITELIST)
                || matchesAnyItemTag(stack, configured.interactionToolTagWhitelist());
    }
    
    // ==================== 种植物品 API ====================
    
    /** 种植物品白名单 */
    private static final Set<ResourceLocation> PLANTABLE_WHITELIST = ConcurrentHashMap.newKeySet();
    
    /** 种植物品黑名单 */
    private static final Set<ResourceLocation> PLANTABLE_BLACKLIST = ConcurrentHashMap.newKeySet();
    
    /** 种植物品标签白名单 */
    private static final Set<TagKey<Item>> PLANTABLE_TAG_WHITELIST = ConcurrentHashMap.newKeySet();
    
    /** 种植物品标签黑名单 */
    private static final Set<TagKey<Item>> PLANTABLE_TAG_BLACKLIST = ConcurrentHashMap.newKeySet();
    
    /**
     * 注册可种植物品
     * 
     * @param itemId 物品 ID（支持标签格式）
     * @return 如果注册成功返回 true
     */
    public static boolean registerPlantableItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        itemId = itemId.trim();
        if (itemId.startsWith("#")) {
            String tagId = itemId.substring(1);
            ResourceLocation loc = ResourceLocation.tryParse(tagId);
            if (loc == null) {
                OneKeyMiner.LOGGER.warn("无效的种植物品标签: {}", itemId);
                return false;
            }
            return PLANTABLE_TAG_WHITELIST.add(TagKey.create(Registries.ITEM, loc));
        }
        ResourceLocation loc = ResourceLocation.tryParse(itemId);
        if (loc == null) {
            OneKeyMiner.LOGGER.warn("无效的种植物品 ID: {}", itemId);
            return false;
        }
        return PLANTABLE_WHITELIST.add(loc);
    }

    /**
     * 可种植物品白名单（别名）
     */
    public static boolean whitelistPlantable(String itemId) {
        return registerPlantableItem(itemId);
    }
    
    /**
     * 将种子添加到黑名单
     * 
     * @param itemId 物品 ID
     * @return 如果添加成功返回 true
     */
    public static boolean blacklistSeed(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        itemId = itemId.trim();
        if (itemId.startsWith("#")) {
            String tagId = itemId.substring(1);
            ResourceLocation loc = ResourceLocation.tryParse(tagId);
            if (loc == null) {
                OneKeyMiner.LOGGER.warn("无效的种子标签: {}", itemId);
                return false;
            }
            return PLANTABLE_TAG_BLACKLIST.add(TagKey.create(Registries.ITEM, loc));
        }
        ResourceLocation loc = ResourceLocation.tryParse(itemId);
        if (loc == null) {
            OneKeyMiner.LOGGER.warn("无效的种子 ID: {}", itemId);
            return false;
        }
        return PLANTABLE_BLACKLIST.add(loc);
    }

    /**
     * 可种植物品黑名单（别名）
     */
    public static boolean blacklistPlantable(String itemId) {
        return blacklistSeed(itemId);
    }
    
    /**
     * 检查种子是否在黑名单中
     * 
     * @param item 物品
     * @return 如果在黑名单中返回 true
     */
    public static boolean isSeedBlacklisted(Item item) {
        if (item == null) {
            return false;
        }
        ConfigRegistrations configured = CONFIG_REGISTRATIONS;
        ResourceLocation loc = BuiltInRegistries.ITEM.getKey(item);
        if (PLANTABLE_BLACKLIST.contains(loc)
                || configured.plantableBlacklist().contains(loc)) {
            return true;
        }
        return matchesAnyItemTag(new ItemStack(item), PLANTABLE_TAG_BLACKLIST)
                || matchesAnyItemTag(
                new ItemStack(item),
                configured.plantableTagBlacklist()
        );
    }
    
    /**
     * 检查物品是否为允许的可种植物品
     * 
     * @param stack 物品栈
     * @return 如果允许返回 true
     */
    public static boolean isPlantableItemAllowed(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ConfigRegistrations configured = CONFIG_REGISTRATIONS;
        ResourceLocation loc = BuiltInRegistries.ITEM.getKey(stack.getItem());
        
        // 检查黑名单
        if (PLANTABLE_BLACKLIST.contains(loc)
                || configured.plantableBlacklist().contains(loc)) {
            return false;
        }
        if (matchesAnyItemTag(stack, PLANTABLE_TAG_BLACKLIST)
                || matchesAnyItemTag(stack, configured.plantableTagBlacklist())) {
            return false;
        }
        
        // 检查白名单（ID + 标签）
        return PLANTABLE_WHITELIST.contains(loc)
                || configured.plantableWhitelist().contains(loc)
                || matchesAnyItemTag(stack, PLANTABLE_TAG_WHITELIST)
                || matchesAnyItemTag(stack, configured.plantableTagWhitelist());
    }
    
    // ==================== 查询 API ====================
    
    /**
     * 检查方块是否允许连锁挖矿
     * 
     * @param block 方块实例
     * @return 如果允许返回 true
     */
    public static boolean isBlockAllowed(Block block) {
        if (block == null) {
            return false;
        }
        ResourceLocation loc = BuiltInRegistries.BLOCK.getKey(block);
        MinerConfig config = ConfigManager.getConfig();
        ConfigRegistrations configured = CONFIG_REGISTRATIONS;
        
        // 检查黑名单（优先）
        if (BLOCK_BLACKLIST.contains(loc)
                || configured.blockBlacklist().contains(loc)) {
            return false;
        }
        
        // 检查标签黑名单
        if (matchesAnyBlockTag(block, BLOCK_TAG_BLACKLIST)
                || matchesAnyBlockTag(block, configured.blockTagBlacklist())) {
            return false;
        }
        
        // 如果开启了"挖掘所有方块"模式，不在黑名单中的方块都允许
        if (config.mineAllBlocks) {
            return true;
        }
        
        // 否则检查白名单
        if (BLOCK_WHITELIST.contains(loc)
                || configured.blockWhitelist().contains(loc)) {
            return true;
        }
        
        // 检查标签白名单
        return matchesAnyBlockTag(block, BLOCK_TAG_WHITELIST)
                || matchesAnyBlockTag(block, configured.blockTagWhitelist());
    }
    
    /**
     * 检查方块是否在黑名单中
     * 
     * @param block 方块实例
     * @return 如果在黑名单中返回 true
     */
    public static boolean isBlockBlacklisted(Block block) {
        if (block == null) {
            return false;
        }
        ConfigRegistrations configured = CONFIG_REGISTRATIONS;
        ResourceLocation loc = BuiltInRegistries.BLOCK.getKey(block);
        if (BLOCK_BLACKLIST.contains(loc)
                || configured.blockBlacklist().contains(loc)) {
            return true;
        }
        return matchesAnyBlockTag(block, BLOCK_TAG_BLACKLIST)
                || matchesAnyBlockTag(block, configured.blockTagBlacklist());
    }
    
    /**
     * 检查工具是否允许触发连锁挖矿
     * 
     * @param tool 工具物品栈
     * @return 如果允许返回 true
     */
    public static boolean isToolAllowed(ItemStack tool) {
        MinerConfig config = ConfigManager.getConfig();
        
        if (tool == null || tool.isEmpty()) {
            // 空手是否允许？根据配置决定
            return config.allowBareHand;
        }
        ConfigRegistrations configured = CONFIG_REGISTRATIONS;
        ResourceLocation loc = BuiltInRegistries.ITEM.getKey(tool.getItem());
        
        // 检查黑名单（优先）— ID 和标签
        if (TOOL_BLACKLIST.contains(loc)
                || configured.toolBlacklist().contains(loc)) {
            return false;
        }
        if (matchesAnyItemTag(tool, TOOL_TAG_BLACKLIST)
                || matchesAnyItemTag(tool, configured.toolTagBlacklist())) {
            return false;
        }
        
        // 如果白名单为空（ID 和标签都为空），允许所有工具
        if (TOOL_WHITELIST.isEmpty()
                && TOOL_TAG_WHITELIST.isEmpty()
                && configured.toolWhitelist().isEmpty()
                && configured.toolTagWhitelist().isEmpty()) {
            return true;
        }
        
        // 检查白名单 — ID 和标签
        return TOOL_WHITELIST.contains(loc)
                || configured.toolWhitelist().contains(loc)
                || matchesAnyItemTag(tool, TOOL_TAG_WHITELIST)
                || matchesAnyItemTag(tool, configured.toolTagWhitelist());
    }

    private static void addBlockSelectors(
            Collection<String> selectors,
            Set<ResourceLocation> ids,
            Set<TagKey<Block>> tags
    ) {
        if (selectors == null) {
            return;
        }
        for (String selector : selectors) {
            if (selector == null || selector.isBlank()) {
                continue;
            }
            String normalized = selector.trim();
            boolean tag = normalized.startsWith("#");
            String value = tag ? normalized.substring(1) : normalized;
            ResourceLocation id = ResourceLocation.tryParse(value);
            if (id == null) {
                OneKeyMiner.LOGGER.warn("Ignoring invalid block selector: {}", selector);
            } else if (tag) {
                tags.add(TagKey.create(Registries.BLOCK, id));
            } else {
                ids.add(id);
            }
        }
    }

    private static void addItemSelectors(
            Collection<String> selectors,
            Set<ResourceLocation> ids,
            Set<TagKey<Item>> tags
    ) {
        if (selectors == null) {
            return;
        }
        for (String selector : selectors) {
            if (selector == null || selector.isBlank()) {
                continue;
            }
            String normalized = selector.trim();
            boolean tag = normalized.startsWith("#");
            String value = tag ? normalized.substring(1) : normalized;
            ResourceLocation id = ResourceLocation.tryParse(value);
            if (id == null) {
                OneKeyMiner.LOGGER.warn("Ignoring invalid item selector: {}", selector);
            } else if (tag) {
                tags.add(TagKey.create(Registries.ITEM, id));
            } else {
                ids.add(id);
            }
        }
    }

    private static void addIds(
            Collection<String> selectors,
            Set<ResourceLocation> ids
    ) {
        if (selectors == null) {
            return;
        }
        for (String selector : selectors) {
            if (selector == null || selector.isBlank()) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(selector.trim());
            if (id == null) {
                OneKeyMiner.LOGGER.warn("Ignoring invalid item id: {}", selector);
            } else {
                ids.add(id);
            }
        }
    }

    private static boolean isValidTargetSelector(String selector) {
        if (selector == null || selector.isBlank()) {
            return false;
        }
        if ("*".equals(selector)) {
            return true;
        }
        String value = selector.startsWith("#")
                ? selector.substring(1)
                : selector;
        return ResourceLocation.tryParse(value) != null;
    }

    private static boolean matchesAnyItemTag(
            ItemStack stack,
            Collection<TagKey<Item>> tags
    ) {
        for (TagKey<Item> tag : tags) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAnyBlockTag(
            Block block,
            Collection<TagKey<Block>> tags
    ) {
        for (TagKey<Block> tag : tags) {
            if (block.defaultBlockState().is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sharesAnyBlockTag(
            Block first,
            Block second,
            Collection<TagKey<Block>> tags
    ) {
        for (TagKey<Block> tag : tags) {
            if (first.defaultBlockState().is(tag)
                    && second.defaultBlockState().is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static <T> Set<T> immutableUnion(Set<T> first, Set<T> second) {
        Set<T> result = new HashSet<>(first);
        result.addAll(second);
        return Set.copyOf(result);
    }
    
    /**
     * 获取所有已注册的方块白名单（只读）
     * 
     * @return 方块白名单的不可变集合
     */
    public static Set<ResourceLocation> getBlockWhitelist() {
        return immutableUnion(BLOCK_WHITELIST, CONFIG_REGISTRATIONS.blockWhitelist());
    }
    
    /**
     * 获取所有已注册的方块黑名单（只读）
     * 
     * @return 方块黑名单的不可变集合
     */
    public static Set<ResourceLocation> getBlockBlacklist() {
        return immutableUnion(BLOCK_BLACKLIST, CONFIG_REGISTRATIONS.blockBlacklist());
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
        BLOCK_TAG_BLACKLIST.clear();
        TOOL_WHITELIST.clear();
        TOOL_BLACKLIST.clear();
        TOOL_TAG_WHITELIST.clear();
        TOOL_TAG_BLACKLIST.clear();
        BLOCK_GROUPS.clear();
        INTERACTION_TOOL_WHITELIST.clear();
        INTERACTION_TOOL_BLACKLIST.clear();
        INTERACTION_TOOL_TAG_WHITELIST.clear();
        INTERACTION_TOOL_TAG_BLACKLIST.clear();
        TOOL_ACTION_RULES.clear();
        INTERACTIVE_ITEM_WHITELIST.clear();
        INTERACTIVE_ITEM_BLACKLIST.clear();
        INTERACTION_VALIDATORS.clear();
        PLANTABLE_WHITELIST.clear();
        PLANTABLE_BLACKLIST.clear();
        PLANTABLE_TAG_WHITELIST.clear();
        PLANTABLE_TAG_BLACKLIST.clear();
        CONFIG_REGISTRATIONS = ConfigRegistrations.empty();
    }
    
    /**
     * 重新加载 API 数据
     * 
     * <p>清除现有数据并从配置重新加载</p>
     */
    public static void reload() {
        ConfigManager.reload();
        // Runtime addon registrations remain intact; only the immutable
        // config-derived snapshot is replaced.
        loadFromConfig();
        // 重新注册默认方块（由主类处理）
        OneKeyMiner.LOGGER.info("OneKeyMiner API 已重载");
    }
    
    // ==================== 形状注册 API ====================
    
    /**
     * 注册一个自定义连锁搜索形状
     * 
     * <p>附属模组可以调用此方法注册新的形状，注册后玩家可以在配置界面中选择。</p>
     * 
     * <h2>使用示例</h2>
     * <pre>{@code
     * OneKeyMinerAPI.registerShape(new MyCustomShape());
     * }</pre>
     * 
     * @param shape 形状实例，必须实现 {@link ChainShape} 接口
     * @throws NullPointerException 如果 shape 或其 ID 为 null
     * @see ChainShape
     * @see ShapeRegistry
     */
    public static void registerShape(ChainShape shape) {
        ShapeRegistry.register(shape);
    }
    
    /**
     * 获取已注册的形状
     * 
     * @param id 形状 ID（ResourceLocation）
     * @return 形状实例，如果不存在返回 null
     */
    public static ChainShape getShape(ResourceLocation id) {
        return id == null ? null : ShapeRegistry.getShape(id);
    }
    
    /**
     * 获取所有已注册的形状（按注册顺序）
     * 
     * @return 有序的形状列表（不可变）
     */
    public static List<ChainShape> getRegisteredShapes() {
        return ShapeRegistry.getAllShapes();
    }
    
    /**
     * 检查形状 ID 是否已注册
     * 
     * @param shapeId 形状 ID 字符串，格式 "namespace:path"
     * @return 如果已注册返回 true
     */
    public static boolean isShapeRegistered(String shapeId) {
        return shapeId != null && ShapeRegistry.isValidShapeId(shapeId);
    }
    
    // ==================== 预览 API ====================
    
    /**
     * 获取当前连锁预览的方块列表
     * 
     * <p>返回最近一次预览计算的结果，仅客户端可用。</p>
     * 
     * <h2>使用示例</h2>
     * <pre>{@code
     * List<BlockPos> preview = OneKeyMinerAPI.getPreviewBlocks();
     * for (BlockPos pos : preview) {
     *     // 自定义渲染逻辑
     * }
     * }</pre>
     * 
     * @return 不可变的方块位置列表，如果无预览则为空列表
     */
    public static List<net.minecraft.core.BlockPos> getPreviewBlocks() {
        return org.xiyu.onekeyminer.preview.ChainPreviewManager.getInstance().getPreviewBlocks();
    }
    
    /**
     * 添加预览变更监听器
     * 
     * <p>附属模组可注册监听器以在预览内容变化时获得通知，
     * 用于自定义渲染或额外逻辑。仅客户端有效。</p>
     * 
     * <h2>使用示例</h2>
     * <pre>{@code
     * OneKeyMinerAPI.addPreviewListener((blocks, shapeKey) -> {
     *     System.out.println("预览更新: " + blocks.size() + " 个方块, 形状: " + shapeKey);
     * });
     * }</pre>
     * 
     * @param listener 预览监听器
     */
    public static void addPreviewListener(org.xiyu.onekeyminer.preview.ChainPreviewManager.PreviewListener listener) {
        org.xiyu.onekeyminer.preview.ChainPreviewManager.getInstance().addListener(
                Objects.requireNonNull(listener, "listener")
        );
    }
    
    /**
     * 移除预览变更监听器
     * 
     * @param listener 要移除的监听器
     */
    public static void removePreviewListener(org.xiyu.onekeyminer.preview.ChainPreviewManager.PreviewListener listener) {
        org.xiyu.onekeyminer.preview.ChainPreviewManager.getInstance().removeListener(listener);
    }
    
    // ==================== 配置访问 API ====================
    
    /**
     * 获取当前选中的连锁形状 ID
     * 
     * @return 形状 ID 字符串，格式 "namespace:path"
     */
    public static String getSelectedShape() {
        return ConfigManager.getConfig().selectedShape;
    }
    
    /**
     * 设置当前连锁形状
     * 
     * <p>修改后会自动保存配置并同步到服务器。</p>
     * 
     * @param shapeId 形状 ID 字符串，格式 "namespace:path"
     * @return 如果设置成功返回 true，如果形状 ID 无效返回 false
     */
    public static boolean setSelectedShape(String shapeId) {
        if (shapeId == null || !ShapeRegistry.isValidShapeId(shapeId)) {
            OneKeyMiner.LOGGER.warn("无效的形状 ID: {}", shapeId);
            return false;
        }
        ConfigManager.editConfig("selectedShape", config -> config.selectedShape = shapeId);
        ConfigSyncHelper.triggerSync();
        return true;
    }
    
    /**
     * 检查是否启用掉落物传送
     * 
     * @return 如果启用返回 true
     */
    public static boolean isTeleportDropsEnabled() {
        return ConfigManager.getConfig().teleportDrops;
    }
    
    /**
     * 设置掉落物传送开关
     * 
     * <p>修改后会自动保存配置并同步到服务器。</p>
     * 
     * @param enabled 是否启用
     */
    public static void setTeleportDropsEnabled(boolean enabled) {
        ConfigManager.editConfig("teleportDrops", config -> config.teleportDrops = enabled);
        ConfigSyncHelper.triggerSync();
    }
    
    /**
     * 检查是否启用经验传送
     * 
     * @return 如果启用返回 true
     */
    public static boolean isTeleportExpEnabled() {
        return ConfigManager.getConfig().teleportExp;
    }
    
    /**
     * 设置经验传送开关
     * 
     * <p>修改后会自动保存配置并同步到服务器。</p>
     * 
     * @param enabled 是否启用
     */
    public static void setTeleportExpEnabled(boolean enabled) {
        ConfigManager.editConfig("teleportExp", config -> config.teleportExp = enabled);
        ConfigSyncHelper.triggerSync();
    }

    /** Latest server-authoritative preferences for the current client session. */
    public static java.util.Optional<ClientPreferenceAck> getAcknowledgedServerPreferences() {
        return ClientPreferenceSession.lastAck();
    }
    
    /**
     * 添加配置变更监听器
     * 
     * <p>当通过 API 修改配置时，监听器会被触发。
     * 监听器接收变更的配置键名（如 "selectedShape", "teleportDrops", "teleportExp"）。</p>
     * 
     * <h2>使用示例</h2>
     * <pre>{@code
     * OneKeyMinerAPI.addConfigChangeListener(key -> {
     *     if ("selectedShape".equals(key)) {
     *         System.out.println("形状变更为: " + OneKeyMinerAPI.getSelectedShape());
     *     }
     * });
     * }</pre>
     * 
     * @param listener 配置变更监听器
     */
    public static void addConfigChangeListener(java.util.function.Consumer<String> listener) {
        ConfigSyncHelper.addConfigChangeListener(listener);
    }
    
    /**
     * 移除配置变更监听器
     * 
     * @param listener 要移除的监听器
     * @return 如果成功移除返回 true
     */
    public static boolean removeConfigChangeListener(java.util.function.Consumer<String> listener) {
        return ConfigSyncHelper.removeConfigChangeListener(listener);
    }
}
