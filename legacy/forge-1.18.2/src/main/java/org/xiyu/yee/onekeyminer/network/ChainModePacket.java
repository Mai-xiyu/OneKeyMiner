package org.xiyu.yee.onekeyminer.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
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
    public static ChainModePacket fromNetwork(FriendlyByteBuf buf) {
        return new ChainModePacket(buf.readBoolean());
    }
    
    // 私有构造函数，避免构造函数歧义
//    private ChainModePacket(FriendlyByteBuf buf) {
//        this.active = buf.readBoolean();
//    }
    
    @Override
    public void write(FriendlyByteBuf buf) {
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
    
    public static void handleOnServer(ChainModePacket packet, NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer serverPlayer = context.getSender();
                // 设置玩家的连锁模式状态
                ChainModeCapability.setChainMode(serverPlayer, packet.active);

                try {
                    String key = serverPlayer.getUUID().toString();

                    if (packet.active) {
                        // 每次收到激活消息时都重新计算位置（实现实时更新）
                        // 尝试获取玩家视线下的方块作为计算起点
                        HitResult hr = serverPlayer.pick(6.0D, 0.0F, false);
                        if (hr != null && hr.getType() == HitResult.Type.BLOCK) {
                            BlockHitResult bhr = (BlockHitResult) hr;
                            BlockPos pos = bhr.getBlockPos();
                            BlockState state = serverPlayer.level.getBlockState(pos);
                            ItemStack held = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);

                            // 计算并写入三类 positions，供附属模组读取
                            List<BlockPos> mining = ChainAPI.computeMiningPositions(serverPlayer, (ServerLevel) serverPlayer.level, pos, state, held);
                            ChainPositionStore.setMiningPositions(key, mining);

                            List<BlockPos> interaction = ChainAPI.computeInteractionPositions(serverPlayer, (ServerLevel) serverPlayer.level, pos, state, InteractionHand.MAIN_HAND);
                            ChainPositionStore.setInteractionPositions(key, interaction);

                            List<BlockPos> planting = ChainAPI.computePlantingPositions(serverPlayer, (ServerLevel) serverPlayer.level, pos, state, held);
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
    public static void handleOnClient(ChainModePacket packet, NetworkEvent.Context context) {
        Minecraft minecraft = Minecraft.getInstance();
        context.enqueueWork(() -> {
            if (minecraft.player == null) return;

            minecraft.player.displayClientMessage(new net.minecraft.network.chat.TranslatableComponent(
                packet.active ? "message.onekeyminer.chain_mode_on" : "message.onekeyminer.chain_mode_off"), true);
        });
        context.setPacketHandled(true);
    }
} 