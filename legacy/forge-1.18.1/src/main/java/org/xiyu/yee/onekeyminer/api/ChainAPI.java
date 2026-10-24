package org.xiyu.yee.onekeyminer.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.yee.onekeyminer.capability.ChainModeCapability;
import org.xiyu.yee.onekeyminer.config.CommonConfig;
import org.xiyu.yee.onekeyminer.config.ServerConfig;
import org.xiyu.yee.onekeyminer.chain.ChainHandler;
import org.xiyu.yee.onekeyminer.chain.PlantingHandler;
import org.xiyu.yee.onekeyminer.chain.InteractionHandler;

import java.util.*;

/**
 * 提供连锁计算的公共 API（仅计算 BlockPos 列表，不执行实际破坏/交互）
 */
public class ChainAPI {

    public static org.xiyu.onekeyminer.legacy.core.LegacyHudSettings getPreviewHudSettings() {
        return org.xiyu.onekeyminer.legacy.core.LegacyHudPreferences.get(hudConfigPath());
    }

    public static void setPreviewHudSettings(org.xiyu.onekeyminer.legacy.core.LegacyHudSettings settings) {
        org.xiyu.onekeyminer.legacy.core.LegacyHudPreferences.set(hudConfigPath(), settings);
    }

    public static void resetPreviewHudSettings() {
        setPreviewHudSettings(org.xiyu.onekeyminer.legacy.core.LegacyHudSettings.defaults());
    }

    private static java.nio.file.Path hudConfigPath() {
        return net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("onekeyminer-hud.properties");
    }

    public static List<BlockPos> computeMiningPositions(ServerPlayer player, ServerLevel level, BlockPos startPos, BlockState startState, ItemStack tool) {
        List<BlockPos> result = new ArrayList<>();
        if (player == null || level == null || startPos == null || startState == null || tool == null) return result;

        // 基础条件检查
        if (!ChainModeCapability.isChainModeActive(player)) return result;
        if (startState.isAir()) return result;
        if (CommonConfig.INSTANCE.requireSneaking.get() && !player.isShiftKeyDown()) return result;
        if (player.isCreative() && !CommonConfig.INSTANCE.enableInCreative.get()) return result;
        if (!player.isCreative() && player.getFoodData().getFoodLevel() < ServerConfig.INSTANCE.hungerThreshold.get()) return result;
        
        Block block = startState.getBlock();
        if (!CommonConfig.INSTANCE.isBlockMineable(block)) return result;
        if (!CommonConfig.INSTANCE.ignoreToolCompatibility.get() && !player.hasCorrectToolForDrops(startState)) return result;
        
        if (!player.isCreative() && tool.isDamageableItem()) {
            double durabilityProtection = ServerConfig.INSTANCE.toolDurabilityThreshold.get();
            if (tool.getDamageValue() >= tool.getMaxDamage() - durabilityProtection) return result;
        }

        try {
            // 调用 ChainHandler 的计算逻辑，但不实际挖掘
            result = ChainHandler.computeMiningPositions(player, level, startPos, startState, tool);
        } catch (Throwable t) {
            // 如果调用失败，使用备用的简化逻辑
            if (level.getBlockState(startPos).getBlock() == startState.getBlock()) {
                result.add(startPos);
            }
            
            // 检查直接相邻的6个位置
            BlockPos[] neighbors = {
                startPos.above(), startPos.below(),
                startPos.north(), startPos.south(),
                startPos.east(), startPos.west()
            };
            
            for (BlockPos neighbor : neighbors) {
                if (result.size() >= 10) break; // 限制数量
                if (level.getBlockState(neighbor).getBlock() == startState.getBlock()) {
                    result.add(neighbor);
                }
            }
        }

        return result;
    }

    public static List<BlockPos> computeInteractionPositions(ServerPlayer player, ServerLevel level, BlockPos startPos, BlockState targetState, InteractionHand hand) {
        List<BlockPos> result = new ArrayList<>();
        if (player == null || level == null || startPos == null || targetState == null) return result;

        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.isEmpty()) return result;
        
        // 遵循原始条件：工具耐久检查
        if (heldItem.isDamageableItem() && heldItem.getDamageValue() >= heldItem.getMaxDamage() - 1) return result;

        try {
            // 调用 InteractionHandler 的计算逻辑，但不实际执行交互
            result = InteractionHandler.computeInteractionPositions(player, level, startPos, targetState, hand);
        } catch (Throwable t) {
            // 如果调用失败，使用备用的简化逻辑
            if (level.getBlockState(startPos).getBlock() == targetState.getBlock()) {
                result.add(startPos);
            }
            
            // 检查直接相邻的6个位置
            BlockPos[] neighbors = {
                startPos.above(), startPos.below(),
                startPos.north(), startPos.south(),
                startPos.east(), startPos.west()
            };
            
            for (BlockPos neighbor : neighbors) {
                if (result.size() >= 10) break; // 限制数量
                if (level.getBlockState(neighbor).getBlock() == targetState.getBlock()) {
                    result.add(neighbor);
                }
            }
        }

