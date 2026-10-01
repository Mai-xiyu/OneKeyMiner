package org.xiyu.yee.onekeyminer.chain;

import net.minecraft.util.EnumActionResult;

import net.minecraft.util.EnumActionResult;


import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.SoundCategory;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.block.state.IBlockState;
import org.xiyu.yee.onekeyminer.Onekeyminer;
import org.xiyu.yee.onekeyminer.capability.ChainModeCapability;
import org.xiyu.yee.onekeyminer.config.CommonConfig;
import org.xiyu.yee.onekeyminer.config.ServerConfig;
import org.xiyu.yee.onekeyminer.network.ChainActionPacket;
import org.xiyu.yee.onekeyminer.network.NetworkHandler;
import org.xiyu.onekeyminer.legacy.core.LegacyChainSearch;
import org.xiyu.onekeyminer.legacy.core.LegacyPosition;

import java.util.*;

import net.minecraft.init.Enchantments;

/**
 * 连锁挖掘处理器
 */
public class ChainHandler {
    // 防止重复挖掘的位置集合
    private static final Set<BlockPos> CURRENTLY_MINING = new HashSet<>();
    private static int XPS=0;
    /**
     * 连锁挖掘入口点 - 外部调用接口
     */
    public static boolean tryChainMine(EntityPlayerMP player, BlockPos pos, IBlockState state, ItemStack tool) {
        // 清理集合，避免上次操作残留
        CURRENTLY_MINING.clear();

        // 参数验证
        if ((player == null || pos == null || state == null || tool == null)) {
            Onekeyminer.LOGGER.debug("连锁挖掘参数无效");
            return false;
        }

        // 验证连锁挖掘条件
        if (!validateChainMiningConditions(player, pos, state, tool)) {
            return false;
        }

        // 所有条件验证通过，执行连锁挖掘
    String blockId = state.getBlock().getUnlocalizedName();
    String blockName = state.getBlock().getUnlocalizedName();

        // 执行连锁挖掘逻辑
        int blocksMined = performChainMining(player, pos, state, tool);

        // 发送反馈
        if (blocksMined > 0) {
            sendFeedback(player, blocksMined);
        }

        return blocksMined > 0;
    }

    /**
     * 计算连锁挖掘的所有位置，但不实际挖掘 - 提供给API调用
     */
    public static List<BlockPos> computeMiningPositions(EntityPlayerMP player, WorldServer level,
                                                        BlockPos startPos, IBlockState startState, ItemStack tool) {
        List<BlockPos> result = new ArrayList<>();
        if (player == null || level == null || startPos == null || startState == null || tool == null) {
            return result;
        }

        int maxBlocks = player.isCreative()
                ? CommonConfig.INSTANCE.maxBlocksInChainCreative.get()
                : CommonConfig.INSTANCE.maxBlocksInChain.get();
        int maxDepth = CommonConfig.INSTANCE.maxChainDepth.get();
        boolean diagonal = CommonConfig.INSTANCE.enableDiagonalChaining.get();
        boolean matchFullState = CommonConfig.INSTANCE.matchIBlockState.get();
        boolean matchSeedState = CommonConfig.INSTANCE.matchseedIBlockState.get();
        LegacyPosition start = new LegacyPosition(startPos.getX(), startPos.getY(), startPos.getZ());

        try {
            List<LegacyPosition> positions = LegacyChainSearch.findConnected(
                    start, maxBlocks, maxDepth, diagonal,
                    position -> isBlockMatching(startState,
                            level.getBlockState(new BlockPos(position.getX(), position.getY(), position.getZ())),
                            matchFullState, matchSeedState));
            for (LegacyPosition position : positions) {
                result.add(new BlockPos(position.getX(), position.getY(), position.getZ()));
            }
        } catch (RuntimeException error) {
            Onekeyminer.LOGGER.error("Failed to compute legacy chain positions", error);
        }
        return result;
    }

    private static List<BlockPos> computeMiningPositionsLegacy(EntityPlayerMP player, WorldServer level,
                                                        BlockPos startPos, IBlockState startState, ItemStack tool) {
        return computeMiningPositions(player, level, startPos, startState, tool);
    }

