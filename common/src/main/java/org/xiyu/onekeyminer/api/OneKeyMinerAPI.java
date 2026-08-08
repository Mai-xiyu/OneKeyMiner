package org.xiyu.onekeyminer.api;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.chain.ChainActionType;
import org.xiyu.onekeyminer.chain.ServerUseBridge;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.ConfigSyncHelper;
import org.xiyu.onekeyminer.config.MinerConfig;
import org.xiyu.onekeyminer.mining.MiningStateManager;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;
import org.xiyu.onekeyminer.network.ClientPreferenceSession;
import org.xiyu.onekeyminer.registry.TagResolver;
import org.xiyu.onekeyminer.shape.ChainShape;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

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
 * <pre>
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
 * });
 * </pre>
 *
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.21.7
 * @see org.xiyu.onekeyminer.api.event.ChainEvents
 */
public final class OneKeyMinerAPI {

    /** 运行时方块白名单 */
    private static final Set<Identifier> BLOCK_WHITELIST = ConcurrentHashMap.newKeySet();

    /** 运行时方块黑名单 */
    private static final Set<Identifier> BLOCK_BLACKLIST = ConcurrentHashMap.newKeySet();

    /** 运行时方块标签白名单 */
    private static final Set<TagKey<Block>> BLOCK_TAG_WHITELIST = ConcurrentHashMap.newKeySet();

    /** 运行时方块标签黑名单 */
    private static final Set<TagKey<Block>> BLOCK_TAG_BLACKLIST = ConcurrentHashMap.newKeySet();

    /** 运行时工具白名单 */
    private static final Set<Identifier> TOOL_WHITELIST = ConcurrentHashMap.newKeySet();

    /** 运行时工具黑名单 */
    private static final Set<Identifier> TOOL_BLACKLIST = ConcurrentHashMap.newKeySet();

    /** 运行时工具标签白名单 */
    private static final Set<TagKey<Item>> TOOL_TAG_WHITELIST = ConcurrentHashMap.newKeySet();

    /** 运行时工具标签黑名单 */
    private static final Set<TagKey<Item>> TOOL_TAG_BLACKLIST = ConcurrentHashMap.newKeySet();

    /** 方块分组映射（用于宽松匹配） */
    private static final Map<Identifier, String> BLOCK_GROUPS = new ConcurrentHashMap<>();

    /**
     * Config-derived registrations are replaced as one immutable snapshot.
     * Runtime registrations from built-ins and add-ons remain independent.
     */
    private record ConfigRegistrations(
            Set<Identifier> blockWhitelist,
            Set<Identifier> blockBlacklist,
            Set<TagKey<Block>> blockTagWhitelist,
            Set<TagKey<Block>> blockTagBlacklist,
            Set<Identifier> toolWhitelist,
            Set<Identifier> toolBlacklist,
            Set<TagKey<Item>> toolTagWhitelist,
            Set<TagKey<Item>> toolTagBlacklist,
            Set<Identifier> interactionToolWhitelist,
            Set<Identifier> interactionToolBlacklist,
            Set<TagKey<Item>> interactionToolTagWhitelist,
            Set<TagKey<Item>> interactionToolTagBlacklist,
            Set<Identifier> interactiveItemWhitelist,
            Set<Identifier> interactiveItemBlacklist,
            Set<Identifier> plantableWhitelist,
            Set<Identifier> plantableBlacklist,
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

        Set<Identifier> blockWhitelist = new HashSet<>();
        Set<Identifier> blockBlacklist = new HashSet<>();
        Set<TagKey<Block>> blockTagWhitelist = new HashSet<>();
        Set<TagKey<Block>> blockTagBlacklist = new HashSet<>();
        Set<Identifier> toolWhitelist = new HashSet<>();
        Set<Identifier> toolBlacklist = new HashSet<>();
        Set<TagKey<Item>> toolTagWhitelist = new HashSet<>();
        Set<TagKey<Item>> toolTagBlacklist = new HashSet<>();
        Set<Identifier> interactionToolWhitelist = new HashSet<>();
        Set<Identifier> interactionToolBlacklist = new HashSet<>();
        Set<TagKey<Item>> interactionToolTagWhitelist = new HashSet<>();
        Set<TagKey<Item>> interactionToolTagBlacklist = new HashSet<>();
        Set<Identifier> interactiveItemWhitelist = new HashSet<>();
        Set<Identifier> interactiveItemBlacklist = new HashSet<>();
        Set<Identifier> plantableWhitelist = new HashSet<>();
        Set<Identifier> plantableBlacklist = new HashSet<>();
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
        if (blockId != null && blockId.startsWith("#")) {
            return registerBlockTag(blockId);
        }
        Identifier loc = parseIdentifier(blockId);
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
        Identifier loc = BuiltInRegistries.BLOCK.getKey(block);
        return loc != null && BLOCK_WHITELIST.add(loc);
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
        Identifier loc = parseIdentifier(stripTagPrefix(tagId));
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
        if (blockId != null && blockId.startsWith("#")) {
            return unregisterBlockTag(blockId);
        }
        Identifier loc = parseIdentifier(blockId);
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
        Identifier loc = parseIdentifier(stripTagPrefix(tagId));
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
        if (blockId != null && blockId.startsWith("#")) {
            return blacklistBlockTag(blockId);
        }
        Identifier loc = parseIdentifier(blockId);
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
        Identifier loc = BuiltInRegistries.BLOCK.getKey(block);
        return loc != null && BLOCK_BLACKLIST.add(loc);
    }

