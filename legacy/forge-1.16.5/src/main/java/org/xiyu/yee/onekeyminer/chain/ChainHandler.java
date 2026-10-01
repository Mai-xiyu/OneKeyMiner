package org.xiyu.yee.onekeyminer.chain;


import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.CropsBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.LootParameterSets;
import net.minecraft.util.math.vector.Vector3d;
import org.xiyu.yee.onekeyminer.Onekeyminer;
import org.xiyu.yee.onekeyminer.capability.ChainModeCapability;
import org.xiyu.yee.onekeyminer.config.CommonConfig;
import org.xiyu.yee.onekeyminer.config.ServerConfig;
import org.xiyu.yee.onekeyminer.network.ChainActionPacket;
import org.xiyu.yee.onekeyminer.network.NetworkHandler;
import org.xiyu.onekeyminer.legacy.core.LegacyChainSearch;
import org.xiyu.onekeyminer.legacy.core.LegacyPosition;

import java.util.*;

import static net.minecraft.enchantment.Enchantments.SILK_TOUCH;

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
    public static boolean tryChainMine(ServerPlayerEntity player, BlockPos pos, BlockState state, ItemStack tool) {
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
        String blockId = state.getBlock().getDescriptionId();
        String blockName = state.getBlock().getName().getString();

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
    public static List<BlockPos> computeMiningPositions(ServerPlayerEntity player, ServerWorld level,
                                                        BlockPos startPos, BlockState startState, ItemStack tool) {
        List<BlockPos> result = new ArrayList<>();
        if (player == null || level == null || startPos == null || startState == null || tool == null) {
            return result;
        }

        int maxBlocks = player.isCreative()
                ? CommonConfig.INSTANCE.maxBlocksInChainCreative.get()
                : CommonConfig.INSTANCE.maxBlocksInChain.get();
        int maxDepth = CommonConfig.INSTANCE.maxChainDepth.get();
        boolean diagonal = CommonConfig.INSTANCE.enableDiagonalChaining.get();
        boolean matchFullState = CommonConfig.INSTANCE.matchBlockState.get();
        boolean matchSeedState = CommonConfig.INSTANCE.matchseedBlockState.get();
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

    private static List<BlockPos> computeMiningPositionsLegacy(ServerPlayerEntity player, ServerWorld level,
                                                        BlockPos startPos, BlockState startState, ItemStack tool) {
        return computeMiningPositions(player, level, startPos, startState, tool);
    }

    /**
     * 验证是否满足连锁挖掘的条件
     */
    private static boolean validateChainMiningConditions(ServerPlayerEntity player, BlockPos pos, BlockState state, ItemStack tool) {
        // 检查连锁模式是否激活
        if (!ChainModeCapability.isChainModeActive(player)) {
            if(CommonConfig.INSTANCE.enableDebugMode.get()) {
                Onekeyminer.LOGGER.debug("连锁模式未启用");
            }
            return false;
        }

        // 检查方块是否为空气
        if (state.isAir()) {
            return false;
        }

        // 检查蹲下需求
        if (CommonConfig.INSTANCE.requireSneaking.get() && !player.isShiftKeyDown()) {
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
                player.getFoodData().getFoodLevel() < ServerConfig.INSTANCE.hungerThreshold.get()) {
            if(CommonConfig.INSTANCE.enableDebugMode.get()) {
                Onekeyminer.LOGGER.debug("玩家饥饿度不足: {} < {}",
                        player.getFoodData().getFoodLevel(), ServerConfig.INSTANCE.hungerThreshold.get());
            }
            return false;
        }

        // 检查方块是否可挖掘
        Block blockId = state.getBlock();
        String blockName = state.getBlock().getName().getString();
        if (!CommonConfig.INSTANCE.isBlockMineable(blockId)) {
            if(CommonConfig.INSTANCE.enableDebugMode.get()) {
                Onekeyminer.LOGGER.debug("方块在不可挖掘列表中: {} (内部ID: {})", blockName, blockId);
            }
            return false;
        }

        // 工具兼容性检查
        if (!CommonConfig.INSTANCE.ignoreToolCompatibility.get() &&
                !player.hasCorrectToolForDrops(state)) {
            if(CommonConfig.INSTANCE.enableDebugMode.get()) {
                Onekeyminer.LOGGER.debug("工具不兼容: {}", tool.getItem().getDescriptionId());
            }
            return false;
        }

        // 工具耐久度保护检查
        if (!player.isCreative() && tool.isDamageableItem()) {
            Double durabilityProtection = ServerConfig.INSTANCE.toolDurabilityThreshold.get();
            if (tool.getDamageValue() >= tool.getMaxDamage() - durabilityProtection) {
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
    private static int performChainMining(ServerPlayerEntity player, BlockPos startPos, BlockState startState, ItemStack tool) {
        World level = player.level;  // 1.18中直接用player.level获取世界
        ServerWorld serverLevel = (ServerWorld) level;  // 1.16中世界类为ServerWorld

        // The shared traversal is evaluated before any block is mutated.
        boolean matchFullState = CommonConfig.INSTANCE.matchBlockState.get();
        boolean matchseedState = CommonConfig.INSTANCE.matchseedBlockState.get();
        boolean teleportDrops = CommonConfig.INSTANCE.teleportDropsToPlayer.get();
        List<BlockPos> miningPositions = computeMiningPositions(player, serverLevel, startPos, startState, tool);

        int blocksMined = 0;
        XPS = 0; // 重置静态经验计数器

        for (BlockPos currentPos : miningPositions) {
            BlockState currentState = level.getBlockState(currentPos);
            if (!isBlockMatching(startState, currentState, matchFullState, matchseedState)) {
                continue;
            }

            // 破坏方块（根据配置选择模式）
            boolean blockDestroyed;
            if (teleportDrops) {
                // TP模式：使用适配1.16的breakBlockWithTeleport方法
                blockDestroyed = breakBlockWithTeleport(serverLevel, currentPos, currentState, player, tool);
            } else {
                // 非TP模式：适配1.16的战利品和经验逻辑
                TileEntity blockEntity = level.getBlockEntity(currentPos);

        // 1. 构建LootContext Builder（1.16.5）
        LootContext.Builder builder = new LootContext.Builder(serverLevel)
            .withParameter(LootParameters.ORIGIN, Vector3d.atCenterOf(currentPos))
            .withParameter(LootParameters.TOOL, tool)
            .withOptionalParameter(LootParameters.THIS_ENTITY, player)
            .withOptionalParameter(LootParameters.BLOCK_ENTITY, blockEntity);

        // 2. 获取受附魔影响的掉落物
        List<ItemStack> drops = currentState.getDrops(builder);

                // 3. 检查精准采集（1.16中附魔常量为Enchantments.SILK_TOUCH）
                boolean hasSilkTouch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0;

                // 4. 计算经验（1.16使用getExpDrop）
                int xp = 0;
                int fortuneLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, tool); // 时运等级
                xp = currentState.getBlock().getExpDrop(currentState, (net.minecraft.world.IWorldReader) level, currentPos, fortuneLevel, hasSilkTouch ? 1 : 0);

                // 5. 破坏方块（禁用原版掉落）
                blockDestroyed = serverLevel.destroyBlock(currentPos, false, player);
                if (!blockDestroyed) continue;

                // 6. 在原位置生成掉落物
                for (ItemStack itemStack : drops) {
                    if (!itemStack.isEmpty()) {
                        Block.popResource(level, currentPos, itemStack);
                    }
                }

                // 7. 生成经验球
                if (xp > 0) {
                    ExperienceOrbEntity orb = new ExperienceOrbEntity(level,
                            currentPos.getX() + 0.5,
                            currentPos.getY() + 0.5,
                            currentPos.getZ() + 0.5,
                            xp);
                    level.addFreshEntity(orb);
                }

                // 8. 播放破坏音效
                level.playSound(null, currentPos, currentState.getSoundType().getBreakSound(), SoundCategory.BLOCKS, 1.0F, 1.0F);

                // 9. 消耗工具耐久（1.18中无getEquipmentSlot()，直接指定主手槽位）
                if (!player.isCreative() && tool.isDamageableItem()) {
                    tool.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(net.minecraft.inventory.EquipmentSlotType.MAINHAND));
                    if (tool.isEmpty()) {
                        break;
                    }
                }
            }

            if (blockDestroyed) {
                blocksMined++;
                // Teleport-drop mode damages the tool inside its adapter too.
                // Stop before attempting another block once that stack breaks.
                if (!player.isCreative() && tool.isEmpty()) {
                    break;
                }
            }
        }

        // TP模式下生成累积经验球（保持不变）
        if (teleportDrops && XPS > 0) {
            BlockPos dropPos = player.blockPosition();
            ExperienceOrbEntity orb = new ExperienceOrbEntity(level,
                    dropPos.getX() + 0.5,
                    dropPos.getY() + 0.5,
                    dropPos.getZ() + 0.5,
                    XPS);
            level.addFreshEntity(orb);
            XPS = 0; // 重置
        }

        return blocksMined;
    }
    /**
     * 检查方块状态是否匹配
     */
    private static boolean isBlockMatching(BlockState reference, BlockState target, boolean matchFullState,boolean matchSeedState) {
        if (target.isAir()) {
            return false;
        }

        // 完全匹配方块状态

        if ((matchFullState && !(target.getBlock() instanceof CropsBlock)||(matchSeedState && target.getBlock() instanceof CropsBlock))) {
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

    /**
     * 检查方块是否有特殊匹配规则
     */
    private static boolean hasSpecialMatchingRules(Block block) {
        // 这里可以添加需要特殊处理的方块类型
        String blockId = block.getDescriptionId();

        // 例如，特殊处理矿石、木头等
        return blockId.contains("ore") ||
                blockId.contains("log") ||
                blockId.contains("wood") ||
                blockId.contains("leaves");
    }

    /**
     * 匹配特殊方块的属性
     */
    private static boolean matchSpecialBlockProperties(BlockState reference, BlockState target) {
        // 对于矿石，应该允许不同朝向的相同矿石连锁
        if (reference.getBlock().getDescriptionId().contains("ore")) {
            return true;
        }

        // 对于木头，应该只连锁相同木材类型的木头
        if (reference.getBlock().getDescriptionId().contains("log") ||
                reference.getBlock().getDescriptionId().contains("wood")) {
            // 提取木材类型（如oak, spruce等）
            String refType = extractWoodType(reference.getBlock().getDescriptionId());
            String targetType = extractWoodType(target.getBlock().getDescriptionId());
            return refType.equals(targetType);
        }

        // 对于树叶，应该只连锁相同类型的树叶
        if (reference.getBlock().getDescriptionId().contains("leaves")) {
            String refType = extractWoodType(reference.getBlock().getDescriptionId());
            String targetType = extractWoodType(target.getBlock().getDescriptionId());
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
    private static boolean breakBlockWithTeleport(ServerWorld level, BlockPos pos, BlockState state, ServerPlayerEntity player, ItemStack tool) {
        if (level == null || player == null) return false;

        // 获取玩家位置作为掉落点
        BlockPos dropPos = player.blockPosition();

        try {
            // 获取方块实体
            TileEntity blockEntity = level.getBlockEntity(pos);

        // 创建战利品上下文 Builder（1.16.5）
        LootContext.Builder builder = new LootContext.Builder(level)
            .withParameter(LootParameters.ORIGIN, Vector3d.atCenterOf(pos))
            .withParameter(LootParameters.TOOL, tool)
            .withOptionalParameter(LootParameters.THIS_ENTITY, player)
            .withOptionalParameter(LootParameters.BLOCK_ENTITY, blockEntity);

        // 获取方块的掉落物
        List<ItemStack> drops = state.getDrops(builder);

            // 检查是否有精准采集附魔
            boolean hasSilkTouch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0;

            // 获取方块的经验值（1.16）
            int xp = 0;
            int fortuneLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, tool); // 时运等级
            xp = state.getBlock().getExpDrop(state, (net.minecraft.world.IWorldReader) level, pos, fortuneLevel, hasSilkTouch ? 1 : 0);

            // 触发方块破坏事件
            boolean broken = level.destroyBlock(pos, false, player);
            if (!broken) return false;

            // 在玩家位置生成掉落物
            for (ItemStack itemStack : drops) {
                if (!itemStack.isEmpty()) {
                    Block.popResource(level, dropPos, itemStack);
                }
            }

            // 在玩家位置生成经验球（保持原逻辑）
            if (xp > 0 && !hasSilkTouch) {
                XPS += xp; // 假设XPS是你的自定义经验累计变量
            }

            // 播放方块破坏音效
            level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundCategory.BLOCKS, 1.0F, 1.0F);

            // 消耗工具耐久度（1.18中getEquipmentSlot()需改为直接指定装备槽）
            if (!player.isCreative() && tool.isDamageableItem()) {
                tool.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(EquipmentSlotType.MAINHAND));

                // 如果工具损坏，返回成功
                if (tool.isEmpty()) {
                    return true;
                }
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
    private static void sendFeedback(ServerPlayerEntity player, int blocksMined) {
        // 始终发送客户端通知包，让客户端根据其配置决定如何显示
        NetworkHandler.sendToPlayer(new ChainActionPacket("mining", blocksMined), player);
    }
} 