        return result;
    }

    public static List<BlockPos> computePlantingPositions(ServerPlayer player, ServerLevel level, BlockPos startPos, BlockState targetState, ItemStack seedItem) {
        List<BlockPos> result = new ArrayList<>();
        if (player == null || level == null || startPos == null || seedItem == null) return result;

        // 直接调用 PlantingHandler 的计算逻辑，但只计算不实际种植
        try {
            result = PlantingHandler.computePlantingPositions(player, level, startPos, targetState, seedItem);
        } catch (Throwable t) {
            // 如果调用失败，使用备用的简化逻辑
            if (!isPlantableItem(seedItem)) return result;
            
            // 简化逻辑：只检查起始位置和直接相邻位置
            if (canPlantAt(level, startPos, level.getBlockState(startPos), seedItem)) {
                result.add(startPos);
            }
            
            // 检查直接相邻的6个位置
            BlockPos[] neighbors = {
                startPos.above(), startPos.below(),
                startPos.north(), startPos.south(),
                startPos.east(), startPos.west()
            };
            
            for (BlockPos neighbor : neighbors) {
                if (result.size() >= 10) break; // 限制数量防止过多
                if (canPlantAt(level, neighbor, level.getBlockState(neighbor), seedItem)) {
                    result.add(neighbor);
                }
            }
        }

        return result;
    }

    // 种植相关的备用方法
    private static boolean canPlantAt(Level level, BlockPos pos, BlockState state, ItemStack seedItem) {
        if (!level.isEmptyBlock(pos)) return false;
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        return canPlantOnSurface(seedItem.getItem(), belowState);
    }

    private static boolean canPlantOnSurface(Item item, BlockState surfaceState) {
        if (surfaceState == null) return false;
        Block surfaceBlock = surfaceState.getBlock();
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof CropBlock) return surfaceBlock == net.minecraft.world.level.block.Blocks.FARMLAND;
            if (block instanceof net.minecraft.world.level.block.SaplingBlock || block instanceof net.minecraft.world.level.block.FlowerBlock) {
                return surfaceBlock == net.minecraft.world.level.block.Blocks.DIRT || surfaceBlock == net.minecraft.world.level.block.Blocks.GRASS_BLOCK || surfaceBlock == net.minecraft.world.level.block.Blocks.PODZOL || surfaceBlock == net.minecraft.world.level.block.Blocks.MYCELIUM;
            }
            if (block instanceof net.minecraft.world.level.block.MushroomBlock) return surfaceBlock == net.minecraft.world.level.block.Blocks.MYCELIUM;
            if (item == net.minecraft.world.item.Items.POTATO || item == net.minecraft.world.item.Items.CARROT || item == net.minecraft.world.item.Items.WHEAT_SEEDS || item == net.minecraft.world.item.Items.BEETROOT_SEEDS) {
                return surfaceBlock == net.minecraft.world.level.block.Blocks.FARMLAND;
            }
        }
        return surfaceBlock == net.minecraft.world.level.block.Blocks.DIRT || surfaceBlock == net.minecraft.world.level.block.Blocks.GRASS_BLOCK || surfaceBlock == net.minecraft.world.level.block.Blocks.PODZOL || surfaceBlock == net.minecraft.world.level.block.Blocks.SAND || surfaceBlock == net.minecraft.world.level.block.Blocks.SOUL_SAND || surfaceBlock == net.minecraft.world.level.block.Blocks.MYCELIUM || surfaceBlock instanceof FarmBlock;
    }

    private static boolean isPlantableItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        if (CommonConfig.INSTANCE.isSeedBlacklisted(item)) return false;
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof CropBlock || block instanceof net.minecraft.world.level.block.SaplingBlock || block instanceof net.minecraft.world.level.block.BushBlock || block instanceof net.minecraft.world.level.block.FlowerBlock || block instanceof net.minecraft.world.level.block.TallFlowerBlock || block instanceof net.minecraft.world.level.block.CactusBlock || block instanceof net.minecraft.world.level.block.TallGrassBlock || block instanceof net.minecraft.world.level.block.FungusBlock || block instanceof net.minecraft.world.level.block.RootsBlock || block instanceof net.minecraft.world.level.block.NetherSproutsBlock || block instanceof net.minecraft.world.level.block.CocoaBlock || block instanceof net.minecraft.world.level.block.SugarCaneBlock || block instanceof net.minecraft.world.level.block.NetherWartBlock || block instanceof net.minecraft.world.level.block.AzaleaBlock) {
                return true;
            }
        }
        String name = item.toString().toLowerCase();
        if (name.contains("seed") || name.contains("sapling") || name.contains("seedling") || name.contains("plant")) return true;
    return item == net.minecraft.world.item.Items.WHEAT_SEEDS || item == net.minecraft.world.item.Items.BEETROOT_SEEDS || item == net.minecraft.world.item.Items.PUMPKIN_SEEDS || item == net.minecraft.world.item.Items.MELON_SEEDS || item == net.minecraft.world.item.Items.POTATO || item == net.minecraft.world.item.Items.CARROT || item == net.minecraft.world.item.Items.SWEET_BERRIES || item == net.minecraft.world.item.Items.GLOW_BERRIES || item == net.minecraft.world.item.Items.CRIMSON_FUNGUS || item == net.minecraft.world.item.Items.WARPED_FUNGUS || item == net.minecraft.world.item.Items.COCOA_BEANS || item == net.minecraft.world.item.Items.KELP || item == net.minecraft.world.item.Items.BAMBOO || item == net.minecraft.world.item.Items.SUGAR_CANE || item == net.minecraft.world.item.Items.CACTUS || item == net.minecraft.world.item.Items.SEA_PICKLE;
    }
}