    /**
     * 从黑名单移除方块
     *
     * @param blockId 方块 ID
     * @return 如果移除成功返回 true
     */
    public static boolean unblacklistBlock(String blockId) {
        if (blockId != null && blockId.startsWith("#")) {
            return unblacklistBlockTag(blockId);
        }
        Identifier loc = parseIdentifier(blockId);
        if (loc == null) return false;
        return BLOCK_BLACKLIST.remove(loc);
    }

    /**
     * 将方块标签添加到运行时黑名单。可带或不带 {@code #} 前缀。
     */
    public static boolean blacklistBlockTag(String tagId) {
        Identifier loc = parseIdentifier(stripTagPrefix(tagId));
        if (loc == null) {
            OneKeyMiner.LOGGER.warn("无效的方块标签: {}", tagId);
            return false;
        }
        return BLOCK_TAG_BLACKLIST.add(TagKey.create(Registries.BLOCK, loc));
    }

    /**
     * 从运行时方块标签黑名单移除标签。可带或不带 {@code #} 前缀。
     */
    public static boolean unblacklistBlockTag(String tagId) {
        Identifier loc = parseIdentifier(stripTagPrefix(tagId));
        return loc != null
                && BLOCK_TAG_BLACKLIST.remove(TagKey.create(Registries.BLOCK, loc));
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
        if (itemId.startsWith("#")) {
            String tagId = itemId.substring(1);
            Identifier loc = parseIdentifier(tagId);
            if (loc == null) {
                OneKeyMiner.LOGGER.warn("无效的物品标签: {}", itemId);
                return false;
            }
            return TOOL_TAG_WHITELIST.add(TagKey.create(Registries.ITEM, loc));
        }
        Identifier loc = parseIdentifier(itemId);
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
        if (itemId.startsWith("#")) {
            String tagId = itemId.substring(1);
            Identifier loc = parseIdentifier(tagId);
            if (loc == null) {
                OneKeyMiner.LOGGER.warn("无效的物品标签: {}", itemId);
                return false;
            }
            return TOOL_TAG_BLACKLIST.add(TagKey.create(Registries.ITEM, loc));
        }
        Identifier loc = parseIdentifier(itemId);
        if (loc == null) {
            OneKeyMiner.LOGGER.warn("无效的物品 ID: {}", itemId);
            return false;
        }
        return TOOL_BLACKLIST.add(loc);
    }

    public static boolean unwhitelistTool(String itemId) {
        return removeItemSelector(itemId, TOOL_WHITELIST, TOOL_TAG_WHITELIST);
    }

