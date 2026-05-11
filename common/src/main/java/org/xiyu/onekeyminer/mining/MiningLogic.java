package org.xiyu.onekeyminer.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.api.OneKeyMinerAPI;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.MinerConfig;
import org.xiyu.onekeyminer.platform.PlatformServices;
import org.xiyu.onekeyminer.shape.ChainShape;
import org.xiyu.onekeyminer.shape.ShapeContext;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Legacy mining entry point kept for loaders that still call it directly.
 */
public class MiningLogic {
    private static final ThreadLocal<Boolean> IS_MINING = ThreadLocal.withInitial(() -> false);

    public static void onBlockBreak(ServerPlayer player, Level level, BlockPos originPos, BlockState originState) {
        if (IS_MINING.get()) {
            return;
        }

        MinerConfig config = ConfigManager.getConfig();
        if (!config.enabled || !MiningStateManager.isHoldingKey(player)) {
            return;
        }
        if (!OneKeyMinerAPI.isBlockAllowed(originState.getBlock())) {
            return;
        }

        ItemStack tool = player.getMainHandItem();
        if (!OneKeyMinerAPI.isToolAllowed(tool)) {
            return;
        }

        try {
            IS_MINING.set(true);
            performVeinMining(player, level, originPos, originState, tool, config);
        } finally {
            IS_MINING.set(false);
        }
    }

    private static void performVeinMining(
            ServerPlayer player,
            Level level,
            BlockPos originPos,
            BlockState originState,
            ItemStack tool,
            MinerConfig config
    ) {
        List<BlockPos> blocksToMine = collectBlocks(player, level, originPos, originState, config);
        if (blocksToMine.isEmpty()) {
            return;
        }

        MiningResult result = mineBlocks(player, level, blocksToMine, tool, config);
        if (config.showStats && result.totalMined() > 0) {
            sendMiningStats(player, result);
        }

        OneKeyMiner.LOGGER.debug(
                "Chain mining complete: mined {} blocks, stop reason {}",
                result.totalMined(),
                result.stopReason()
        );
    }

    private static List<BlockPos> collectBlocks(
            ServerPlayer player,
            Level level,
            BlockPos originPos,
            BlockState originState,
            MinerConfig config
    ) {
        ChainShape shape = ShapeRegistry.getShapeOrDefault(MiningStateManager.getPlayerShape(player));
        if (shape == null) {
            shape = ShapeRegistry.getShapeOrDefault(config.selectedShape);
        }
        if (shape == null) {
            return List.of();
        }

        Direction verticalDir = null;
        float pitch = player.getXRot();
        if (pitch < -45.0F) {
            verticalDir = Direction.UP;
        } else if (pitch > 45.0F) {
            verticalDir = Direction.DOWN;
        }

        ShapeContext context = new ShapeContext.Builder()
                .level(level)
                .originPos(originPos)
                .originState(originState)
                .playerFacing(player.getDirection())
                .playerLookingVertical(verticalDir)
                .maxBlocks(config.maxBlocks)
                .maxDistance(config.maxDistance)
                .allowDiagonal(config.allowDiagonal)
                .blockMatcher((origin, target) -> isMatchingBlock(origin, target, config))
                .build();

        return shape.collectBlocks(context);
    }

    private static MiningResult mineBlocks(
            ServerPlayer player,
            Level level,
            List<BlockPos> blocks,
            ItemStack tool,
            MinerConfig config
    ) {
        List<BlockPos> minedBlocks = new ArrayList<>();
        StopReason stopReason = StopReason.COMPLETED;
        int totalExpCollected = 0;
        boolean teleportDrops = MiningStateManager.isTeleportDrops(player);
        boolean teleportExp = MiningStateManager.isTeleportExp(player);

        Set<Integer> existingEntityIds = new HashSet<>();
        if (teleportDrops || teleportExp) {
            AABB searchArea = calculateSearchArea(blocks);
            if (level instanceof ServerLevel serverLevel) {
                for (ItemEntity entity : serverLevel.getEntitiesOfClass(ItemEntity.class, searchArea)) {
                    existingEntityIds.add(entity.getId());
                }
                for (ExperienceOrb entity : serverLevel.getEntitiesOfClass(ExperienceOrb.class, searchArea)) {
                    existingEntityIds.add(entity.getId());
                }
            }
        }

        for (BlockPos pos : blocks) {
            if (config.consumeDurability && config.stopOnLowDurability && tool.isDamageableItem()) {
                int remainingDurability = tool.getMaxDamage() - tool.getDamageValue();
                if (remainingDurability <= config.preserveDurability) {
                    stopReason = StopReason.LOW_DURABILITY;
                    break;
                }
            }

            if (tool.isEmpty()) {
                stopReason = StopReason.TOOL_BROKEN;
                break;
            }

            if (config.consumeHunger && player.getFoodData().getFoodLevel() <= config.minHungerLevel) {
                stopReason = StopReason.LOW_HUNGER;
                break;
            }

            BlockState state = level.getBlockState(pos);
            if (!PlatformServices.getInstance().canPlayerBreakBlock(player, level, pos, state)) {
                continue;
            }

            if (PlatformServices.getInstance().simulateBlockBreak(player, level, pos)) {
                minedBlocks.add(pos);
                if (config.consumeHunger && config.hungerMultiplier > 0) {
                    player.getFoodData().addExhaustion(0.005f * config.hungerMultiplier);
                }
            }
        }

        if (level instanceof ServerLevel serverLevel && !minedBlocks.isEmpty()) {
            AABB searchArea = calculateSearchArea(minedBlocks);
            if (teleportDrops) {
                collectAndTeleportDrops(serverLevel, player, searchArea, existingEntityIds);
            }
            if (teleportExp) {
                totalExpCollected = collectAndTeleportExp(serverLevel, player, searchArea, existingEntityIds);
            }
        }

        return new MiningResult(minedBlocks, minedBlocks.size(), stopReason, totalExpCollected);
    }

