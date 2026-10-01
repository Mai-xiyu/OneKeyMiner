package org.xiyu.yee.onekeyminer.network;
import net.minecraft.network.PacketBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.item.ItemStack;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;
import org.xiyu.yee.onekeyminer.Onekeyminer;
import org.xiyu.yee.onekeyminer.api.ChainAPI;
import org.xiyu.yee.onekeyminer.api.ChainPositionStore;
import org.xiyu.yee.onekeyminer.capability.ChainModeCapability;

import java.util.List;

/**
 * 连锁模式切换消息数据包
 */
public class ChainModePacket implements IOneKeyPacket {
    private final boolean active;
    
    // 客户端创建时使用
    public ChainModePacket(boolean active) {
        this.active = active;
    }
    
    // 从缓冲区读取时使用，添加构造函数标识符避免歧义
    public static ChainModePacket fromNetwork(net.minecraft.network.PacketBuffer buf) {
        return new ChainModePacket(buf.readBoolean());
    }
    
    // 私有构造函数，避免构造函数歧义
//    private ChainModePacket(net.minecraft.network.PacketBuffer buf) {
//        this.active = buf.readBoolean();
//    }
    
    @Override
    public void write(net.minecraft.network.PacketBuffer buf) {
        buf.writeBoolean(active);
    }
    
    @Override
    public ResourceLocation id() {
        return NetworkHandler.CHAIN_MODE_ID;
    }
    
    @Override
    public Object type() {
        return NetworkHandler.CHAIN_MODE_TYPE;
    }
    
    public static void handleOnServer(ChainModePacket packet, net.minecraftforge.fml.network.NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayerEntity serverPlayer = context.getSender();
                // 设置玩家的连锁模式状态
                ChainModeCapability.setChainMode(serverPlayer, packet.active);

                try {
                    String key = serverPlayer.getUUID().toString();

                    if (packet.active) {
                        // 每次收到激活消息时都重新计算位置（实现实时更新）
                        // 尝试获取玩家视线下的方块作为计算起点
                        RayTraceResult hr = serverPlayer.pick(6.0D, 0.0F, false);
                        if (hr != null && hr.getType() == RayTraceResult.Type.BLOCK) {
                            BlockRayTraceResult bhr = (BlockRayTraceResult) hr;
                            BlockPos pos = bhr.getBlockPos();
                            BlockState state = serverPlayer.level.getBlockState(pos);
                            ItemStack held = serverPlayer.getItemInHand(Hand.MAIN_HAND);

                            // 计算并写入三类 positions，供附属模组读取
                            List<BlockPos> mining = ChainAPI.computeMiningPositions(serverPlayer, (ServerWorld) serverPlayer.level, pos, state, held);
                            ChainPositionStore.setMiningPositions(key, mining);

                            List<BlockPos> interaction = ChainAPI.computeInteractionPositions(serverPlayer, (ServerWorld) serverPlayer.level, pos, state, Hand.MAIN_HAND);
                            ChainPositionStore.setInteractionPositions(key, interaction);

                            List<BlockPos> planting = ChainAPI.computePlantingPositions(serverPlayer, (ServerWorld) serverPlayer.level, pos, state, held);
                            ChainPositionStore.setPlantingPositions(key, planting);

                            // 将结果打包并发送给玩家（客户端将更新本地 ChainPositionStore）
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
        context.setPacketHandled(true);
    }
    
    @OnlyIn(Dist.CLIENT)
    public static void handleOnClient(ChainModePacket packet, net.minecraftforge.fml.network.NetworkEvent.Context context) {
        Minecraft minecraft = Minecraft.getInstance();
        context.enqueueWork(() -> {
            if (minecraft.player == null) return;

            minecraft.player.displayClientMessage(new TranslationTextComponent(
                packet.active ? "message.onekeyminer.chain_mode_on" : "message.onekeyminer.chain_mode_off"), true);
        });
        context.setPacketHandled(true);
    }
} 