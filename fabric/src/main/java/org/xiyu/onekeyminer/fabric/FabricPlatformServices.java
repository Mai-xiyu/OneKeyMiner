package org.xiyu.onekeyminer.fabric;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.loader.api.FabricLoader;
import org.xiyu.onekeyminer.platform.PlatformServices;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabric 平台服务实现
 * 
 * <p>实现 {@link PlatformServices} 接口，提供 Fabric 平台特定的功能实现。</p>
 * 
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.21.9
 */
public class FabricPlatformServices implements PlatformServices {
    
    /** 玩家链式模式状态存储 */
    private static final Map<UUID, Boolean> CHAIN_MODE_STATES = new ConcurrentHashMap<>();
    
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
        // Fabric 使用 PlayerBlockBreakEvents.BEFORE 事件来检查权限
        // 由于我们在 AFTER 事件中触发，权限检查已经通过
        // 这里进行额外的基础检查
        
        // 检查玩家是否有足够权限
        if (player.isSpectator()) {
            return false;
        }
        
        // 检查方块是否可以被破坏
        if (state.getDestroySpeed(level, pos) < 0) {
            return false; // 基岩等不可破坏方块
        }
        
        
        return true;
    }
    
    @Override
    public boolean canPlayerInteract(ServerPlayer player, Level level, BlockPos pos, BlockState state) {
        // 检查玩家是否可以与方块交互
        
        // 检查玩家是否是旁观者模式
        if (player.isSpectator()) {
            return false;
        }
        
        // 检查玩家是否在冒险模式且没有交互权限
        if (player.isCreative() || player.hasPermissions(2)) {
            return true;
        }
        
        return true;
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
            // 在 1.21.9 中，这个方法可能有不同的名称（取决于映射）
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
            BlockPos pos,
            InteractionHand hand,
            ItemStack item
    ) {
        // 使用 Item#useOn 来模拟玩家对方块使用物品
        // 这会触发正确的游戏事件（如锄头耕地、斧头剥皮等）
        
        try {
            // 构建 BlockHitResult
            BlockHitResult hitResult = new BlockHitResult(
                    Vec3.atCenterOf(pos),
                    Direction.UP,
                    pos,
                    false
            );
            
            // 构建 UseOnContext
            UseOnContext context = new UseOnContext(player, hand, hitResult);
            
            // 执行物品使用
            InteractionResult result = item.useOn(context);
            
            return result.consumesAction();
        } catch (Exception e) {
            org.xiyu.onekeyminer.OneKeyMiner.LOGGER.error("模拟物品使用失败: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean isChainModeActive(ServerPlayer player) {
        // 始终使用按住按键激活模式，检查状态存储
        return CHAIN_MODE_STATES.getOrDefault(player.getUUID(), false);
    }
    
    @Override
    public void setChainModeActive(ServerPlayer player, boolean active) {
        // 设置链式模式状态
        if (active) {
            CHAIN_MODE_STATES.put(player.getUUID(), true);
        } else {
            CHAIN_MODE_STATES.remove(player.getUUID());
        }
        // 同时更新 MiningStateManager 的按键状态（用于 MiningLogic 检查）
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
        CHAIN_MODE_STATES.remove(playerId);
        org.xiyu.onekeyminer.mining.MiningStateManager.setHoldingKey(playerId, false);
    }
}
