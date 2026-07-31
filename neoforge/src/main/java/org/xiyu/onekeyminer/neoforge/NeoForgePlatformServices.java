package org.xiyu.onekeyminer.neoforge;

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
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.chain.ServerUseBridge;
import org.xiyu.onekeyminer.platform.PlatformServices;

import java.nio.file.Path;
import java.util.UUID;

/**
 * NeoForge 平台服务实现
 * 
 * <p>实现 {@link PlatformServices} 接口，提供 NeoForge 平台特定的功能实现。</p>
 * 
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.21.9
 */
public class NeoForgePlatformServices implements PlatformServices {
    
    @Override
    public String getPlatformName() {
        return "neoforge";
    }
    
    @Override
    public boolean isClient() {
        return FMLEnvironment.getDist().isClient();
    }
    
    @Override
    public boolean isDedicatedServer() {
        return FMLEnvironment.getDist().isDedicatedServer();
    }
    
    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }
    
    @Override
    public boolean canPlayerBreakBlock(ServerPlayer player, Level level, BlockPos pos, BlockState state) {
        if (player.isSpectator() || !level.hasChunkAt(pos)) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0 && !player.isCreative()) {
            return false;
        }
        // The authoritative destroy call posts BlockEvent.BreakEvent exactly once.
        return level.mayInteract(player, pos)
                && player.mayUseItemAt(pos, Direction.UP, player.getMainHandItem());
    }
    
    @Override
    public boolean canPlayerInteract(ServerPlayer player, Level level, BlockPos pos, BlockState state) {
        if (player.isSpectator() || !level.hasChunkAt(pos)) {
            return false;
        }
        return level.mayInteract(player, pos)
                && player.mayUseItemAt(pos, Direction.UP, player.getMainHandItem());
    }
    
    @Override
    public boolean simulateBlockBreak(ServerPlayer player, Level level, BlockPos pos) {
        // 使用 ServerPlayerGameMode#destroyBlock 模拟玩家破坏方块
        // 这会触发所有相关事件和处理逻辑：
        // - BlockEvent.BreakEvent（可被保护模组取消）
        // - 正确的战利品表掉落
        // - 工具耐久度消耗
        // - 附魔效果（时运、精准采集等）
        // - 破坏音效和粒子效果
        
        try {
            return player.gameMode.destroyBlock(pos);
        } catch (Exception e) {
            OneKeyMiner.LOGGER.error("NeoForge 模拟方块破坏失败: {}", e.getMessage());
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
        // 模拟物品右键使用
        // 用于耕地、剥皮原木、制作土径等交互操作
        
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
            OneKeyMiner.LOGGER.error("NeoForge 模拟物品使用失败: {}", e.getMessage());
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
            // NeoForge patches Player#interactOn to dispatch its cancellable
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
                    "NeoForge 模拟实体交互失败，目标 {}",
                    target.getUUID(),
                    e
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
        // 配置界面在模组主类中通过 ModContainer 注册
        // 见 NeoForgeConfigScreen 类和 OneKeyMinerNeoForge 主类
    }
    
    @Override
    public boolean isChainModeActive(ServerPlayer player) {
        return org.xiyu.onekeyminer.mining.MiningStateManager.isHoldingKey(player);
    }
    
    @Override
    public void setChainModeActive(ServerPlayer player, boolean active) {
        org.xiyu.onekeyminer.mining.MiningStateManager.setHoldingKey(player, active);
        
        // 可选：发送消息给玩家
        OneKeyMiner.LOGGER.debug("玩家 {} 的链式模式已{}",
                player.getName().getString(),
                active ? "激活" : "关闭");
    }
    
    @Override
    public void sendChainActionMessage(ServerPlayer player, String actionType, int count) {
        // 向玩家发送链式操作完成消息
        // 使用 Action Bar 显示
        String key = "message.onekeyminer.chain_action." + actionType;
        
        Component message = Component.translatable(key, count);
        player.displayClientMessage(message, true); // true = Action Bar
    }
    
    /**
     * 清理玩家状态（玩家退出时调用）
     * 
     * @param playerUuid 玩家 UUID
     */
    public static void cleanupPlayer(UUID playerUuid) {
        org.xiyu.onekeyminer.mining.MiningStateManager.clearState(playerUuid);
    }
}
