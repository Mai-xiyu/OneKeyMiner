package org.xiyu.onekeyminer.neoforge;

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
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.platform.PlatformServices;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    
    /** 存储玩家链式模式状态（使用 UUID 作为 key） */
    private static final Map<UUID, Boolean> CHAIN_MODE_STATES = new ConcurrentHashMap<>();
    
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
        // NeoForge 使用 BlockEvent.BreakEvent 进行权限检查
        // 该事件可被取消，所以如果我们到达这里，权限检查已通过
        
        // 基础检查
        if (player.isSpectator()) {
            return false;
        }
        
        // 检查方块是否可被破坏（基岩等不可破坏方块）
        if (state.getDestroySpeed(level, pos) < 0 && !player.isCreative()) {
            return false;
        }
        
        // 使用 NeoForge 的 BlockEvent.BreakEvent 进行权限检查
        // 创建并触发事件来检查权限
        try {
            BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player);
            NeoForge.EVENT_BUS.post(event);
            return !event.isCanceled();
        } catch (Exception e) {
            OneKeyMiner.LOGGER.debug("权限检查事件触发失败: {}", e.getMessage());
            return true; // 默认允许
        }
    }
    
    @Override
    public boolean canPlayerInteract(ServerPlayer player, Level level, BlockPos pos, BlockState state) {
        // 基础检查
        if (player.isSpectator()) {
            return false;
        }
        
        // 使用破坏权限作为基础检查
        // 实际上交互权限可能与破坏权限不同，但这是安全的默认行为
        return canPlayerBreakBlock(player, level, pos, state);
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
            BlockPos pos,
            InteractionHand hand,
            ItemStack item
    ) {
        // 模拟物品右键使用
        // 用于耕地、剥皮原木、制作土径等交互操作
        
        try {
            // 创建点击结果
            BlockHitResult hitResult = new BlockHitResult(
                    Vec3.atCenterOf(pos),
                    Direction.UP,
                    pos,
                    false
            );
            
            // 创建使用上下文
            UseOnContext context = new UseOnContext(player, hand, hitResult);
            
            // 调用物品的 useOn 方法 - 这是原版的通用交互入口
            // 会触发 PlayerInteractEvent.RightClickBlock 等相关事件
            InteractionResult result = item.useOn(context);
            
            return result.consumesAction();
        } catch (Exception e) {
            OneKeyMiner.LOGGER.error("NeoForge 模拟物品使用失败: {}", e.getMessage());
            return false;
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
        // 始终使用按住按键激活模式，检查状态存储
        return CHAIN_MODE_STATES.getOrDefault(player.getUUID(), false);
    }
    
    @Override
    public void setChainModeActive(ServerPlayer player, boolean active) {
        // 设置玩家的链式模式状态
        CHAIN_MODE_STATES.put(player.getUUID(), active);
        
        // 同时更新 MiningStateManager 的按键状态（用于 MiningLogic 检查）
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
        String key = switch (actionType) {
            case "mining" -> "message.onekeyminer.chain_mining";
            case "interaction" -> "message.onekeyminer.chain_interaction";
            case "planting" -> "message.onekeyminer.chain_planting";
            default -> "message.onekeyminer.chain_action";
        };
        
        Component message = Component.translatable(key, count);
        player.displayClientMessage(message, true); // true = Action Bar
    }
    
    /**
     * 清理玩家状态（玩家退出时调用）
     * 
     * @param playerUuid 玩家 UUID
     */
    public static void cleanupPlayer(UUID playerUuid) {
        CHAIN_MODE_STATES.remove(playerUuid);
    }
}
