package org.xiyu.onekeyminer.chain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.api.OneKeyMinerAPI;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.MinerConfig;
import org.xiyu.onekeyminer.mining.MiningStateManager;
import org.xiyu.onekeyminer.platform.PlatformServices;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Observes authoritative server use methods at entry and return.
 *
 * <p>The loader callbacks available on Minecraft 1.20.4 run before vanilla
 * reports whether an action succeeded. The common mixins therefore capture
 * the original target here and start derived work only after a consuming
 * server result is returned.</p>
 */
public final class ServerUseBridge {
    private static final ThreadLocal<Deque<BlockUseAttempt>> BLOCK_ATTEMPTS =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<EntityUseAttempt>> ENTITY_ATTEMPTS =
            ThreadLocal.withInitial(ArrayDeque::new);

    private ServerUseBridge() {
    }

    public static void beginBlockUse(
            ServerPlayer player,
            Level level,
            ItemStack stack,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        Deque<BlockUseAttempt> attempts = BLOCK_ATTEMPTS.get();
        attempts.push(captureBlockUse(player, level, stack, hand, hitResult));
    }

    public static void completeBlockUse(InteractionResult originalResult) {
        Deque<BlockUseAttempt> attempts = BLOCK_ATTEMPTS.get();
        if (attempts.isEmpty()) {
            return;
        }
        BlockUseAttempt attempt = attempts.pop();
        if (attempts.isEmpty()) {
            BLOCK_ATTEMPTS.remove();
        }
        if (!attempt.active()
                || originalResult == null
                || !originalResult.consumesAction()) {
            return;
        }
        if (attempt.actionType() == ChainActionType.PLANTING) {
            if (!attempt.level().hasChunkAt(attempt.originPos())) {
                return;
            }
            BlockState placedState = attempt.level().getBlockState(attempt.originPos());
            if (placedState.isAir() || placedState.equals(attempt.originState())) {
                return;
            }
        }

        ChainActionContext context = ChainActionContext.forCompletedBlockUse(
                attempt.player(),
                attempt.level(),
                attempt.originPos(),
                attempt.originState(),
                attempt.actionType(),
                attempt.hand(),
                attempt.interactionOverride(),
                attempt.hitResult()
        );
        report(attempt.player(), attempt.level(), ChainActionLogic.execute(context));
    }

    public static void beginEntityUse(
            ServerPlayer player,
            Entity target,
            InteractionHand hand
    ) {
        Deque<EntityUseAttempt> attempts = ENTITY_ATTEMPTS.get();
        attempts.push(captureEntityUse(player, target, hand));
    }

    public static void completeEntityUse(InteractionResult originalResult) {
        Deque<EntityUseAttempt> attempts = ENTITY_ATTEMPTS.get();
        if (attempts.isEmpty()) {
            return;
        }
        EntityUseAttempt attempt = attempts.pop();
        if (attempts.isEmpty()) {
            ENTITY_ATTEMPTS.remove();
        }
        if (!attempt.active()
                || originalResult == null
                || !originalResult.consumesAction()
                || attempt.target() instanceof Shearable shearable
                && shearable.readyForShearing()) {
            return;
        }

        ChainActionContext context = ChainActionContext.forCompletedEntityUse(
                attempt.player(),
                attempt.level(),
                new ChainActionContext.EntityIdentity(
                        attempt.target().getUUID(),
                        attempt.originPos()
                ),
                attempt.originState(),
                attempt.hand()
        );
        report(attempt.player(), attempt.level(), ChainActionLogic.execute(context));
    }

