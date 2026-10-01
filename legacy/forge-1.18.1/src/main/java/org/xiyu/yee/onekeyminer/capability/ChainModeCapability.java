package org.xiyu.yee.onekeyminer.capability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import org.xiyu.yee.onekeyminer.Onekeyminer;

public class ChainModeCapability {
    private static final String CHAIN_MODE_TAG = Onekeyminer.MODID + ":chain_mode";

    // 初始化方法，在主类构造函数中调用


    /**
     * 检查连锁模式是否激活
     */
    public static boolean isChainModeActive(Player player) {
        // In 1.20.1 Player#getPersistentData returns a CompoundTag where
        // getBoolean(key) directly returns a boolean
        return player.getPersistentData().getBoolean(CHAIN_MODE_TAG);
    }
    
    // 设置玩家的连锁挖掘状态
    public static void setChainMode(ServerPlayer player, boolean active) {
        player.getPersistentData().putBoolean(CHAIN_MODE_TAG, active);
    }
    
    // 在玩家死亡后复制链式挖掘状态
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player originalPlayer = event.getOriginal();
        if (event.getEntity() instanceof Player newPlayer) {
            // 只有在玩家死亡时才复制数据
            if (event.isWasDeath()) {
                boolean wasChainActive = isChainModeActive(originalPlayer);
                newPlayer.getPersistentData().putBoolean(CHAIN_MODE_TAG, wasChainActive);
            }
        }
    }
} 