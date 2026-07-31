package org.xiyu.onekeyminer.forge;

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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.chain.ServerUseBridge;
import org.xiyu.onekeyminer.platform.PlatformServices;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Forge 平台服务实现
 * 
 * <p>实现 {@link PlatformServices} 接口，提供 Forge 平台特定的功能实现。</p>
 * 
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.21.9
 */
public class ForgePlatformServices implements PlatformServices {
    
    @Override
    public String getPlatformName() {
        return "forge";
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
    public boolean canPlayerBreakBlock(ServerPlayer player, Level level, BlockPos pos, BlockState state) {
        // Forge 使用 BlockEvent.BreakEvent 进行权限检查
        // 如果事件未被取消，权限检查已通过
        
        // 基础检查
        if (player.isSpectator() || !level.hasChunkAt(pos)) {
            return false;
        }
        
        // 检查方块是否可被破坏
        if (state.getDestroySpeed(level, pos) < 0) {
            return false;
        }
        
        // TODO: 集成保护模组
        
        return level.mayInteract(player, pos)
                && player.mayUseItemAt(pos, Direction.UP, player.getMainHandItem());
    }
    
    @Override
    public boolean canPlayerInteract(ServerPlayer player, Level level, BlockPos pos, BlockState state) {
        // 检查玩家是否可以与方块交互
        
        // 检查玩家是否是旁观者模式
        if (player.isSpectator() || !level.hasChunkAt(pos)) {
            return false;
        }
        
        // 检查玩家是否在创造模式或有管理员权限
        if (player.isCreative() || player.hasPermissions(2)) {
            return true;
        }
        
        // TODO: 集成保护模组的交互权限检查
        
        return level.mayInteract(player, pos)
                && player.mayUseItemAt(pos, Direction.UP, player.getMainHandItem());
    }
    
    @Override
    public boolean simulateBlockBreak(ServerPlayer player, Level level, BlockPos pos) {
        // 使用 ServerPlayerGameMode#destroyBlock 模拟玩家破坏方块
        
        try {
            return player.gameMode.destroyBlock(pos);
        } catch (Exception e) {
            OneKeyMiner.LOGGER.error("Forge 模拟方块破坏失败: {}", e.getMessage());
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
                    ServerUseBridge.observeBlockUse(
                            () -> player.gameMode.useItemOn(
                                    player,
                                    level,
                                    item,
                                    hand,
                                    hitResult
                            )
                    );
            return observed.actionDispatched()
                    && observed.result() != null
                    && observed.result().consumesAction();
        } catch (Exception e) {
            OneKeyMiner.LOGGER.error("Forge 模拟物品使用失败: {}", e.getMessage());
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
            // Forge patches Player#interactOn to dispatch its cancellable
            // entity-interaction events.
            ServerUseBridge.ObservedUse<InteractionResult> observed =
                    ServerUseBridge.observeEntityUse(
                            () -> player.interactOn(target, hand)
                    );
            return observed.actionDispatched() && observed.result() != null
                    ? observed.result()
                    : InteractionResult.FAIL;
        } catch (Exception e) {
            OneKeyMiner.LOGGER.error(
                    "Forge 模拟实体交互失败，目标 {}",
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
        return ModList.get().isLoaded(modId);
    }
    
    @Override
    public void registerConfigScreen() {
        // 配置界面在模组主类中通过 ModLoadingContext 注册
        // 见 ForgeConfigScreen 类
    }
    
    @Override
    public String getConventionalTagPrefix() {
        return "forge";
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