    private static BlockUseAttempt captureBlockUse(
            ServerPlayer player,
            Level level,
            ItemStack stack,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (!isEligible(player, level)
                || stack == null
                || hand == null
                || hitResult == null) {
            return BlockUseAttempt.INACTIVE;
        }

        BlockPos clickedPos = hitResult.getBlockPos().immutable();
        if (!level.hasChunkAt(clickedPos)) {
            return BlockUseAttempt.INACTIVE;
        }
        BlockState clickedState = level.getBlockState(clickedPos);
        OneKeyMinerAPI.ToolActionRule customRule =
                OneKeyMinerAPI.findToolActionForBlock(stack, clickedState).orElse(null);
        ChainActionType actionType;
        ChainActionContext.InteractionOverride interactionOverride = null;

        if (customRule != null) {
            actionType = customRule.actionType();
            if (actionType == ChainActionType.INTERACTION) {
                interactionOverride = ChainActionLogic.mapInteractionOverride(
                        customRule.interactionRule()
                );
                if (interactionOverride == null) {
                    return BlockUseAttempt.INACTIVE;
                }
            }
        } else if (stack.isEmpty()) {
            // Harvesting has no guaranteed consuming vanilla use and is
            // handled by the loader callback through authoritative breaking.
            return BlockUseAttempt.INACTIVE;
        } else if (ChainActionLogic.isPlantableItem(stack)) {
            actionType = ChainActionType.PLANTING;
        } else if (ChainActionLogic.isValidInteractionTarget(stack, clickedState)
                || ChainActionLogic.canAttemptBlockInteraction(stack)) {
            actionType = ChainActionType.INTERACTION;
        } else {
            return BlockUseAttempt.INACTIVE;
        }

        MinerConfig config = ConfigManager.getConfig();
        if (actionType == ChainActionType.MINING
                || actionType == ChainActionType.HARVESTING
                || actionType == ChainActionType.INTERACTION && !config.enableInteraction
                || actionType == ChainActionType.PLANTING && !config.enablePlanting) {
            return BlockUseAttempt.INACTIVE;
        }

        BlockPos originPos = actionType == ChainActionType.PLANTING
                ? clickedPos.relative(hitResult.getDirection())
                : clickedPos;
        if (!level.hasChunkAt(originPos)) {
            return BlockUseAttempt.INACTIVE;
        }
        return new BlockUseAttempt(
                true,
                player,
                level,
                hand,
                hitResult,
                originPos.immutable(),
                level.getBlockState(originPos),
                actionType,
                interactionOverride
        );
    }

    private static EntityUseAttempt captureEntityUse(
            ServerPlayer player,
            Entity target,
            InteractionHand hand
    ) {
        if (player == null
                || target == null
                || hand == null
                || !(target instanceof Shearable shearable)
                || !shearable.readyForShearing()
                || !isEligible(player, player.level())) {
            return EntityUseAttempt.INACTIVE;
        }
        MinerConfig config = ConfigManager.getConfig();
        if (!config.enableInteraction) {
            return EntityUseAttempt.INACTIVE;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        OneKeyMinerAPI.ToolActionRule customRule =
                OneKeyMinerAPI.findToolActionForEntity(heldItem, target).orElse(null);
        if (customRule != null
                && (customRule.actionType() != ChainActionType.INTERACTION
                || customRule.interactionRule() != OneKeyMinerAPI.InteractionRule.SHEARING)) {
            return EntityUseAttempt.INACTIVE;
        }
        if (customRule == null && !ChainActionLogic.isShearingTool(heldItem)) {
            return EntityUseAttempt.INACTIVE;
        }

        BlockPos originPos = target.blockPosition().immutable();
        return new EntityUseAttempt(
                true,
                player,
                player.level(),
                target,
                hand,
                originPos,
                player.level().getBlockState(originPos)
        );
    }

    private static boolean isEligible(ServerPlayer player, Level level) {
        return player != null
                && level != null
                && !level.isClientSide()
                && !ChainActionLogic.isProcessing()
                && ConfigManager.getConfig().enabled
                && MiningStateManager.isHoldingKey(player);
    }

    private static void report(
            ServerPlayer player,
            Level level,
            ChainActionResult result
    ) {
        if (result == null || !result.isSuccess() || result.totalCount() <= 0) {
            return;
        }
        MinerConfig config = ConfigManager.getConfig();
        if (config.showStats) {
            PlatformServices.getInstance().sendChainActionMessage(
                    player,
                    result.actionType().getId(),
                    result.totalCount()
            );
        }
        if (config.playSound) {
            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS,
                    0.6f,
                    1.0f
            );
        }
        OneKeyMiner.LOGGER.debug("Completed derived {} action: {}", result.actionType(), result.getSummary());
    }

    private record BlockUseAttempt(
            boolean active,
            ServerPlayer player,
            Level level,
            InteractionHand hand,
            BlockHitResult hitResult,
            BlockPos originPos,
            BlockState originState,
            ChainActionType actionType,
            ChainActionContext.InteractionOverride interactionOverride
    ) {
        private static final BlockUseAttempt INACTIVE = new BlockUseAttempt(
                false, null, null, null, null, null, null, null, null
        );
    }

    private record EntityUseAttempt(
            boolean active,
            ServerPlayer player,
            Level level,
            Entity target,
            InteractionHand hand,
            BlockPos originPos,
            BlockState originState
    ) {
        private static final EntityUseAttempt INACTIVE = new EntityUseAttempt(
                false, null, null, null, null, null, null
        );
    }
}
