package org.xiyu.onekeyminer.neoforge;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.mining.MiningStateManager;
import org.xiyu.onekeyminer.platform.PlatformServices;

import java.nio.file.Path;
import java.util.UUID;

/** NeoForge 20.4 platform adapter. */
public final class NeoForgePlatformServices implements PlatformServices {

    @Override
    public String getPlatformName() {
        return "neoforge";
    }

    @Override
    public boolean isClient() {
        return FMLLoader.getDist().isClient();
    }

    @Override
    public boolean isDedicatedServer() {
        return FMLLoader.getDist().isDedicatedServer();
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean canPlayerBreakBlock(
            ServerPlayer player,
            Level level,
            BlockPos pos,
            BlockState state
    ) {
        if (player.isSpectator()) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0 && !player.isCreative()) {
            return false;
        }

        // Do not post BlockEvent.BreakEvent here. destroyBlock below posts the
        // authoritative event once and observes protection-mod cancellation.
        return true;
    }

    @Override
    public boolean canPlayerInteract(
            ServerPlayer player,
            Level level,
            BlockPos pos,
            BlockState state
    ) {
        // Authoritative use/break methods below still run NeoForge and vanilla
        // permission hooks. This precheck must not reject valid off-hand uses.
        return !player.isSpectator();
    }

    @Override
    public boolean simulateBlockBreak(ServerPlayer player, Level level, BlockPos pos) {
        try {
            return player.gameMode.destroyBlock(pos);
        } catch (RuntimeException exception) {
            OneKeyMiner.LOGGER.error(
                    "NeoForge block break failed at {}",
                    pos,
                    exception
            );
            return false;
        }
    }

    @Override
    public boolean simulateItemUseOnBlock(
            ServerPlayer player,
            Level level,
            BlockHitResult hitResult,
            InteractionHand hand,
            ItemStack item
    ) {
        try {
            InteractionResult result = player.gameMode.useItemOn(
                    player,
                    level,
                    item,
                    hand,
                    hitResult
            );
            return result.consumesAction();
        } catch (RuntimeException exception) {
            OneKeyMiner.LOGGER.error(
                    "NeoForge item interaction failed at {}",
                    hitResult.getBlockPos(),
                    exception
            );
            return false;
        }
    }

    @Override
    public InteractionResult simulateEntityInteraction(
            ServerPlayer player,
            Level level,
            Entity target,
            InteractionHand hand
    ) {
        try {
            return player.interactOn(target, hand);
        } catch (RuntimeException exception) {
            OneKeyMiner.LOGGER.error(
                    "NeoForge entity interaction failed for {}",
                    target.getUUID(),
                    exception
            );
            return InteractionResult.FAIL;
        }
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public void registerConfigScreen() {
        // Registered by NeoForgeClientSetup on the physical client.
    }

    @Override
    public String getConventionalTagPrefix() {
        // NeoForge adopted c in 1.20.5; the 20.4 convention is forge.
        return "forge";
    }

    @Override
    public boolean isChainModeActive(ServerPlayer player) {
        return MiningStateManager.isHoldingKey(player);
    }

    @Override
    public void setChainModeActive(ServerPlayer player, boolean active) {
        MiningStateManager.setHoldingKey(player, active);
    }

    @Override
    public void sendChainActionMessage(
            ServerPlayer player,
            String actionType,
            int count
    ) {
        player.displayClientMessage(
                Component.translatable(
                        "message.onekeyminer.chain_action." + actionType,
                        count
                ),
                true
        );
    }

    public static void cleanupPlayer(UUID playerId) {
        MiningStateManager.clearState(playerId);
    }
}