    /**
     * 验证是否满足连锁挖掘的条件
     */
    private static boolean validateChainMiningConditions(EntityPlayerMP player, BlockPos pos, IBlockState state, ItemStack tool) {
        // 检查连锁模式是否激活
        if (!ChainModeCapability.isChainModeActive(player)) {
            if(CommonConfig.INSTANCE.enableDebugMode.get()) {
                Onekeyminer.LOGGER.debug("连锁模式未启用");
            }
            return false;
        }

        // 检查方块是否为空气 (1.12)
        if (state.getBlock() == net.minecraft.init.Blocks.AIR) {
            return false;
        }

        // 检查蹲下需求
        if (CommonConfig.INSTANCE.requireSneaking.get() && !player.isSneaking()) {
            if(CommonConfig.INSTANCE.enableDebugMode.get()) {
                Onekeyminer.LOGGER.debug("需要下蹲但玩家未下蹲");
            }
            return false;
        }

        // 检查创造模式设置
        if (player.isCreative() && !CommonConfig.INSTANCE.enableInCreative.get()) {
            if(CommonConfig.INSTANCE.enableDebugMode.get()) {
                Onekeyminer.LOGGER.debug("创造模式下连锁挖掘未启用");
            }
            return false;
        }

        // 检查饥饿度
    if (!player.isCreative() &&
        player.getFoodStats().getFoodLevel() < ServerConfig.INSTANCE.hungerThreshold.get()) {
            if(CommonConfig.INSTANCE.enableDebugMode.get()) {
                Onekeyminer.LOGGER.debug("玩家饥饿度不足: {} < {}",
                        player.getFoodStats().getFoodLevel(), ServerConfig.INSTANCE.hungerThreshold.get());
            }
            return false;
        }

        // 检查方块是否可挖掘
        Block blockId = state.getBlock();
    String blockName = state.getBlock().getUnlocalizedName();
        if (!CommonConfig.INSTANCE.isBlockMineable(blockId)) {
            if(CommonConfig.INSTANCE.enableDebugMode.get()) {
                Onekeyminer.LOGGER.debug("方块在不可挖掘列表中: {} (内部ID: {})", blockName, blockId);
            }
            return false;
        }

        // 工具兼容性检查
    if (!CommonConfig.INSTANCE.ignoreToolCompatibility.get() &&
        !player.canHarvestBlock(state)) {
                if(CommonConfig.INSTANCE.enableDebugMode.get()) {
                Onekeyminer.LOGGER.debug("工具不兼容: {}", tool.getItem().getUnlocalizedName());
            }
            return false;
        }

        // 工具耐久度保护检查
        if (!player.isCreative() && (tool.isItemStackDamageable() || tool.getMaxDamage() > 0)) {
            Double durabilityProtection = ServerConfig.INSTANCE.toolDurabilityThreshold.get();
            if (tool.getItemDamage() >= tool.getMaxDamage() - durabilityProtection) {
                if(CommonConfig.INSTANCE.enableDebugMode.get()) {
                    Onekeyminer.LOGGER.debug("工具耐久度不足进行连锁挖掘");
                }
                return false;
            }
        }

        return true;
    }

    /**
     * 执行连锁挖掘的核心逻辑
     * @return 挖掘的方块数量
     */
    private static int performChainMining(EntityPlayerMP player, BlockPos startPos, IBlockState startState, ItemStack tool) {
    World level = player.world;  // 1.12: use player.world
    WorldServer serverLevel = (WorldServer) level;

        // The shared traversal is evaluated before any block is mutated.
    boolean matchFullState = CommonConfig.INSTANCE.matchIBlockState.get();
    boolean matchseedState = CommonConfig.INSTANCE.matchseedIBlockState.get();
        boolean teleportDrops = CommonConfig.INSTANCE.teleportDropsToPlayer.get();
        List<BlockPos> miningPositions = computeMiningPositions(player, serverLevel, startPos, startState, tool);

        int blocksMined = 0;
        XPS = 0; // 重置静态经验计数器

        for (BlockPos currentPos : miningPositions) {
            IBlockState currentState = level.getBlockState(currentPos);
            if (!isBlockMatching(startState, currentState, matchFullState, matchseedState)) {
                continue;
            }

            // 破坏方块（根据配置选择模式）
            boolean blockDestroyed;
            if (teleportDrops) {
                // TP模式：先让世界生成原版掉落，再把掉落移动到玩家处。
                blockDestroyed = breakBlockWithTeleport(serverLevel, currentPos, currentState, player, tool);
            } else {
                // World#destroyBlock does not fire the player break pipeline in
                // 1.12.2, so handle its experience output explicitly below.
                blockDestroyed = level.destroyBlock(currentPos, true);
                if (!blockDestroyed) continue;

                int experience = getExperienceDrop(currentState, level, currentPos, tool);
                if (experience > 0) {
                    currentState.getBlock().dropXpOnBlockBreak(level, currentPos, experience);
                }

                // 播放破坏音效（使用方块的声音）
                try {
                    level.playSound(null, currentPos, currentState.getBlock().getSoundType(currentState, level, currentPos, player).getBreakSound(), SoundCategory.BLOCKS, 1.0F, 1.0F);
                } catch (Throwable t) {
                    // 如果 API 不匹配则忽略
                }
            }

            if (blockDestroyed) {
                blocksMined++;
                if (!damageTool(player, tool)) {
                    break;
                }
            }
        }

        // TP模式下生成累积经验球（保持不变）
    if (teleportDrops && XPS > 0) {
        BlockPos dropPos = player.getPosition();
        EntityXPOrb orb = new EntityXPOrb(level,
            dropPos.getX() + 0.5,
            dropPos.getY() + 0.5,
            dropPos.getZ() + 0.5,
            XPS);
        try { level.spawnEntity(orb); } catch (Throwable t) { /* best-effort spawn */ }
        XPS = 0; // 重置
    }

        return blocksMined;
    }
    /**
     * 检查方块状态是否匹配
     */
    private static boolean isBlockMatching(IBlockState reference, IBlockState target, boolean matchFullState,boolean matchSeedState) {
        if (target.getBlock() == net.minecraft.init.Blocks.AIR) {
            return false;
        }

        // 完全匹配方块状态

        if ((matchFullState && !(target.getBlock() instanceof BlockCrops)||(matchSeedState && target.getBlock() instanceof BlockCrops))) {
            return reference.equals(target);
        }

        // 仅匹配方块类型，忽略方块状态（如朝向、阶段等）
        if (reference.getBlock() == target.getBlock()) {
            // 特殊处理：确保某些特殊方块的特定属性匹配
            // 例如，不同类型的木头应该分开连锁
            if (hasSpecialMatchingRules(reference.getBlock())) {
                return matchSpecialBlockProperties(reference, target);
            }
            return true;
        }

        return false;
    }

