package org.xiyu.yee.onekeyminer.capability;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import org.xiyu.yee.onekeyminer.Onekeyminer;

public class ChainModeCapability {
    private static final String CHAIN_MODE_TAG = Onekeyminer.MODID + ":chain_mode";

    /**
     * 检查连锁模式是否激活
     */
    public static boolean isChainModeActive(EntityPlayerMP player) {
        // 在1.12.2中使用getEntityData()获取持久化数据
        try {
            NBTTagCompound data = player.getEntityData();
            if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
                data.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
            }
            NBTTagCompound persisted = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
            return persisted.getBoolean(CHAIN_MODE_TAG);
        } catch (Exception e) {
            Onekeyminer.LOGGER.debug("检查连锁模式时读取持久化数据出错: {}", e.getMessage());
        }
        return false;
    }
    
    // 设置玩家的连锁挖掘状态
    public static void setChainMode(EntityPlayerMP player, boolean active) {
        try {
            NBTTagCompound data = player.getEntityData();
            if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
                data.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
            }
            NBTTagCompound persisted = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
            persisted.setBoolean(CHAIN_MODE_TAG, active);
            data.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        } catch (Exception e) {
            Onekeyminer.LOGGER.error("设置连锁模式持久化数据时出错: {}", e.getMessage());
        }
    }
    
    // 在玩家死亡后复制链式挖掘状态
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        EntityPlayer originalPlayer = event.getOriginal();
        if (event.getEntityPlayer() instanceof EntityPlayer) {
            EntityPlayer newPlayer = event.getEntityPlayer();
            // 只有在玩家死亡时才复制数据
            if (event.isWasDeath()) {
                // 从原始玩家的NBT中读取状态
                boolean wasChainActive = false;
                try {
                    NBTTagCompound origData = originalPlayer.getEntityData();
                    if (origData.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
                        NBTTagCompound origPersisted = origData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
                        wasChainActive = origPersisted.getBoolean(CHAIN_MODE_TAG);
                    }
                } catch (Exception e) {
                    Onekeyminer.LOGGER.error("读取原始玩家连锁模式数据时出错: {}", e.getMessage());
                }
                
                try {
                    NBTTagCompound data = newPlayer.getEntityData();
                    if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
                        data.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
                    }
                    NBTTagCompound persisted = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
                    persisted.setBoolean(CHAIN_MODE_TAG, wasChainActive);
                    data.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
                } catch (Exception e) {
                    Onekeyminer.LOGGER.error("复制玩家连锁模式数据时出错: {}", e.getMessage());
                }
            }
        }
    }
} 