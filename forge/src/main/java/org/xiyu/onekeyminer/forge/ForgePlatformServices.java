package org.xiyu.onekeyminer.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.platform.PlatformServices;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    
    /** 玩家链式模式状态存储 */
    private static final Map<UUID, Boolean> CHAIN_MODE_STATES = new ConcurrentHashMap<>();
    
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
        if (player.isSpectator()) {
            return false;
        }
        
        // 检查方块是否可被破坏
        if (state.getDestroySpeed(level, pos) < 0) {
            return false;
        }
        
        // TODO: 集成保护模组
        
        return true;
    }
    
    @Override
    public boolean canPlayerInteract(ServerPlayer player, Level level, BlockPos pos, BlockState state) {
        // 检查玩家是否可以与方块交互
        
        // 检查玩家是否是旁观者模式
        if (player.isSpectator()) {
            return false;
        }
        
        // 检查玩家是否在创造模式或有管理员权限
        if (player.isCreative() || player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return true;
        }
        
        // TODO: 集成保护模组的交互权限检查
        
        return true;
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
            OneKeyMiner.LOGGER.error("Forge 模拟物品使用失败: {}", e.getMessage());
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
        return ModList.get().isLoaded(modId);
    }
    
    @Override
    public void registerConfigScreen() {
        // 配置界面在模组主类中通过 ModLoadingContext 注册
        // 见 ForgeConfigScreen 类
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
