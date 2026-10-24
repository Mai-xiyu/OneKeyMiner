package org.xiyu.yee.onekeyminer.api;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.WorldServer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumHand;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.state.IBlockState;
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
        return net.minecraftforge.fml.common.Loader.instance().getConfigDir().toPath().resolve("onekeyminer-hud.properties");
    }

    public static List<BlockPos> computeMiningPositions(EntityPlayerMP player, WorldServer level, BlockPos startPos, IBlockState startState, ItemStack tool) {
        List<BlockPos> result = new ArrayList<>();
        if (player == null || level == null || startPos == null || startState == null || tool == null) return result;

        // 基础条件检查
        if (!ChainModeCapability.isChainModeActive(player)) return result;
    if (startState.getBlock() == net.minecraft.init.Blocks.AIR) return result;
    if (CommonConfig.INSTANCE.requireSneaking.get() && !player.isSneaking()) return result;
    if (player.isCreative() && !CommonConfig.INSTANCE.enableInCreative.get()) return result;
    if (!player.isCreative() && player.getFoodStats().getFoodLevel() < ServerConfig.INSTANCE.hungerThreshold.get()) return result;
        
        Block block = startState.getBlock();
        if (!CommonConfig.INSTANCE.isBlockMineable(block)) return result;
    if (!CommonConfig.INSTANCE.ignoreToolCompatibility.get() && !player.canHarvestBlock(startState)) return result;
        
        if (!player.isCreative() && (tool.isItemStackDamageable() || tool.getMaxDamage() > 0)) {
            double durabilityProtection = ServerConfig.INSTANCE.toolDurabilityThreshold.get();
            if (tool.getItemDamage() >= tool.getMaxDamage() - durabilityProtection) return result;
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
                startPos.up(), startPos.down(),
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

    public static List<BlockPos> computeInteractionPositions(EntityPlayerMP player, WorldServer level, BlockPos startPos, IBlockState targetState, EnumHand hand) {
        List<BlockPos> result = new ArrayList<>();
        if (player == null || level == null || startPos == null || targetState == null) return result;

        ItemStack heldItem = player.getHeldItem(hand);
    if (heldItem.isEmpty()) return result;
        
        // 遵循原始条件：工具耐久检查
    if ((heldItem.isItemStackDamageable() || heldItem.getMaxDamage() > 0) && heldItem.getItemDamage() >= heldItem.getMaxDamage() - 1) return result;

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
                startPos.up(), startPos.down(),
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

    public static List<BlockPos> computePlantingPositions(EntityPlayerMP player, WorldServer level, BlockPos startPos, IBlockState targetState, ItemStack seedItem) {
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
                startPos.up(), startPos.down(),
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
    private static boolean canPlantAt(World level, BlockPos pos, IBlockState state, ItemStack seedItem) {
        if (!level.isAirBlock(pos)) return false;
        BlockPos below = pos.down();
        IBlockState belowState = level.getBlockState(below);
        return canPlantOnSurface(seedItem.getItem(), belowState);
    }

    private static boolean canPlantOnSurface(Item item, IBlockState surfaceState) {
        if (surfaceState == null) return false;
        Block surfaceBlock = surfaceState.getBlock();
        if (item instanceof ItemBlock) {
            ItemBlock blockItem = (ItemBlock) item;
            Block block = blockItem.getBlock();
            // 仅处理 1.12 常见的作物与树苗情况
            if (block instanceof BlockCrops) return surfaceBlock == net.minecraft.init.Blocks.FARMLAND;
            String bname = block.getUnlocalizedName().toLowerCase();
            if (bname.contains("sapling") || bname.contains("flower") || bname.contains("mushroom")) {
                return surfaceBlock == net.minecraft.init.Blocks.DIRT || surfaceBlock == net.minecraft.init.Blocks.GRASS || surfaceBlock == net.minecraft.init.Blocks.MYCELIUM;
            }
            if (bname.contains("potato") || bname.contains("carrot") || bname.contains("wheat") || bname.contains("beetroot")) {
                return surfaceBlock == net.minecraft.init.Blocks.FARMLAND;
            }
        }
    return surfaceBlock == net.minecraft.init.Blocks.DIRT || surfaceBlock == net.minecraft.init.Blocks.GRASS || surfaceBlock == net.minecraft.init.Blocks.SAND || surfaceBlock == net.minecraft.init.Blocks.SOUL_SAND || surfaceBlock == net.minecraft.init.Blocks.MYCELIUM || surfaceBlock instanceof BlockFarmland;
    }

    private static boolean isPlantableItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        if (CommonConfig.INSTANCE.isSeedBlacklisted(item)) return false;
        if (item instanceof ItemBlock) {
            ItemBlock blockItem = (ItemBlock) item;
            Block block = blockItem.getBlock();
            if (block instanceof BlockCrops || block.getUnlocalizedName().toLowerCase().contains("sapling") || block.getUnlocalizedName().toLowerCase().contains("cocoa") || block.getUnlocalizedName().toLowerCase().contains("sugar") || block.getUnlocalizedName().toLowerCase().contains("wart")) {
                return true;
            }
        }
        String name = item.toString().toLowerCase();
        if (name.contains("seed") || name.contains("sapling") || name.contains("seedling") || name.contains("plant")) return true;
        // 保守策略：基于名称的启发式判断以兼容不同 MC 版本，避免直接引用在 1.12 中可能不存在的 Items 常量
        if (name.contains("pumpkin") || name.contains("melon") || name.contains("potato") || name.contains("carrot") || name.contains("cocoa") || name.contains("sugar") || name.contains("cactus")) return true;
        return false;
    }
}
