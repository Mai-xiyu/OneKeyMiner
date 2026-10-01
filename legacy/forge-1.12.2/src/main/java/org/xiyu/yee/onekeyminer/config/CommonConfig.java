package org.xiyu.yee.onekeyminer.config;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommonConfig {
    public static final CommonConfig INSTANCE = new CommonConfig();
    public static final Object SPEC = null;
    
    private static Configuration config;
    
    // 所有配置字段
    public final ConfigValue<List<String>> chainPlantableFarmlands = new ConfigValue<>();
    public final IntValue maxChainBlocks = new IntValue();
    public final ConfigValue<List<String>> nonChainableBlocks = new ConfigValue<>();
    public final ConfigValue<List<String>> seedBlacklist = new ConfigValue<>();
    public final BooleanValue enableDiagonalChaining = new BooleanValue();
    public final IntValue maxMiningDepth = new IntValue();
    public final BooleanValue teleportDropsToPlayer = new BooleanValue();
    public final BooleanValue ignoreToolCompatibility = new BooleanValue();
    public final BooleanValue matchIBlockState = new BooleanValue();
    public final BooleanValue matchseedIBlockState = new BooleanValue();
    public final BooleanValue enableInCreative = new BooleanValue();
    public final BooleanValue requireSneaking = new BooleanValue();
    public final IntValue maxBlocksInChain = new IntValue();
    public final IntValue maxBlocksInChainCreative = new IntValue();
    public final IntValue maxChainDepth = new IntValue();
    public final BooleanValue enableDebugMode = new BooleanValue();
    
    public static void init(File configFile) {
        config = new Configuration(configFile);
        config.load();
        
        String category = "general";
        
        INSTANCE.maxChainBlocks.value = config.getInt("maxChainBlocks", category, 64, 1, 4096,
            "连锁操作的最大方块数||Maximum number of blocks for chain operations");
        
        String[] defaultBlacklist = {"minecraft:bedrock"};
        INSTANCE.nonChainableBlocks.value = Arrays.asList(config.getStringList("nonChainableBlocks", category, defaultBlacklist,
            "不允许连锁挖掘的方块黑名单||Blocks that should not be chain mined"));
        
        String[] defaultSeedBlacklist = {};
        INSTANCE.seedBlacklist.value = Arrays.asList(config.getStringList("seedBlacklist", category, defaultSeedBlacklist,
            "不允许连锁种植的种子黑名单||Seeds that should not be chain planted"));
        
        String[] defaultFarmlands = {"minecraft:farmland"};
        INSTANCE.chainPlantableFarmlands.value = Arrays.asList(config.getStringList("chainPlantableFarmlands", category, defaultFarmlands,
            "允许连锁种植的耕地白名单"));
        
        INSTANCE.enableDiagonalChaining.value = config.getBoolean("enableDiagonalChaining", category, false,
            "是否允许对角线连锁");
        
        INSTANCE.maxMiningDepth.value = config.getInt("maxMiningDepth", category, 16, 1, 64,
            "最大挖掘深度");
        
        INSTANCE.teleportDropsToPlayer.value = config.getBoolean("teleportDropsToPlayer", category, false,
            "是否将连锁挖掘的掉落物传送到玩家身下");
        
        INSTANCE.ignoreToolCompatibility.value = config.getBoolean("ignoreToolCompatibility", category, true,
            "是否忽略工具兼容性检查");
        
        INSTANCE.matchIBlockState.value = config.getBoolean("matchIBlockState", category, false,
            "是否匹配完整的方块状态");
        
        INSTANCE.matchseedIBlockState.value = config.getBoolean("matchseedIBlockState", category, false,
            "是否匹配完整的种子方块状态");
        
        INSTANCE.enableInCreative.value = config.getBoolean("enableInCreative", category, true,
            "创造模式是否启用连锁挖掘");
        
        INSTANCE.requireSneaking.value = config.getBoolean("requireSneaking", category, false,
            "是否需要下蹲才能触发连锁挖掘");
        
        INSTANCE.maxBlocksInChain.value = config.getInt("maxBlocksInChain", category, 64, 1, 4096,
            "存活模式下连锁挖掘的最大方块数");
        
        INSTANCE.maxBlocksInChainCreative.value = config.getInt("maxBlocksInChainCreative", category, 256, 1, 16384,
            "创造模式下连锁挖掘的最大方块数");
        
        INSTANCE.maxChainDepth.value = config.getInt("maxChainDepth", category, 16, 1, 64,
            "最大连锁搜索深度");
        
        INSTANCE.enableDebugMode.value = config.getBoolean("enableDebugMode", category, false,
            "启用调试模式");
        
        if (config.hasChanged()) {
            config.save();
        }
    }
    
    public static void save() {
        if (config != null && config.hasChanged()) {
            config.save();
        }
    }
    
    public boolean isBlockMineable(Block block) {
        ResourceLocation blockId = block.getRegistryName();
        for (String entry : nonChainableBlocks.get()) {
            if (entry == null) continue;
            if (entry.startsWith("#")) {
                ResourceLocation tagId = new ResourceLocation(entry.substring(1));
                if (tagId != null && blockId != null && blockId.toString().equals(tagId.toString())) {
                    return false;
                }
            } else {
                if (blockId != null && blockId.toString().equals(entry)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public boolean isSeedBlacklisted(Item item) {
        ResourceLocation itemId = item.getRegistryName();
        for (String entry : seedBlacklist.get()) {
            if (entry == null) continue;
            if (entry.startsWith("#")) {
                ResourceLocation tagId = new ResourceLocation(entry.substring(1));
                if (tagId != null && itemId != null && itemId.toString().equals(tagId.toString())) {
                    return true;
                }
            } else {
                if (itemId != null && itemId.toString().equals(entry)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean isFarmlandAllowedForChainPlanting(IBlockState farmlandState) {
        if (farmlandState == null) return false;
        Block farmlandBlock = farmlandState.getBlock();
        ResourceLocation farmlandId = farmlandBlock.getRegistryName();
        
        for (String entry : chainPlantableFarmlands.get()) {
            if (entry == null || entry.trim().isEmpty()) continue;
            if (entry.startsWith("#")) {
                ResourceLocation tagId = new ResourceLocation(entry.substring(1));
                if (tagId != null && farmlandId != null && farmlandId.toString().equals(tagId.toString())) {
                    return true;
                }
            } else {
                if (farmlandId.toString().equals(entry.trim())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    // 包装类
    public static class BooleanValue {
        private boolean value;
        public boolean get() { return value; }
        public void set(boolean value) { this.value = value; }
    }
    
    public static class IntValue {
        private int value;
        public int get() { return value; }
        public void set(int value) { this.value = value; }
    }
    
    public static Configuration getConfig() {
        return config;
    }
    
    public static class ConfigValue<T> {
        private T value;
        public T get() { return value; }
        public void set(T value) { this.value = value; }
    }
}
