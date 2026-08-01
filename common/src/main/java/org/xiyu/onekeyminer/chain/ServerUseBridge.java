package org.xiyu.onekeyminer.chain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.api.OneKeyMinerAPI;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.MinerConfig;
import org.xiyu.onekeyminer.platform.PlatformServices;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Loader-neutral after-use bridge.
 *
 * <p>The loader event or vanilla action handles the clicked origin first.
 * Only a successful result may authorize derived targets, so protection and
 * audit hooks observe the origin exactly once.</p>
 */
public final class ServerUseBridge {

    private static final Set<ChainActionType> RIGHT_CLICK_ACTION_TYPES = Set.of(
            ChainActionType.INTERACTION,
            ChainActionType.PLANTING
    );
    private static final UseDispatchObserver BLOCK_USE_OBSERVER =
            new UseDispatchObserver();
    private static final UseDispatchObserver ENTITY_USE_OBSERVER =
            new UseDispatchObserver();

    private ServerUseBridge() {
    }

    public static InteractionResult useOn(
            ItemStack item,
            UseOnContext context
    ) {
        Player player = context.getPlayer();
        BlockHitResult hitResult = new BlockHitResult(
                context.getClickLocation(),
                context.getClickedFace(),
                context.getClickedPos(),
                context.isInside()
        );
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return item.useOn(context);
        }
        return runBlockUse(
                serverPlayer,
                context.getLevel(),
                item,
                context.getHand(),
                hitResult,
                () -> item.useOn(context)
        );
    }

    public static ItemInteractionResult useItemOnBlock(
            BlockState state,
            ItemStack item,
            Level level,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return state.useItemOn(item, level, player, hand, hitResult);
        }
        return runBlockStateUse(
                serverPlayer,
                level,
                item,
                hand,
                hitResult,
                () -> state.useItemOn(
                        item,
                        level,
                        player,
                        hand,
                        hitResult
                )
        );
    }

    private static ItemInteractionResult runBlockStateUse(
            ServerPlayer player,
            Level level,
            ItemStack item,
            InteractionHand hand,
            BlockHitResult hitResult,
            Supplier<ItemInteractionResult> originalUse
    ) {
        PendingBlockUse pending = captureBlockUse(
                player,
                level,
                item,
                hand,
                hitResult
        );
        BLOCK_USE_OBSERVER.markDispatched();
        ItemInteractionResult result = originalUse.get();
        if (pending != null && result != null && result.consumesAction()) {
            completeBlockUse(pending);
        }
        return result;
    }

    public static InteractionResult runBlockUse(
            ServerPlayer player,
            Level level,
            ItemStack item,
            InteractionHand hand,
            BlockHitResult hitResult,
            Supplier<InteractionResult> originalUse
    ) {
        PendingBlockUse pending = captureBlockUse(
                player,
                level,
                item,
                hand,
                hitResult
        );
        BLOCK_USE_OBSERVER.markDispatched();
        InteractionResult result = originalUse.get();
        if (pending != null && result != null && result.consumesAction()) {
            completeBlockUse(pending);
        }
        return result;
    }

    public static InteractionResult interact(
            Player player,
            Entity target,
            InteractionHand hand
    ) {
        return runEntityUse(
                player,
                target,
                hand,
                player.getItemInHand(hand),
                () -> target.interact(player, hand)
        );
    }

    public static InteractionResult interactLivingEntity(
            ItemStack item,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        return runEntityUse(
                player,
                target,
                hand,
                item,
                () -> item.interactLivingEntity(player, target, hand)
        );
    }

    public static InteractionResult interactAt(
            Player player,
            Entity target,
            Vec3 location,
            InteractionHand hand
    ) {
        return runEntityUse(
                player,
                target,
                hand,
                player.getItemInHand(hand),
                () -> target.interactAt(player, location, hand)
        );
    }

    public static InteractionResult runEntityUse(
            Player player,
            Entity target,
            InteractionHand hand,
            ItemStack item,
            Supplier<InteractionResult> originalUse
    ) {
        PendingEntityUse pending = captureEntityUse(
                player,
                target,
                hand,
                item
        );
        ENTITY_USE_OBSERVER.markDispatched();
        InteractionResult result = originalUse.get();
        completeSuccessfulEntityUse(pending, result);
        return result;
    }

    public static <T> ObservedUse<T> observeBlockUse(
            Supplier<T> authoritativeUse
    ) {
        return observeUse(BLOCK_USE_OBSERVER, authoritativeUse);
    }

    public static <T> ObservedUse<T> observeEntityUse(
            Supplier<T> authoritativeUse
    ) {
        return observeUse(ENTITY_USE_OBSERVER, authoritativeUse);
    }

    private static <T> ObservedUse<T> observeUse(
            UseDispatchObserver observer,
            Supplier<T> authoritativeUse
    ) {
        UseDispatchObserver.Observed<T> observed =
                observer.observe(authoritativeUse);
        return new ObservedUse<>(
                observed.result(),
                observed.dispatched()
        );
    }

    private static void completeSuccessfulEntityUse(
            PendingEntityUse pending,
            InteractionResult result
    ) {
        if (pending != null && result != null && result.consumesAction()) {
            completeEntityUse(pending);
        }
    }

    private static PendingBlockUse captureBlockUse(
            ServerPlayer player,
            Level level,
            ItemStack item,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (ChainActionLogic.isProcessing()
                || player == null
                || level == null
                || level.isClientSide()
                || player.level() != level
                || item == null
                || hand == null
                || hitResult == null) {
            return null;
        }

        MinerConfig config = ConfigManager.getConfig();
        if (!config.enabled
                || !PlatformServices.getInstance().isChainModeActive(player)) {
            return null;
        }

        BlockPos clickedPos = hitResult.getBlockPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        OneKeyMinerAPI.ToolActionRule customRule =
                OneKeyMinerAPI.findToolActionForBlock(
                        item,
                        clickedState,
                        RIGHT_CLICK_ACTION_TYPES
                ).orElse(null);

        ChainActionType actionType;
        ChainActionContext.InteractionOverride interactionOverride = null;
        if (customRule != null) {
            actionType = customRule.actionType();
            if (actionType == ChainActionType.INTERACTION) {
                interactionOverride = ChainActionLogic.mapInteractionOverride(
                        customRule.interactionRule()
                );
                if (interactionOverride == null) {
                    return null;
                }
            }
        } else {
            actionType = detectNativeBlockAction(item, clickedState);
        }

        if (actionType == ChainActionType.INTERACTION
                && !config.enableInteraction) {
            return null;
        }
        if (actionType == ChainActionType.PLANTING
                && !config.enablePlanting) {
            return null;
        }
        // Harvesting remains a pre-event operation because vanilla right-click
        // on most mature crops returns PASS and performs no original action.
        if (actionType != ChainActionType.INTERACTION
                && actionType != ChainActionType.PLANTING) {
            return null;
        }

        BlockPos originPos = clickedPos;
        BlockState originState = clickedState;
        if (actionType == ChainActionType.PLANTING) {
            originPos = new BlockPlaceContext(
                    player,
                    hand,
                    item,
                    hitResult
            ).getClickedPos();
            if (!level.hasChunkAt(originPos)) {
                return null;
            }
            originState = level.getBlockState(originPos);
        }

        return new PendingBlockUse(
                player,
                level,
                originPos.immutable(),
                originState,
                actionType,
                item.copy(),
                hand,
                interactionOverride,
                customRule,
                determineBlockRequirement(actionType, item, customRule),
                hitResult
        );
    }

    private static PendingEntityUse captureEntityUse(
            Player player,
            Entity target,
            InteractionHand hand,
            ItemStack item
    ) {
        if (ChainActionLogic.isProcessing()
                || !(player instanceof ServerPlayer serverPlayer)
                || target == null
                || hand == null
                || player.level().isClientSide()) {
            return null;
        }

        MinerConfig config = ConfigManager.getConfig();
        if (!config.enabled
                || !config.enableInteraction
                || !PlatformServices.getInstance()
                        .isChainModeActive(serverPlayer)) {
            return null;
        }

        if (item == null || item.isEmpty()) {
            return null;
        }

        OneKeyMinerAPI.ToolActionRule customRule =
                OneKeyMinerAPI.findToolActionForEntity(
                        item,
                        target,
                        ChainActionType.INTERACTION
                ).orElse(null);
        boolean trustSuccessfulResult;
        if (customRule != null) {
            ChainActionContext.InteractionOverride override =
                    ChainActionLogic.mapInteractionOverride(
                            customRule.interactionRule()
                    );
            if (override != ChainActionContext.InteractionOverride.SHEARING) {
                return null;
            }
            trustSuccessfulResult = !(target instanceof Shearable shearable)
                    || !shearable.readyForShearing();
        } else {
            if (!(item.getItem() instanceof ShearsItem)
                    || !(target instanceof Shearable shearable)
                    || !shearable.readyForShearing()) {
                return null;
            }
            trustSuccessfulResult = false;
        }

        BlockPos originPos = target.blockPosition().immutable();
        Level level = serverPlayer.level();
        return new PendingEntityUse(
                serverPlayer,
                level,
                originPos,
                level.getBlockState(originPos),
                item.copy(),
                hand,
                target.getUUID(),
                ShearingCompletionVerifier.capture(level, target),
                customRule,
                trustSuccessfulResult
        );
    }

    private static ChainActionType detectNativeBlockAction(
            ItemStack item,
            BlockState clickedState
    ) {
        if (item.isEmpty()) {
            return ChainActionLogic.isMatureCrop(clickedState)
                    ? ChainActionType.HARVESTING
                    : null;
        }
        if (ChainActionLogic.isPlantableItem(item)) {
            return ChainActionType.PLANTING;
        }
        if (ChainActionLogic.isValidInteractionTarget(item, clickedState)) {
            return ChainActionType.INTERACTION;
        }
        return null;
    }

    private static OriginalUseCompletionPolicy.BlockRequirement
            determineBlockRequirement(
                    ChainActionType actionType,
                    ItemStack item,
                    OneKeyMinerAPI.ToolActionRule customRule
            ) {
        boolean statefulNativeTool = item.getItem() instanceof HoeItem
                || item.getItem() instanceof AxeItem
                || item.getItem() instanceof ShovelItem;
        return OriginalUseCompletionPolicy.selectBlockRequirement(
                actionType == ChainActionType.PLANTING,
                customRule != null,
                statefulNativeTool
        );
    }

    private static void completeBlockUse(PendingBlockUse pending) {
        if (pending.player().level() != pending.level()
                || !isSameUsableItem(pending)) {
            return;
        }

        BlockState completedState = pending.level().getBlockState(
                pending.originPos()
        );
        boolean stateChanged = !completedState.equals(pending.originState());
        if (!OriginalUseCompletionPolicy.permitsDerivedBlockUse(
                pending.completionRequirement(),
                stateChanged,
                !completedState.isAir()
        )) {
            return;
        }
        try {
            ChainActionResult result = ChainActionLogic.execute(
                    ChainActionContext.forCompletedBlockUse(
                            pending.player(),
                            pending.level(),
                            pending.originPos(),
                            pending.originState(),
                            pending.actionType(),
                            pending.originalItem(),
                            pending.hand(),
                            pending.interactionOverride(),
                            pending.matchedToolActionRule(),
                            pending.hitResult()
                    )
            );
            reportResult(pending.player(), pending.level(), result);
        } catch (RuntimeException exception) {
            OneKeyMiner.LOGGER.error(
                    "Failed to dispatch derived block interaction",
                    exception
            );
        }
    }

    private static void completeEntityUse(PendingEntityUse pending) {
        if (pending.player().level() != pending.level()
                || !isSameUsableItem(pending)) {
            return;
        }
        if (!OriginalUseCompletionPolicy.permitsDerivedEntityUse(
                pending.trustSuccessfulResult(),
                hasCompletedShearing(pending)
        )) {
            return;
        }
        try {
            ChainActionResult result = ChainActionLogic.execute(
                    ChainActionContext.forCompletedEntityUse(
                            pending.player(),
                            pending.level(),
                            pending.originPos(),
                            pending.originState(),
                            pending.originalItem(),
                            pending.hand(),
                            pending.originEntityId(),
                            pending.matchedToolActionRule()
                    )
            );
            reportResult(pending.player(), pending.level(), result);
        } catch (RuntimeException exception) {
            OneKeyMiner.LOGGER.error(
                    "Failed to dispatch derived entity interaction",
                    exception
            );
        }
    }

    private static boolean isSameUsableItem(PendingBlockUse pending) {
        ItemStack current = pending.player().getItemInHand(pending.hand());
        return !current.isEmpty()
                && ItemStack.isSameItem(pending.originalItem(), current);
    }

    private static boolean isSameUsableItem(PendingEntityUse pending) {
        ItemStack current = pending.player().getItemInHand(pending.hand());
        return !current.isEmpty()
                && ItemStack.isSameItem(pending.originalItem(), current);
    }

    private static boolean hasCompletedShearing(PendingEntityUse pending) {
        return ShearingCompletionVerifier.completed(
                pending.level(),
                pending.shearingSnapshot()
        );
    }

    private static void reportResult(
            ServerPlayer player,
            Level level,
            ChainActionResult result
    ) {
        if (!result.isSuccess()) {
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
        OneKeyMiner.LOGGER.debug(
                "{} completed after original use: {}",
                result.actionType().getDisplayName(),
                result.getSummary()
        );
    }

    private record PendingBlockUse(
            ServerPlayer player,
            Level level,
            BlockPos originPos,
            BlockState originState,
            ChainActionType actionType,
            ItemStack originalItem,
            InteractionHand hand,
            ChainActionContext.InteractionOverride interactionOverride,
            OneKeyMinerAPI.ToolActionRule matchedToolActionRule,
            OriginalUseCompletionPolicy.BlockRequirement completionRequirement,
            BlockHitResult hitResult
    ) {
    }

    private record PendingEntityUse(
            ServerPlayer player,
            Level level,
            BlockPos originPos,
            BlockState originState,
            ItemStack originalItem,
            InteractionHand hand,
            java.util.UUID originEntityId,
            ShearingCompletionVerifier.Snapshot shearingSnapshot,
            OneKeyMinerAPI.ToolActionRule matchedToolActionRule,
            boolean trustSuccessfulResult
    ) {
    }

    public record ObservedUse<T>(T result, boolean actionDispatched) {
    }
}
