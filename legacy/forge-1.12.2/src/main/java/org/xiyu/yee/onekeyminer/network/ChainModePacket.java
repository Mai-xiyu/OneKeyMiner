package org.xiyu.yee.onekeyminer.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.xiyu.yee.onekeyminer.Onekeyminer;
import org.xiyu.yee.onekeyminer.api.ChainAPI;
import org.xiyu.yee.onekeyminer.api.ChainPositionStore;
import org.xiyu.yee.onekeyminer.capability.ChainModeCapability;

import java.util.List;

/**
 * 连锁模式切换消息数据包
 */
public class ChainModePacket implements IOneKeyPacket, IMessage {
    private boolean active;
    
    public ChainModePacket() {
        // 无参构造函数必须存在
    }
    
    // 客户端创建时使用
    public ChainModePacket(boolean active) {
        this.active = active;
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        active = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(active);
    }
    
    @Override
    public void write(ByteBuf buf) {
        toBytes(buf);
    }
    
    @Override
    public ResourceLocation id() {
        return NetworkHandler.CHAIN_MODE_ID;
    }
    
    @Override
    public Object type() {
        return NetworkHandler.CHAIN_MODE_TYPE;
    }
    
    public static class Handler implements IMessageHandler<ChainModePacket, IMessage> {
        @Override
        public IMessage onMessage(ChainModePacket message, MessageContext ctx) {
            EntityPlayerMP serverPlayer = ctx.getServerHandler().player;
            serverPlayer.getServerWorld().addScheduledTask(() -> {
                // 设置玩家的连锁模式状态
                ChainModeCapability.setChainMode(serverPlayer, message.active);

                try {
                    String key = serverPlayer.getUniqueID().toString();

                    if (message.active) {
                        // 每次收到激活消息时都重新计算位置(实现实时更新)
                        // 尝试获取玩家视线下的方块作为计算起点
                        RayTraceResult hr = serverPlayer.rayTrace(6.0D, 0.0F);
                        if (hr != null && hr.typeOfHit == RayTraceResult.Type.BLOCK) {
                            BlockPos pos = hr.getBlockPos();
                            IBlockState state = serverPlayer.world.getBlockState(pos);
                            ItemStack held = serverPlayer.getHeldItem(EnumHand.MAIN_HAND);

                            // 计算并写入三类 positions,供附属模组读取
                            List<BlockPos> mining = ChainAPI.computeMiningPositions(serverPlayer, (WorldServer) serverPlayer.world, pos, state, held);
                            ChainPositionStore.setMiningPositions(key, mining);

                            List<BlockPos> interaction = ChainAPI.computeInteractionPositions(serverPlayer, (WorldServer) serverPlayer.world, pos, state, EnumHand.MAIN_HAND);
                            ChainPositionStore.setInteractionPositions(key, interaction);

                            List<BlockPos> planting = ChainAPI.computePlantingPositions(serverPlayer, (WorldServer) serverPlayer.world, pos, state, held);
                            ChainPositionStore.setPlantingPositions(key, planting);

                            // 将结果打包并发送给玩家(客户端将更新本地 ChainPositionStore)
                            ChainPositionsPacket sync = new ChainPositionsPacket(key, true, mining, interaction, planting);
                            NetworkHandler.sendToPlayer(sync, serverPlayer);
                        }
                    } else {
                        // 关闭时清理缓存
                        ChainPositionStore.clearAll(key);
                        // 发送关闭同步包
                        ChainPositionsPacket sync = new ChainPositionsPacket(key, false, null, null, null);
                        NetworkHandler.sendToPlayer(sync, serverPlayer);
                    }
                } catch (Exception e) {
                    Onekeyminer.LOGGER.error("处理 ChainModePacket 时计算 positions 出错: {}", e.getMessage(), e);
                }
            });
            return null;
        }
    }
} 