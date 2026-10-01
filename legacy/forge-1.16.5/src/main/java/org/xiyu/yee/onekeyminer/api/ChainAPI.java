package org.xiyu.yee.onekeyminer.api;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.CropsBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.BlockState;
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

    public static List<BlockPos> computeMiningPositions(ServerPlayerEntity player, ServerWorld level, BlockPos startPos, BlockState startState, ItemStack tool) {
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

    public static List<BlockPos> computeInteractionPositions(ServerPlayerEntity player, ServerWorld level, BlockPos startPos, BlockState targetState, Hand hand) {
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

    public static List<BlockPos> computePlantingPositions(ServerPlayerEntity player, ServerWorld level, BlockPos startPos, BlockState targetState, ItemStack seedItem) {
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
    private static boolean canPlantAt(World level, BlockPos pos, BlockState state, ItemStack seedItem) {
        if (!level.isEmptyBlock(pos)) return false;
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        return canPlantOnSurface(seedItem.getItem(), belowState);
    }

    private static boolean canPlantOnSurface(Item item, BlockState surfaceState) {
        if (surfaceState == null) return false;
        Block surfaceBlock = surfaceState.getBlock();
        if (item instanceof BlockItem) {
            BlockItem blockItem = (BlockItem) item;
            Block block = blockItem.getBlock();
            if (block instanceof CropsBlock) return surfaceBlock == net.minecraft.block.Blocks.FARMLAND;
            if (block instanceof net.minecraft.block.SaplingBlock || block instanceof net.minecraft.block.FlowerBlock) {
                return surfaceBlock == net.minecraft.block.Blocks.DIRT || surfaceBlock == net.minecraft.block.Blocks.GRASS_BLOCK || surfaceBlock == net.minecraft.block.Blocks.PODZOL || surfaceBlock == net.minecraft.block.Blocks.MYCELIUM;
            }
            if (block instanceof net.minecraft.block.MushroomBlock) return surfaceBlock == net.minecraft.block.Blocks.MYCELIUM;
            if (item == net.minecraft.item.Items.POTATO || item == net.minecraft.item.Items.CARROT || item == net.minecraft.item.Items.WHEAT_SEEDS || item == net.minecraft.item.Items.BEETROOT_SEEDS) {
                return surfaceBlock == net.minecraft.block.Blocks.FARMLAND;
            }
        }
        return surfaceBlock == net.minecraft.block.Blocks.DIRT || surfaceBlock == net.minecraft.block.Blocks.GRASS_BLOCK || surfaceBlock == net.minecraft.block.Blocks.PODZOL || surfaceBlock == net.minecraft.block.Blocks.SAND || surfaceBlock == net.minecraft.block.Blocks.SOUL_SAND || surfaceBlock == net.minecraft.block.Blocks.MYCELIUM || surfaceBlock instanceof FarmlandBlock;
    }

    private static boolean isPlantableItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        if (CommonConfig.INSTANCE.isSeedBlacklisted(item)) return false;
        if (item instanceof BlockItem) {
            BlockItem blockItem = (BlockItem) item;
            Block block = blockItem.getBlock();
            if (block instanceof CropsBlock || block instanceof net.minecraft.block.SaplingBlock || block instanceof net.minecraft.block.BushBlock || block instanceof net.minecraft.block.FlowerBlock || block instanceof net.minecraft.block.TallFlowerBlock || block instanceof net.minecraft.block.CactusBlock || block instanceof net.minecraft.block.TallGrassBlock || block instanceof net.minecraft.block.FungusBlock || block instanceof net.minecraft.block.NetherSproutsBlock || block instanceof net.minecraft.block.CocoaBlock || block instanceof net.minecraft.block.SugarCaneBlock || block instanceof net.minecraft.block.NetherWartBlock) {
                return true;
            }
        }
        String name = item.toString().toLowerCase();
        if (name.contains("seed") || name.contains("sapling") || name.contains("seedling") || name.contains("plant")) return true;
    return item == net.minecraft.item.Items.WHEAT_SEEDS || item == net.minecraft.item.Items.BEETROOT_SEEDS || item == net.minecraft.item.Items.PUMPKIN_SEEDS || item == net.minecraft.item.Items.MELON_SEEDS || item == net.minecraft.item.Items.POTATO || item == net.minecraft.item.Items.CARROT || item == net.minecraft.item.Items.SWEET_BERRIES || item == net.minecraft.item.Items.CRIMSON_FUNGUS || item == net.minecraft.item.Items.WARPED_FUNGUS || item == net.minecraft.item.Items.COCOA_BEANS || item == net.minecraft.item.Items.KELP || item == net.minecraft.item.Items.BAMBOO || item == net.minecraft.item.Items.SUGAR_CANE || item == net.minecraft.item.Items.CACTUS || item == net.minecraft.item.Items.SEA_PICKLE;
    }
}