    public static boolean unblacklistTool(String itemId) {
        return removeItemSelector(itemId, TOOL_BLACKLIST, TOOL_TAG_BLACKLIST);
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
        Identifier loc = parseIdentifier(blockId);
        if (loc != null && groupId != null && !groupId.isBlank()) {
            BLOCK_GROUPS.put(loc, groupId.trim());
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
        Identifier loc1 = BuiltInRegistries.BLOCK.getKey(block1);
        Identifier loc2 = BuiltInRegistries.BLOCK.getKey(block2);

        String group1 = BLOCK_GROUPS.get(loc1);
        String group2 = BLOCK_GROUPS.get(loc2);

        if (group1 != null && group1.equals(group2)) {
            return true;
        }

        return shareConfiguredBlockTag(block1, block2);
    }

    /**
     * 检查两个方块是否共享同一个已注册的白名单标签
     *
     * @param block1 方块 1
     * @param block2 方块 2
     * @return 如果共享标签返回 true
     */
    public static boolean blocksShareTag(Block block1, Block block2) {
        if (block1 == null || block2 == null) {
            return false;
        }
        return shareConfiguredBlockTag(block1, block2);
    }

    // ==================== 交互工具 API ====================

    /** 交互工具白名单 */
    private static final Set<Identifier> INTERACTION_TOOL_WHITELIST = ConcurrentHashMap.newKeySet();

    /** 交互工具黑名单 */
    private static final Set<Identifier> INTERACTION_TOOL_BLACKLIST = ConcurrentHashMap.newKeySet();

    /** 交互工具标签白名单 */
    private static final Set<TagKey<Item>> INTERACTION_TOOL_TAG_WHITELIST = ConcurrentHashMap.newKeySet();

    /** 交互工具标签黑名单 */
    private static final Set<TagKey<Item>> INTERACTION_TOOL_TAG_BLACKLIST = ConcurrentHashMap.newKeySet();

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
        if (itemId.startsWith("#")) {
            String tagId = itemId.substring(1);
            Identifier loc = parseIdentifier(tagId);
            if (loc == null) {
                OneKeyMiner.LOGGER.warn("无效的交互工具标签: {}", itemId);
                return false;
            }
            return INTERACTION_TOOL_TAG_WHITELIST.add(TagKey.create(Registries.ITEM, loc));
        }
        Identifier loc = parseIdentifier(itemId);
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
        if (itemId.startsWith("#")) {
            String tagId = itemId.substring(1);
            Identifier loc = parseIdentifier(tagId);
            if (loc == null) {
                OneKeyMiner.LOGGER.warn("无效的交互工具标签: {}", itemId);
                return false;
            }
            return INTERACTION_TOOL_TAG_BLACKLIST.add(TagKey.create(Registries.ITEM, loc));
        }
        Identifier loc = parseIdentifier(itemId);
        if (loc == null) {
            OneKeyMiner.LOGGER.warn("无效的交互工具 ID: {}", itemId);
            return false;
        }
        return INTERACTION_TOOL_BLACKLIST.add(loc);
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
            targets = targets == null ? List.of() : List.copyOf(targets);
        }
    }

    public record ToolSelector(Identifier itemId, TagKey<Item> itemTag) {
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
                Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
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

        if (targetType == null || actionType == null
                || (actionType == ChainActionType.INTERACTION && interactionRule == null)) {
            OneKeyMiner.LOGGER.warn(
                    "Incomplete tool action rule: targetType={}, actionType={}, interactionRule={}",
                    targetType,
                    actionType,
                    interactionRule
            );
            return false;
        }
        if (targetType == ToolTargetType.ENTITY && actionType != ChainActionType.INTERACTION) {
            OneKeyMiner.LOGGER.warn(
                    "Entity tool rules only support INTERACTION, got {}",
                    actionType
            );
            return false;
        }
        if (actionType != ChainActionType.INTERACTION) {
            interactionRule = null;
        }

        List<String> normalizedTargets = targets == null
                ? List.of()
                : targets.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(target -> !target.isEmpty())
                        .distinct()
                        .toList();
        TOOL_ACTION_RULES.add(new ToolActionRule(selector, targetType, actionType, interactionRule, normalizedTargets));
        return true;
    }

    public static List<ToolActionRule> getToolActionRules() {
        return List.copyOf(TOOL_ACTION_RULES);
    }

    public static boolean unregisterToolAction(ToolActionRule rule) {
        return rule != null && TOOL_ACTION_RULES.remove(rule);
    }

    /**
     * Executes a server-side block action implemented by a consuming loader
     * callback, then authorizes derived targets only when that action
     * completed according to its registered rule.
     *
     * <p>Use this only when the callback itself performs the action and
     * returns a consuming result. Do not wrap a callback that returns
     * {@link InteractionResult#PASS} and lets vanilla continue, or the
     * original action would run twice.</p>
     */
    public static InteractionResult executeBlockUseWithChain(
            ServerPlayer player,
            InteractionHand hand,
            BlockHitResult hitResult,
            Supplier<InteractionResult> originalUse
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(hand, "hand");
        Objects.requireNonNull(hitResult, "hitResult");
        Objects.requireNonNull(originalUse, "originalUse");
        return ServerUseBridge.runBlockUse(
                player,
                player.level(),
                player.getItemInHand(hand),
                hand,
                hitResult,
                originalUse
        );
    }

    /**
     * Entity counterpart of {@link #executeBlockUseWithChain}. It is
     * intended for consuming loader callbacks and exact-position entity
     * hooks that implement the original action themselves.
     */
    public static InteractionResult executeEntityUseWithChain(
            ServerPlayer player,
            Entity target,
            InteractionHand hand,
            Supplier<InteractionResult> originalUse
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(hand, "hand");
        Objects.requireNonNull(originalUse, "originalUse");
        return ServerUseBridge.runEntityUse(
                player,
                target,
                hand,
                player.getItemInHand(hand),
                originalUse
        );
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
     *
     * @deprecated Prefer an action-filtered overload. A tool may own rules for
     * multiple actions, so an unfiltered lookup can select the wrong rule.
     */
    @Deprecated(forRemoval = false)
    public static Optional<ToolActionRule> findToolActionForBlock(ItemStack stack, BlockState state) {
        for (ToolActionRule rule : TOOL_ACTION_RULES) {
            if (isToolActionRuleApplicableToBlock(rule, stack, state)) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    /**
     * 查询工具在指定方块上、指定动作类型中的第一条自定义规则。
     *
     * <p>同一工具可以同时注册挖掘和右键动作。调用方应使用本重载，
     * 避免其他动作类型中更早注册的规则遮蔽本次动作。</p>
     */
    public static Optional<ToolActionRule> findToolActionForBlock(
            ItemStack stack,
            BlockState state,
            Set<ChainActionType> actionTypes
    ) {
        Objects.requireNonNull(actionTypes, "actionTypes");
        for (ToolActionRule rule : TOOL_ACTION_RULES) {
            if (actionTypes.contains(rule.actionType())
                    && isToolActionRuleApplicableToBlock(rule, stack, state)) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    /**
     * 查询工具在指定方块上的指定动作规则。
     */
    public static Optional<ToolActionRule> findToolActionForBlock(
            ItemStack stack,
            BlockState state,
            ChainActionType actionType
    ) {
        return findToolActionForBlock(
                stack,
                state,
                Set.of(Objects.requireNonNull(actionType, "actionType"))
        );
    }

    /**
     * 查询工具在指定实体上的自定义规则
     *
     * @deprecated Prefer the action-filtered overload for forward-compatible
     * rule selection.
     */
    @Deprecated(forRemoval = false)
    public static Optional<ToolActionRule> findToolActionForEntity(ItemStack stack, Entity entity) {
        for (ToolActionRule rule : TOOL_ACTION_RULES) {
            if (isToolActionRuleApplicableToEntity(rule, stack, entity)) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    /**
     * 查询工具在指定实体上的指定动作规则。
     */
    public static Optional<ToolActionRule> findToolActionForEntity(
            ItemStack stack,
            Entity entity,
            ChainActionType actionType
    ) {
        Objects.requireNonNull(actionType, "actionType");
        for (ToolActionRule rule : TOOL_ACTION_RULES) {
            if (rule.actionType() == actionType
                    && isToolActionRuleApplicableToEntity(rule, stack, entity)) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    /**
     * 检查一条规则是否准确适用于指定工具和方块。
     *
     * <p>调用方可以保留在起始目标上选中的规则，并用本方法验证每个派生目标，
     * 避免另一条同工具规则意外扩大本次操作范围。</p>
     */
    public static boolean isToolActionRuleApplicableToBlock(
            ToolActionRule rule,
            ItemStack stack,
            BlockState state
    ) {
        return rule != null
                && stack != null
                && state != null
                && rule.targetType() == ToolTargetType.BLOCK
                && rule.toolSelector().matches(stack)
                && matchesBlockTargets(state, rule.targets());
    }

    /**
     * 检查一条规则是否准确适用于指定工具和实体。
     *
     * <p>与 {@link #isToolActionRuleApplicableToBlock(ToolActionRule, ItemStack, BlockState)}
     * 一样，本方法验证传入的同一条规则，而不是重新选择任意匹配规则。</p>
     */
    public static boolean isToolActionRuleApplicableToEntity(
            ToolActionRule rule,
            ItemStack stack,
            Entity entity
    ) {
        return rule != null
                && stack != null
                && entity != null
                && rule.targetType() == ToolTargetType.ENTITY
                && rule.toolSelector().matches(stack)
                && matchesEntityTargets(entity, rule.targets());
    }

    /**
     * 检查工具是否存在任意自定义动作规则
     */
    public static boolean hasToolActionRule(ItemStack stack, ChainActionType actionType) {
        if (stack == null) {
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
        if (selector == null || selector.isBlank()) {
            return null;
        }
        if (selector.startsWith("#")) {
            Identifier loc = parseIdentifier(selector.substring(1));
            if (loc == null) {
                return null;
            }
            TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), loc);
            return new ToolSelector(null, tag);
        }
        Identifier loc = parseIdentifier(selector);
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
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        for (String entry : targets) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            if ("*".equals(entry)) {
                return true;
            }
            if (entry.startsWith("#")) {
                Identifier tagId = parseIdentifier(entry.substring(1));
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
     * 检查物品是否被交互工具黑名单明确拒绝。
     *
     * <p>同时检查运行时注册以及当前不可变配置快照中的物品 ID 和标签。</p>
     *
     * @param stack 物品栈
     * @return 命中任一交互工具黑名单时返回 true
     */
    public static boolean isInteractionToolBlacklisted(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        ConfigRegistrations configured = CONFIG_REGISTRATIONS;
        Identifier loc = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return INTERACTION_TOOL_BLACKLIST.contains(loc)
                || configured.interactionToolBlacklist().contains(loc)
                || matchesAnyItemTag(stack, INTERACTION_TOOL_TAG_BLACKLIST)
                || matchesAnyItemTag(stack, configured.interactionToolTagBlacklist());
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

        if (isInteractionToolBlacklisted(stack)) {
            return false;
        }

        ConfigRegistrations configured = CONFIG_REGISTRATIONS;
        Identifier loc = BuiltInRegistries.ITEM.getKey(stack.getItem());

        // 检查白名单（ID + 标签）
        boolean hasWhitelist = !INTERACTION_TOOL_WHITELIST.isEmpty()
                || !INTERACTION_TOOL_TAG_WHITELIST.isEmpty()
                || !configured.interactionToolWhitelist().isEmpty()
                || !configured.interactionToolTagWhitelist().isEmpty();
        if (!hasWhitelist) {
            return false;
        }
        return INTERACTION_TOOL_WHITELIST.contains(loc)
                || configured.interactionToolWhitelist().contains(loc)
                || matchesAnyItemTag(stack, INTERACTION_TOOL_TAG_WHITELIST)
                || matchesAnyItemTag(stack, configured.interactionToolTagWhitelist());
    }

    // ==================== 交互物品 API ====================

    /** 通用交互物品白名单（骨粉、刷子等消耗型交互物品） */
    private static final Set<Identifier> INTERACTIVE_ITEM_WHITELIST = ConcurrentHashMap.newKeySet();

    /** 通用交互物品黑名单 */
    private static final Set<Identifier> INTERACTIVE_ITEM_BLACKLIST = ConcurrentHashMap.newKeySet();

    /** 交互验证器列表 */
    private static final List<InteractionValidator> INTERACTION_VALIDATORS = new CopyOnWriteArrayList<>();

    /**
     * 交互验证器接口
     */
    @FunctionalInterface
    public interface InteractionValidator {
        boolean canInteract(ItemStack stack, BlockState state);
    }

    /**
     * 注册交互物品到白名单
     */
    public static boolean registerInteractiveItem(String itemId) {
        Identifier loc = parseIdentifier(itemId);
        if (loc == null) {
            OneKeyMiner.LOGGER.warn("无效的交互物品 ID: {}", itemId);
            return false;
        }
        return INTERACTIVE_ITEM_WHITELIST.add(loc);
    }

    /**
     * 将交互物品加入黑名单
     */
    public static boolean blacklistInteractiveItem(String itemId) {
        Identifier loc = parseIdentifier(itemId);
        if (loc == null) {
            OneKeyMiner.LOGGER.warn("无效的交互物品 ID: {}", itemId);
            return false;
        }
        return INTERACTIVE_ITEM_BLACKLIST.add(loc);
    }

    /**
     * 注册交互验证器
     */
    public static void registerInteractionValidator(InteractionValidator validator) {
        INTERACTION_VALIDATORS.add(Objects.requireNonNull(validator, "validator"));
    }

    public static boolean unregisterInteractionValidator(InteractionValidator validator) {
        return validator != null && INTERACTION_VALIDATORS.remove(validator);
    }

    /**
     * 检查物品是否为允许的交互物品
     */
    public static boolean isInteractiveItemAllowed(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ConfigRegistrations configured = CONFIG_REGISTRATIONS;
        Identifier loc = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (INTERACTIVE_ITEM_BLACKLIST.contains(loc)
                || configured.interactiveItemBlacklist().contains(loc)) {
            return false;
        }
        if (!INTERACTIVE_ITEM_WHITELIST.isEmpty()
                || !configured.interactiveItemWhitelist().isEmpty()) {
            return INTERACTIVE_ITEM_WHITELIST.contains(loc)
                    || configured.interactiveItemWhitelist().contains(loc);
        }
        return false;
    }

    /**
     * 验证物品是否可以与方块交互
     */
    public static boolean validateInteraction(ItemStack stack, BlockState state) {
        if (stack == null || state == null) {
            return false;
        }
        for (InteractionValidator validator : INTERACTION_VALIDATORS) {
            try {
                if (!validator.canInteract(stack, state)) {
                    return false;
                }
            } catch (RuntimeException e) {
                OneKeyMiner.LOGGER.error("Interaction validator failed", e);
                return false;
            }
        }
        return true;
    }

    // ==================== 种植物品 API ====================

    /** 种植物品白名单 */
    private static final Set<Identifier> PLANTABLE_WHITELIST = ConcurrentHashMap.newKeySet();

    /** 种植物品黑名单 */
    private static final Set<Identifier> PLANTABLE_BLACKLIST = ConcurrentHashMap.newKeySet();

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
        if (itemId.startsWith("#")) {
            String tagId = itemId.substring(1);
            Identifier loc = parseIdentifier(tagId);
            if (loc == null) {
                OneKeyMiner.LOGGER.warn("无效的种植物品标签: {}", itemId);
                return false;
            }
            return PLANTABLE_TAG_WHITELIST.add(TagKey.create(Registries.ITEM, loc));
        }
        Identifier loc = parseIdentifier(itemId);
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
        if (itemId.startsWith("#")) {
            String tagId = itemId.substring(1);
            Identifier loc = parseIdentifier(tagId);
            if (loc == null) {
                OneKeyMiner.LOGGER.warn("无效的种子标签: {}", itemId);
                return false;
            }
            return PLANTABLE_TAG_BLACKLIST.add(TagKey.create(Registries.ITEM, loc));
        }
        Identifier loc = parseIdentifier(itemId);
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
        Identifier loc = BuiltInRegistries.ITEM.getKey(item);
        if (PLANTABLE_BLACKLIST.contains(loc)
                || configured.plantableBlacklist().contains(loc)) {
            return true;
        }
        return matchesAnyItemTag(item, PLANTABLE_TAG_BLACKLIST)
                || matchesAnyItemTag(item, configured.plantableTagBlacklist());
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
        Identifier loc = BuiltInRegistries.ITEM.getKey(stack.getItem());

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
        boolean hasWhitelist = !PLANTABLE_WHITELIST.isEmpty()
                || !PLANTABLE_TAG_WHITELIST.isEmpty()
                || !configured.plantableWhitelist().isEmpty()
                || !configured.plantableTagWhitelist().isEmpty();
        if (!hasWhitelist) {
            return false;
        }
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
        ConfigRegistrations configured = CONFIG_REGISTRATIONS;
        Identifier loc = BuiltInRegistries.BLOCK.getKey(block);
        MinerConfig config = ConfigManager.getConfig();

        // Blacklists, including tag blacklists, always win.
        if (isBlockBlacklisted(block)) {
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
        Identifier loc = BuiltInRegistries.BLOCK.getKey(block);
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
        Identifier loc = BuiltInRegistries.ITEM.getKey(tool.getItem());

        if (isToolBlacklisted(tool)) {
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

    /**
     * Checks the runtime and configured mining-tool blacklists.
     *
     * @param tool tool stack to inspect
     * @return {@code true} when an item ID or tag blacklist matches
     */
    public static boolean isToolBlacklisted(ItemStack tool) {
        if (tool == null || tool.isEmpty()) {
            return false;
        }
        ConfigRegistrations configured = CONFIG_REGISTRATIONS;
        Identifier loc = BuiltInRegistries.ITEM.getKey(tool.getItem());
        return TOOL_BLACKLIST.contains(loc)
                || configured.toolBlacklist().contains(loc)
                || matchesAnyItemTag(tool, TOOL_TAG_BLACKLIST)
                || matchesAnyItemTag(tool, configured.toolTagBlacklist());
    }

    /**
     * 获取所有已注册的方块白名单（只读）
     *
     * @return 方块白名单的不可变集合
     */
    public static Set<Identifier> getBlockWhitelist() {
        return immutableUnion(BLOCK_WHITELIST, CONFIG_REGISTRATIONS.blockWhitelist());
    }

    /**
     * 获取所有已注册的方块黑名单（只读）
     *
     * @return 方块黑名单的不可变集合
     */
    public static Set<Identifier> getBlockBlacklist() {
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
     * <p>仅原子替换配置派生数据；不会清除内建或第三方运行时注册。</p>
     */
    public static void reload() {
        loadFromConfig();
        OneKeyMiner.LOGGER.info("OneKeyMiner API 已重载");
    }

    // ==================== Shape API ====================

    public static void registerShape(ChainShape shape) {
        ShapeRegistry.register(Objects.requireNonNull(shape, "shape"));
    }

    public static boolean unregisterShape(Identifier id) {
        if (id == null) {
            return false;
        }
        if (id.toString().equals(ConfigManager.getConfig().selectedShape)) {
            OneKeyMiner.LOGGER.warn("Cannot unregister the currently selected chain shape: {}", id);
            return false;
        }
        return ShapeRegistry.unregister(id);
    }

    public static ChainShape getShape(Identifier id) {
        return id != null ? ShapeRegistry.getShape(id) : null;
    }

    public static List<ChainShape> getRegisteredShapes() {
        return ShapeRegistry.getAllShapes();
    }

    public static boolean isShapeRegistered(String shapeId) {
        return ShapeRegistry.isValidShapeId(shapeId);
    }

    public static List<net.minecraft.core.BlockPos> getPreviewBlocks() {
        return org.xiyu.onekeyminer.preview.ChainPreviewManager.getInstance().getPreviewBlocks();
    }

    public static void addPreviewListener(org.xiyu.onekeyminer.preview.ChainPreviewManager.PreviewListener listener) {
        org.xiyu.onekeyminer.preview.ChainPreviewManager.getInstance().addListener(listener);
    }

    public static boolean removePreviewListener(org.xiyu.onekeyminer.preview.ChainPreviewManager.PreviewListener listener) {
        return org.xiyu.onekeyminer.preview.ChainPreviewManager.getInstance().removeListener(listener);
    }

    public static String getSelectedShape() {
        return ConfigManager.getConfig().selectedShape;
    }

    public static boolean setSelectedShape(String shapeId) {
        if (shapeId == null || !ShapeRegistry.isValidShapeId(shapeId)) {
            OneKeyMiner.LOGGER.warn("Invalid chain shape ID: {}", shapeId);
            return false;
        }
        ConfigManager.editConfig("selectedShape", config -> {
            config.selectedShape = shapeId;
            config.shapeMode = null;
        });
        return true;
    }

    /**
     * Returns the local client's drop-teleport request.
     *
     * @return local preference, not the effective dedicated-server policy
     * @deprecated use {@link #isLocalDropTeleportRequested()}
     */
    @Deprecated
    public static boolean isTeleportDropsEnabled() {
        return isLocalDropTeleportRequested();
    }

    /**
     * Updates the local client's drop-teleport request. A remote server may
     * still reject it through its policy gate.
     *
     * @param enabled requested value
     * @deprecated use {@link #setLocalDropTeleportRequested(boolean)}
     */
    @Deprecated
    public static void setTeleportDropsEnabled(boolean enabled) {
        setLocalDropTeleportRequested(enabled);
    }

    /**
     * Returns the local drop-teleport preference sent by a physical client.
     *
     * @return the local request
     */
    public static boolean isLocalDropTeleportRequested() {
        return ConfigManager.getClientPreferencesSnapshot().teleportDrops();
    }

    /**
     * Sets and synchronizes the local drop-teleport preference.
     *
     * @param enabled requested value
     */
    public static void setLocalDropTeleportRequested(boolean enabled) {
        ConfigManager.editConfig("teleportDrops", config -> config.teleportDrops = enabled);
    }

    /**
     * Returns the local client's experience-teleport request.
     *
     * @return local preference, not the effective dedicated-server policy
     * @deprecated use {@link #isLocalExperienceTeleportRequested()}
     */
    @Deprecated
    public static boolean isTeleportExpEnabled() {
        return isLocalExperienceTeleportRequested();
    }

    /**
     * Updates the local client's experience-teleport request. A remote server
     * may still reject it through its policy gate.
     *
     * @param enabled requested value
     * @deprecated use {@link #setLocalExperienceTeleportRequested(boolean)}
     */
    @Deprecated
    public static void setTeleportExpEnabled(boolean enabled) {
        setLocalExperienceTeleportRequested(enabled);
    }

    /**
     * Returns the local experience-teleport preference sent by a client.
     *
     * @return the local request
     */
    public static boolean isLocalExperienceTeleportRequested() {
        return ConfigManager.getClientPreferencesSnapshot().teleportExp();
    }

    /**
     * Sets and synchronizes the local experience-teleport preference.
     *
     * @param enabled requested value
     */
    public static void setLocalExperienceTeleportRequested(boolean enabled) {
        ConfigManager.editConfig("teleportExp", config -> config.teleportExp = enabled);
    }

    /**
     * Returns the latest preference snapshot acknowledged and applied by the
     * currently connected server. The value is empty before the first ACK and
     * after disconnect or a local preference edit.
     *
     * @return the latest server-authoritative acknowledgement, if available
     */
    public static Optional<ClientPreferenceAck> getAcknowledgedServerPreferences() {
        return ClientPreferenceSession.lastAck();
    }

    /**
     * Returns the authoritative server policy for client drop requests.
     *
     * @return whether the server honors a client's drop-teleport request
     */
    public static boolean isClientDropTeleportAllowed() {
        return ConfigManager.getServerPreferenceSnapshot().allowClientTeleportDrops();
    }

    /**
     * Updates the authoritative server policy for client drop requests.
     *
     * @param allowed whether the server should honor client requests
     */
    public static void setClientDropTeleportAllowed(boolean allowed) {
        ConfigManager.editConfig(
                "allowClientTeleportDrops",
                config -> config.allowClientTeleportDrops = allowed
        );
    }

    /**
     * Returns the authoritative server policy for client XP requests.
     *
     * @return whether the server honors a client's experience-teleport request
     */
    public static boolean isClientExperienceTeleportAllowed() {
        return ConfigManager.getServerPreferenceSnapshot().allowClientTeleportExp();
    }

    /**
     * Updates the authoritative server policy for client XP requests.
     *
     * @param allowed whether the server should honor client requests
     */
    public static void setClientExperienceTeleportAllowed(boolean allowed) {
        ConfigManager.editConfig(
                "allowClientTeleportExp",
                config -> config.allowClientTeleportExp = allowed
        );
    }

    /**
     * Resolves a player's effective drop behavior from request and policy.
     *
     * @param player server player whose request should be resolved
     * @return the effective server-authoritative value
     */
    public static boolean isDropTeleportEffective(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        ConfigManager.ServerPreferenceSnapshot policy =
                ConfigManager.getServerPreferenceSnapshot();
        return MiningStateManager.isTeleportDrops(player)
                && policy.allowClientTeleportDrops();
    }

    /**
     * Resolves a player's effective XP behavior from request and policy.
     *
     * @param player server player whose request should be resolved
     * @return the effective server-authoritative value
     */
    public static boolean isExperienceTeleportEffective(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        ConfigManager.ServerPreferenceSnapshot policy =
                ConfigManager.getServerPreferenceSnapshot();
        return MiningStateManager.isTeleportExp(player)
                && policy.allowClientTeleportExp();
    }

    /**
     * 添加配置变更监听器
     *
     * <p>当通过 API 修改配置时，监听器会被触发。
     * 监听器接收变更的配置键名（如 "teleportDrops", "teleportExp"）。</p>
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

    private static void addBlockSelectors(
            List<String> entries,
            Set<Identifier> ids,
            Set<TagKey<Block>> tags
    ) {
        if (entries == null) {
            return;
        }
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            if (entry.startsWith("#")) {
                Identifier id = parseIdentifier(entry.substring(1));
                if (id != null) {
                    tags.add(TagKey.create(Registries.BLOCK, id));
                }
            } else {
                Identifier id = parseIdentifier(entry);
                if (id != null) {
                    ids.add(id);
                }
            }
        }
    }

    private static void addItemSelectors(
            List<String> entries,
            Set<Identifier> ids,
            Set<TagKey<Item>> tags
    ) {
        if (entries == null) {
            return;
        }
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            if (entry.startsWith("#")) {
                Identifier id = parseIdentifier(entry.substring(1));
                if (id != null) {
                    tags.add(TagKey.create(Registries.ITEM, id));
                }
            } else {
                Identifier id = parseIdentifier(entry);
                if (id != null) {
                    ids.add(id);
                }
            }
        }
    }

    private static void addIds(List<String> entries, Set<Identifier> ids) {
        if (entries == null) {
            return;
        }
        for (String entry : entries) {
            Identifier id = parseIdentifier(entry);
            if (id != null) {
                ids.add(id);
            }
        }
    }

    private static boolean removeItemSelector(
            String selector,
            Set<Identifier> ids,
            Set<TagKey<Item>> tags
    ) {
        if (selector == null || selector.isBlank()) {
            return false;
        }
        if (selector.startsWith("#")) {
            Identifier id = parseIdentifier(selector.substring(1));
            return id != null && tags.remove(TagKey.create(Registries.ITEM, id));
        }
        Identifier id = parseIdentifier(selector);
        return id != null && ids.remove(id);
    }

    private static String stripTagPrefix(String value) {
        return value != null && value.startsWith("#") ? value.substring(1) : value;
    }

    private static boolean shareConfiguredBlockTag(Block block1, Block block2) {
        ConfigRegistrations configured = CONFIG_REGISTRATIONS;
        return shareBlockTag(block1, block2, BLOCK_TAG_WHITELIST)
                || shareBlockTag(block1, block2, configured.blockTagWhitelist());
    }

    private static boolean shareBlockTag(
            Block block1,
            Block block2,
            Collection<TagKey<Block>> tags
    ) {
        for (TagKey<Block> tag : tags) {
            if (block1.defaultBlockState().is(tag) && block2.defaultBlockState().is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAnyBlockTag(Block block, Collection<TagKey<Block>> tags) {
        for (TagKey<Block> tag : tags) {
            if (block.defaultBlockState().is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAnyItemTag(ItemStack stack, Collection<TagKey<Item>> tags) {
        for (TagKey<Item> tag : tags) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAnyItemTag(Item item, Collection<TagKey<Item>> tags) {
        for (TagKey<Item> tag : tags) {
            if (item.builtInRegistryHolder().is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static <T> Set<T> immutableUnion(Set<T> first, Set<T> second) {
        if (first.isEmpty()) {
            return Set.copyOf(second);
        }
        if (second.isEmpty()) {
            return Set.copyOf(first);
        }
        Set<T> combined = new HashSet<>(first);
        combined.addAll(second);
        return Set.copyOf(combined);
    }

    private static Identifier parseIdentifier(String value) {
        return value == null || value.isBlank() ? null : Identifier.tryParse(value.trim());
    }
}