    private static AABB calculateSearchArea(List<BlockPos> blocks) {
        if (blocks.isEmpty()) {
            return new AABB(0, 0, 0, 0, 0, 0);
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : blocks) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        return new AABB(minX - 2, minY - 2, minZ - 2, maxX + 3, maxY + 3, maxZ + 3);
    }

    private static List<ItemStack> collectAndTeleportDrops(
            ServerLevel level,
            ServerPlayer player,
            AABB area,
            Set<Integer> existingEntityIds
    ) {
        List<ItemStack> collectedDrops = new ArrayList<>();
        List<ItemEntity> newItems = level.getEntitiesOfClass(
                ItemEntity.class,
                area,
                entity -> !existingEntityIds.contains(entity.getId()) && entity.isAlive()
        );

        for (ItemEntity itemEntity : newItems) {
            ItemStack stack = itemEntity.getItem().copy();
            collectedDrops.add(stack.copy());
            if (player.getInventory().add(stack)) {
                itemEntity.discard();
            } else {
                itemEntity.teleportTo(player.getX(), player.getY(), player.getZ());
            }
        }
        return collectedDrops;
    }

    private static int collectAndTeleportExp(
            ServerLevel level,
            ServerPlayer player,
            AABB area,
            Set<Integer> existingEntityIds
    ) {
        List<ExperienceOrb> newOrbs = level.getEntitiesOfClass(
                ExperienceOrb.class,
                area,
                entity -> !existingEntityIds.contains(entity.getId()) && entity.isAlive()
        );

        int totalExp = 0;
        for (ExperienceOrb orb : newOrbs) {
            totalExp += orb.getValue();
            orb.discard();
        }

        if (totalExp > 0) {
            player.giveExperiencePoints(totalExp);
        }

        return totalExp;
    }

    private static boolean isMatchingBlock(BlockState origin, BlockState target, MinerConfig config) {
        if (target.isAir()) {
            return false;
        }

        Block originBlock = origin.getBlock();
        Block targetBlock = target.getBlock();

        if (OneKeyMinerAPI.isBlockBlacklisted(targetBlock)) {
            return false;
        }
        if (!OneKeyMinerAPI.isBlockAllowed(targetBlock)) {
            return false;
        }

        if (config.requireExactMatch) {
            return originBlock == targetBlock;
        }
        return originBlock == targetBlock || OneKeyMinerAPI.areBlocksInSameGroup(originBlock, targetBlock);
    }

    private static void sendMiningStats(ServerPlayer player, MiningResult result) {
        StringBuilder message = new StringBuilder();
        message.append("Chain mining: ").append(result.totalMined()).append(" blocks");

        if (result.expCollected() > 0) {
            message.append(" | +").append(result.expCollected()).append(" XP");
        }

        if (result.stopReason() != StopReason.COMPLETED) {
            message.append(" (").append(result.stopReason().getMessage()).append(")");
        }
        player.sendOverlayMessage(
                net.minecraft.network.chat.Component.literal(message.toString())
        );
    }

    public record MiningResult(List<BlockPos> minedBlocks, int totalMined, StopReason stopReason, int expCollected) {
        public MiningResult(List<BlockPos> minedBlocks, int totalMined, StopReason stopReason) {
            this(minedBlocks, totalMined, stopReason, 0);
        }
    }

    public enum StopReason {
        COMPLETED("completed"),
        MAX_BLOCKS("max blocks"),
        LOW_DURABILITY("low durability"),
        TOOL_BROKEN("tool broken"),
        LOW_HUNGER("low hunger"),
        CANCELLED("cancelled");

        private final String message;

        StopReason(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