    private static int getExperienceDrop(IBlockState state, World level, BlockPos pos, ItemStack tool) {
        if (EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0) {
            return 0;
        }
        int fortune = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, tool);
        return state.getBlock().getExpDrop(state, level, pos, fortune);
    }

    private static boolean damageTool(EntityPlayerMP player, ItemStack tool) {
        if (player.isCreative() || tool == null || tool.isEmpty() || !tool.isItemStackDamageable()) {
            return true;
        }
        tool.damageItem(1, player);
        return !tool.isEmpty();
    }

    /**
     * 检查方块是否有特殊匹配规则
     */
    private static boolean hasSpecialMatchingRules(Block block) {
        // 这里可以添加需要特殊处理的方块类型
    String blockId = block.getUnlocalizedName();

        // 例如，特殊处理矿石、木头等
        return blockId.contains("ore") ||
                blockId.contains("log") ||
                blockId.contains("wood") ||
                blockId.contains("leaves");
    }

    /**
     * 匹配特殊方块的属性
     */
    private static boolean matchSpecialBlockProperties(IBlockState reference, IBlockState target) {
        // 对于矿石，应该允许不同朝向的相同矿石连锁
        if (reference.getBlock().getUnlocalizedName().contains("ore")) {
            return true;
        }

        // 对于木头，应该只连锁相同木材类型的木头
        if (reference.getBlock().getUnlocalizedName().contains("log") ||
                reference.getBlock().getUnlocalizedName().contains("wood")) {
            // 提取木材类型（如oak, spruce等）
            String refType = extractWoodType(reference.getBlock().getUnlocalizedName());
            String targetType = extractWoodType(target.getBlock().getUnlocalizedName());
            return refType.equals(targetType);
        }

        // 对于树叶，应该只连锁相同类型的树叶
        if (reference.getBlock().getUnlocalizedName().contains("leaves")) {
            String refType = extractWoodType(reference.getBlock().getUnlocalizedName());
            String targetType = extractWoodType(target.getBlock().getUnlocalizedName());
            return refType.equals(targetType);
        }

        // 默认情况下允许匹配
        return true;
    }

    /**
     * 从方块ID中提取木材类型
     */
    private static String extractWoodType(String blockId) {
        // 从类似 "block.minecraft.oak_log" 的ID中提取 "oak"
        for (String woodType : new String[]{"oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                "crimson", "warped", "mangrove", "cherry"}) {
            if (blockId.contains(woodType)) {
                return woodType;
            }
        }
        return blockId; // 如果找不到匹配的类型，返回原始ID
    }

    /**
     * 破坏方块并将掉落物传送到玩家位置
     */
    private static boolean breakBlockWithTeleport(WorldServer level, BlockPos pos, IBlockState state, EntityPlayerMP player, ItemStack tool) {
        if (level == null || player == null) return false;

    // 获取玩家位置作为掉落点
    BlockPos dropPos = player.getPosition();

        try {
            // 获取方块实体
            TileEntity blockEntity = level.getTileEntity(pos);

        // 简化：让世界直接处理掉落并在玩家位置播放音效；随后将附近掉落物传送到玩家位置
        boolean broken = level.destroyBlock(pos, true);
        if (!broken) return false;

        int experience = getExperienceDrop(state, level, pos, tool);
        if (experience > 0) {
            XPS += experience;
        }

        try {
            // 把附近掉落物移动到玩家位置
            net.minecraft.util.math.AxisAlignedBB box = new net.minecraft.util.math.AxisAlignedBB(pos);
            List<net.minecraft.entity.item.EntityItem> items = level.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class, box);
            for (net.minecraft.entity.item.EntityItem ei : items) {
                ei.setPosition(dropPos.getX() + 0.5, dropPos.getY() + 0.5, dropPos.getZ() + 0.5);
                ei.motionX = 0; ei.motionY = 0; ei.motionZ = 0;
            }
        } catch (Throwable t) {
            // 容错：如果 API 不同则忽略
        }

        return true;
        } catch (Exception e) {
            Onekeyminer.LOGGER.error("传送掉落物时出错: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 向玩家发送连锁挖掘反馈
     */
    private static void sendFeedback(EntityPlayerMP player, int blocksMined) {
        // 始终发送客户端通知包，让客户端根据其配置决定如何显示
        NetworkHandler.sendToPlayer(new ChainActionPacket("mining", blocksMined), player);
    }
} 
