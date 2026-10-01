package org.xiyu.yee.onekeyminer.capability;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
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
    public static boolean isChainModeActive(ServerPlayerEntity player) {
        // 兼容 1.16 的持久化玩家数据：存储在 player.getPersistentData().getCompound(PlayerEntity.PERSISTED_NBT_TAG)
        try {
            CompoundNBT root = player.getPersistentData();
            String tag = PlayerEntity.PERSISTED_NBT_TAG;
            if (root.contains(tag)) {
                CompoundNBT persisted = root.getCompound(tag);
                return persisted.getBoolean(CHAIN_MODE_TAG);
            }
        } catch (Exception e) {
            Onekeyminer.LOGGER.debug("检查连锁模式时读取持久化数据出错: {}", e.getMessage());
        }
        return false;
    }
    
    // 设置玩家的连锁挖掘状态
    public static void setChainMode(ServerPlayerEntity player, boolean active) {
        try {
            CompoundNBT root = player.getPersistentData();
            String tag = PlayerEntity.PERSISTED_NBT_TAG;
            CompoundNBT persisted = root.contains(tag) ? root.getCompound(tag) : new CompoundNBT();
            persisted.putBoolean(CHAIN_MODE_TAG, active);
            root.put(tag, persisted);
        } catch (Exception e) {
            Onekeyminer.LOGGER.error("设置连锁模式持久化数据时出错: {}", e.getMessage());
        }
    }
    
    // 在玩家死亡后复制链式挖掘状态
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        PlayerEntity originalPlayer = event.getOriginal();
        if (event.getEntity() instanceof PlayerEntity) {
            PlayerEntity newPlayer = (PlayerEntity) event.getEntity();
            // 只有在玩家死亡时才复制数据
            if (event.isWasDeath()) {
                // 从原始玩家的NBT中读取状态
                boolean wasChainActive = false;
                try {
                    CompoundNBT origRoot = originalPlayer.getPersistentData();
                    String tag = PlayerEntity.PERSISTED_NBT_TAG;
                    if (origRoot.contains(tag)) {
                        CompoundNBT origPersisted = origRoot.getCompound(tag);
                        wasChainActive = origPersisted.getBoolean(CHAIN_MODE_TAG);
                    }
                } catch (Exception e) {
                    Onekeyminer.LOGGER.error("读取原始玩家连锁模式数据时出错: {}", e.getMessage());
                }
                
                try {
                    CompoundNBT root = newPlayer.getPersistentData();
                    String tag = PlayerEntity.PERSISTED_NBT_TAG;
                    CompoundNBT persisted = root.contains(tag) ? root.getCompound(tag) : new CompoundNBT();
                    persisted.putBoolean(CHAIN_MODE_TAG, wasChainActive);
                    root.put(tag, persisted);
                } catch (Exception e) {
                    Onekeyminer.LOGGER.error("复制玩家连锁模式数据时出错: {}", e.getMessage());
                }
            }
        }
    }
} 