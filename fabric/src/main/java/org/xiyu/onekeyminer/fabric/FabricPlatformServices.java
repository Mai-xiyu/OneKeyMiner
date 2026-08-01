package org.xiyu.onekeyminer.fabric;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.chain.ServerUseBridge;
import org.xiyu.onekeyminer.platform.PlatformServices;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Fabric 平台服务实现
 *
 * <p>实现 {@link PlatformServices} 接口，提供 Fabric 平台特定的功能实现。</p>
 *
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.21.1
 */
public class FabricPlatformServices implements PlatformServices {

    @Override
    public String getPlatformName() {
        return "fabric";
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT;
    }

    @Override
    public boolean isDedicatedServer() {
        return FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.SERVER;
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean canPlayerBreakBlock(ServerPlayer player, Level level, BlockPos pos, BlockState state) {
        if (player.isSpectator() || !level.hasChunkAt(pos)) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0 && !player.isCreative()) {
            return false;
        }
        // The authoritative destroy call still runs Fabric's per-target break
        // callbacks, so this is only a cheap fail-closed precheck.
        return level.mayInteract(player, pos)
                && player.mayUseItemAt(pos, Direction.UP, player.getMainHandItem());
    }

    @Override
    public boolean canPlayerInteract(ServerPlayer player, Level level, BlockPos pos, BlockState state) {
        if (player.isSpectator() || !level.hasChunkAt(pos)) {
            return false;
        }
        // The authoritative use call emits the physical loader callback for
        // every target; never bypass vanilla's preliminary checks here.
        return level.mayInteract(player, pos)
                && player.mayUseItemAt(pos, Direction.UP, player.getMainHandItem());
    }

    @Override
    public boolean simulateBlockBreak(ServerPlayer player, Level level, BlockPos pos) {
        // 使用 ServerPlayerGameMode#destroyBlock 来模拟玩家破坏方块
        // 这是关键方法，确保：
        // 1. 触发 PlayerBlockBreakEvents
        // 2. 正确应用战利品表
        // 3. 正确处理工具耐久和附魔

        try {
            // 获取 ServerPlayerGameMode 并调用 destroyBlock
            // Mojmap 1.21.1 exposes the authoritative server break path here.
            return player.gameMode.destroyBlock(pos);
        } catch (Exception e) {
            // 如果方法调用失败，记录错误
            org.xiyu.onekeyminer.OneKeyMiner.LOGGER.error("模拟方块破坏失败: {}", e.getMessage());
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
        // 使用 Item#useOn 来模拟玩家对方块使用物品
        // 这会触发正确的游戏事件（如锄头耕地、斧头剥皮等）

        try {
            ServerUseBridge.ObservedUse<InteractionResult> observed =
                    ServerUseBridge.observeBlockUse(() -> {
                        // Fabric dispatches UseBlockCallback in the packet
                        // handler, not in ServerPlayerGameMode. Derived targets
                        // must publish it explicitly so protection/audit mods
                        // see each interaction once.
                        InteractionResult callbackResult =
                                UseBlockCallback.EVENT.invoker().interact(
                                        player,
                                        level,
                                        hand,
                                        hitResult
                                );
                        if (callbackResult == null) {
                            return null;
                        }
                        if (callbackResult != InteractionResult.PASS) {
                            return callbackResult;
                        }
                        return player.gameMode.useItemOn(
                                player,
                                level,
                                item,
                                hand,
                                hitResult
                        );
                    });
            InteractionResult result = observed.result();
            if (result == null) {
                OneKeyMiner.LOGGER.error(
                        "Fabric 方块交互回调为目标 {} 返回了 null，已拒绝该交互",
                        hitResult.getBlockPos()
                );
                return false;
            }
            return observed.actionDispatched() && result.consumesAction();
        } catch (Exception e) {
            org.xiyu.onekeyminer.OneKeyMiner.LOGGER.error("模拟物品使用失败: {}", e.getMessage());
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
            ServerUseBridge.ObservedUse<InteractionResult> observed =
                    ServerUseBridge.observeEntityUse(() -> {
                        // Fabric hooks entity-use in the network handler rather
                        // than Player#interactOn. Derived targets publish it
                        // explicitly before falling back to vanilla behavior.
                        InteractionResult callbackResult =
                                UseEntityCallback.EVENT.invoker().interact(
                                        player,
                                        level,
                                        hand,
                                        target,
                                        null
                                );
                        if (callbackResult == null
                                || callbackResult != InteractionResult.PASS) {
                            return callbackResult;
                        }
                        return player.interactOn(target, hand);
                    });
            if (observed.result() == null) {
                OneKeyMiner.LOGGER.error(
                        "Fabric 实体交互回调为目标 {} 返回了 null，已拒绝该交互",
                        target.getUUID()
                );
                return InteractionResult.FAIL;
            }
            return observed.actionDispatched()
                    ? observed.result()
                    : InteractionResult.FAIL;
        } catch (Exception e) {
            OneKeyMiner.LOGGER.error(
                    "Fabric 模拟实体交互失败，目标 {}",
                    target.getUUID(),
                    e
            );
            return InteractionResult.FAIL;
        }
    }

    @Override
    public boolean isChainModeActive(ServerPlayer player) {
        return org.xiyu.onekeyminer.mining.MiningStateManager.isHoldingKey(player);
    }

    @Override
    public void setChainModeActive(ServerPlayer player, boolean active) {
        org.xiyu.onekeyminer.mining.MiningStateManager.setHoldingKey(player, active);
    }

    @Override
    public void sendChainActionMessage(ServerPlayer player, String actionType, int count) {
        // 使用 ActionBar 消息通知玩家
        String translationKey = "message.onekeyminer.chain_action." + actionType;
        Component message = Component.translatable(translationKey, count);
        player.displayClientMessage(message, true);
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public void registerConfigScreen() {
        // Fabric 的配置界面通过 Mod Menu 集成注册
        // 在 fabric.mod.json 中配置 entrypoints
        // 见 ModMenuIntegration 类
    }

    /**
     * 清理玩家状态（玩家退出时调用）
     *
     * @param playerId 玩家 UUID
     */
    public static void cleanupPlayer(UUID playerId) {
        org.xiyu.onekeyminer.mining.MiningStateManager.clearState(playerId);
    }
}
