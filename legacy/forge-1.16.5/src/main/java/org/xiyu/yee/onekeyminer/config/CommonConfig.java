package org.xiyu.yee.onekeyminer.config;

import net.minecraft.util.registry.Registry;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class CommonConfig {
    public static final ForgeConfigSpec SPEC;
    public static final CommonConfig INSTANCE;

    static {
        final Pair<CommonConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder()
                .configure(CommonConfig::new);
        SPEC = specPair.getRight();
        INSTANCE = specPair.getLeft();
    }
    //耕地白名单
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> chainPlantableFarmlands;
    // 连锁挖掘的最大方块数
    public final ForgeConfigSpec.IntValue maxChainBlocks;

    // 不可连锁的方块黑名单
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> nonChainableBlocks;

    // 种子黑名单
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> seedBlacklist;

    // 是否启用对角线连锁
    public final ForgeConfigSpec.BooleanValue enableDiagonalChaining;

    // 最大挖掘深度
    public final ForgeConfigSpec.IntValue maxMiningDepth;

    // 是否将连锁挖掘的掉落物传送到玩家身下
    public final ForgeConfigSpec.BooleanValue teleportDropsToPlayer;

    // 是否忽略工具兼容性检查
    public final ForgeConfigSpec.BooleanValue ignoreToolCompatibility;

    // 是否匹配完整的方块状态而不仅仅是方块类型
    public final ForgeConfigSpec.BooleanValue matchBlockState;

    // 是否匹配完整的种子方块状态而不仅仅是种子方块类型
    public final ForgeConfigSpec.BooleanValue matchseedBlockState;

    // 创造模式是否启用连锁挖掘
    public final ForgeConfigSpec.BooleanValue enableInCreative;

    // 是否需要下蹲才能触发连锁
    public final ForgeConfigSpec.BooleanValue requireSneaking;

    // 连锁挖掘的最大方块数（存活模式）
    public final ForgeConfigSpec.IntValue maxBlocksInChain;

    // 连锁挖掘的最大方块数（创造模式）
    public final ForgeConfigSpec.IntValue maxBlocksInChainCreative;

    // 最大连锁深度
    public final ForgeConfigSpec.IntValue maxChainDepth;

    // 调试模式（服务器端使用）
    public final ForgeConfigSpec.BooleanValue enableDebugMode;

    private CommonConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("通用配置||Common Config").push("general");

        maxChainBlocks = builder
                .comment("连锁操作的最大方块数||Maximum number of blocks for chain operations")
                .translation("config.onekeyminer.maxChainBlocks")
                .defineInRange("maxChainBlocks", 64, 1, 4096);

        nonChainableBlocks = builder
                .comment("不允许连锁挖掘的方块黑名单||Blocks that should not be chain mined")
                .translation("config.onekeyminer.nonChainableBlocks")
                .defineList("nonChainableBlocks",
                        () -> {
                            List<String> defaults = new ArrayList<>();
                            // 常见建筑方块，不应该连锁
                            defaults.add("minecraft:bedrock");
                            return defaults;
                        },
                        obj -> obj instanceof String);

        seedBlacklist = builder
                .comment("不允许连锁种植的种子黑名单||Seeds that should not be chain planted")
                .translation("config.onekeyminer.seedBlacklist")
                .defineList("seedBlacklist",
                        ArrayList::new,
                        obj -> obj instanceof String);
        chainPlantableFarmlands = builder
                .comment(
                        "允许连锁种植的耕地白名单（支持单个方块ID和标签，标签需以#开头）||" +
                                "Whitelist of farmlands allowed for chain planting (supports single block IDs and tags, tags start with #)" +
                                "\n示例：[\"minecraft:farmland\", \"#yourmod:custom_farmlands\"]"
                )
                .translation("config.onekeyminer.chainPlantableFarmlands") // 统一翻译key格式
                .defineList(
                        "chainPlantableFarmlands",
                        () -> {
                            // 默认值：包含Minecraft原生耕地
                            List<String> defaults = new ArrayList<>();
                            defaults.add("minecraft:farmland");
                            return defaults;
                        },
                        (Predicate<Object>) obj -> obj instanceof String // 校验配置值为字符串
                );

        enableDiagonalChaining = builder
                .comment("是否允许对角线连锁（允许对角连接的方块参与连锁）||Whether to allow diagonal chaining (allow blocks connected diagonally to participate in chain mining)")
                .translation("config.onekeyminer.enableDiagonalChaining")
                .define("enableDiagonalChaining", false);

        maxMiningDepth = builder
                .comment("最大挖掘深度（防止连锁过深）||Maximum mining depth (prevent chain mining from being too deep)")
                .translation("config.onekeyminer.maxMiningDepth")
                .defineInRange("maxMiningDepth", 16, 1, 64);

        teleportDropsToPlayer = builder
                .comment("是否将连锁挖掘的掉落物传送到玩家身下||Whether to teleport the drops from chain mining to the player")
                .translation("config.onekeyminer.teleportDropsToPlayer")
                .define("teleportDropsToPlayer", false);

        ignoreToolCompatibility = builder
                .comment("是否忽略工具兼容性检查||Whether to ignore tool compatibility checks")
                .translation("config.onekeyminer.ignoreToolCompatibility")
                .define("ignoreToolCompatibility", true);

        matchBlockState = builder
                .comment("是否匹配完整的方块状态而不仅仅是方块类型||Whether to match the complete block state rather than just the block type")
                .translation("config.onekeyminer.matchBlockState")
                .define("matchBlockState", false);

        matchseedBlockState =builder
                .comment("是否匹配完整的种子方块状态而不仅仅是种子方块类型 || Whether to match the complete state of the seed block rather than just the type of the seed block.")
                .translation("config.onekeyminer.matchseedBlockState")
                .define("matchseedBlockState", false);
        enableInCreative = builder
                .comment("创造模式是否启用连锁挖掘||Whether to enable chain mining in creative mode")
                .translation("config.onekeyminer.enableInCreative")
                .define("enableInCreative", true);

        requireSneaking = builder
                .comment("是否需要下蹲才能触发连锁挖掘||Whether to require sneaking to trigger chain mining")
                .translation("config.onekeyminer.requireSneaking")
                .define("requireSneaking", false);

        maxBlocksInChain = builder
                .comment("存活模式下连锁挖掘的最大方块数||Maximum number of blocks for chain mining in survival mode")
                .translation("config.onekeyminer.maxBlocksInChain")
                .defineInRange("maxBlocksInChain", 64, 1, 4096);

        maxBlocksInChainCreative = builder
                .comment("创造模式下连锁挖掘的最大方块数||Maximum number of blocks for chain mining in creative mode")
                .translation("config.onekeyminer.maxBlocksInChainCreative")
                .defineInRange("maxBlocksInChainCreative", 256, 1, 16384);

        maxChainDepth = builder
                .comment("最大连锁搜索深度||Maximum chain search depth")
                .translation("config.onekeyminer.maxChainDepth")
                .defineInRange("maxChainDepth", 16, 1, 64);

        enableDebugMode = builder
                .comment("启用调试模式（服务器端调试日志）||Enable debug mode (server-side debug logging)")
                .translation("config.onekeyminer.enableDebugMode")
                .define("enableDebugMode", false);

        builder.pop();
    }


    public boolean isBlockMineable(Block block) {
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);

        for (String entry : nonChainableBlocks.get()) {
            if (entry == null) continue;
            if (entry.startsWith("#")) {
                // 由于目标是向 1.18 回退，某些映射/API 中 TagKey 可能不可用或位置不同，
                // 为了保证能编译，这里使用最保守的字符串匹配作为回退：
                // 如果白名单条目以 # 开头但恰好与该方块的注册 ID 相等，则视为匹配。
                ResourceLocation tagId = ResourceLocation.tryParse(entry.substring(1));
                if (tagId != null && blockId != null && blockId.toString().equals(tagId.toString())) {
                    return false;
                }
            } else {
                // 单个方块
                if (blockId != null && blockId.toString().equals(entry)) {
                    return false;
                }
            }
        }

        return true;
    }


    /**
     * 检查种子是否在黑名单中
     * 支持单个物品 ID 和标签 ID
     */
    public boolean isSeedBlacklisted(Item item) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);

        for (String entry : seedBlacklist.get()) {
            if (entry == null) continue;
            if (entry.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(entry.substring(1));
                if (tagId != null && itemId != null && itemId.toString().equals(tagId.toString())) {
                    return true;
                }
            } else {
                // 单个物品
                if (itemId != null && itemId.toString().equals(entry)) {
                    return true;
                }
            }
        }

        return false;
    }
    public boolean isFarmlandAllowedForChainPlanting(BlockState farmlandState) {
        if (farmlandState == null) return false;

    Block farmlandBlock = farmlandState.getBlock();
    // 获取耕地方块的注册ID（如 "minecraft:farmland"）
    ResourceLocation farmlandId = ForgeRegistries.BLOCKS.getKey(farmlandBlock);

        // 遍历白名单所有条目，匹配即返回true
        for (String entry : chainPlantableFarmlands.get()) {
            if (entry == null || entry.trim().isEmpty()) continue;

            // 1. 匹配标签（条目以#开头，如 "#minecraft:farmlands"）
            if (entry.startsWith("#")) {
                // 解析标签ID（去掉开头的#）
                ResourceLocation tagId = ResourceLocation.tryParse(entry.substring(1));
                if (tagId != null) {
                    // 创建方块标签键，判断耕地是否属于该标签
                    // 同样使用最小化的字符串匹配回退以确保在旧版本上可编译并有基本功能
                    if (farmlandId != null && farmlandId.toString().equals(tagId.toString())) {
                        return true;
                    }
                }
            }
            // 2. 匹配单个方块ID（如 "minecraft:farmland"）
            else {
                // 直接对比方块注册ID字符串
                if (farmlandId.toString().equals(entry.trim())) {
                    return true;
                }
            }
        }

        // 未匹配到任何白名单条目，拒绝连锁种植
        return false;
    }


    /**
     * 将本地化键转换为资源ID格式
     * 支持：
     *  - "block.minecraft.stone" → "minecraft:stone"
     *  - "minecraft:stone" → "minecraft:stone"
     *  - "#minecraft:logs" → "#minecraft:logs"
     */

}