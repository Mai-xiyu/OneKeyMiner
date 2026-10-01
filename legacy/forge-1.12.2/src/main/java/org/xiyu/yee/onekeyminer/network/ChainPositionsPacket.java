package org.xiyu.yee.onekeyminer.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.xiyu.yee.onekeyminer.Onekeyminer;
import org.xiyu.yee.onekeyminer.api.ChainPositionStore;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端 -> 客户端 同步玩家连锁位置信息包
 */
public class ChainPositionsPacket implements IOneKeyPacket, IMessage {
    private String playerUuid;
    private boolean active;
    private List<BlockPos> mining;
    private List<BlockPos> interaction;
    private List<BlockPos> planting;

    public ChainPositionsPacket() {
        // 无参构造函数必须存在
    }

    public ChainPositionsPacket(String playerUuid, boolean active, List<BlockPos> mining, List<BlockPos> interaction, List<BlockPos> planting) {
        this.playerUuid = playerUuid;
        this.active = active;
        this.mining = mining == null ? new ArrayList<>() : mining;
        this.interaction = interaction == null ? new ArrayList<>() : interaction;
        this.planting = planting == null ? new ArrayList<>() : planting;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        playerUuid = ByteBufUtils.readUTF8String(buf);
        active = buf.readBoolean();

        int m = buf.readInt();
        mining = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            mining.add(new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()));
        }

        int it = buf.readInt();
        interaction = new ArrayList<>();
        for (int i = 0; i < it; i++) {
            interaction.add(new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()));
        }

        int p = buf.readInt();
        planting = new ArrayList<>();
        for (int i = 0; i < p; i++) {
            planting.add(new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, playerUuid);
        buf.writeBoolean(active);

        buf.writeInt(mining.size());
        for (BlockPos b : mining) {
            buf.writeInt(b.getX());
            buf.writeInt(b.getY());
            buf.writeInt(b.getZ());
        }

        buf.writeInt(interaction.size());
        for (BlockPos b : interaction) {
            buf.writeInt(b.getX());
            buf.writeInt(b.getY());
            buf.writeInt(b.getZ());
        }

        buf.writeInt(planting.size());
        for (BlockPos b : planting) {
            buf.writeInt(b.getX());
            buf.writeInt(b.getY());
            buf.writeInt(b.getZ());
        }
    }

    @Override
    public void write(ByteBuf buf) {
        toBytes(buf);
    }

    @Override
    public ResourceLocation id() {
        return NetworkHandler.CHAIN_POSITIONS_ID;
    }

    @Override
    public Object type() {
        return NetworkHandler.CHAIN_POSITIONS_TYPE;
    }

    public static class Handler implements IMessageHandler<ChainPositionsPacket, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(ChainPositionsPacket message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                try {
                    ChainPositionStore.setChainMode(message.playerUuid, message.active);
                    ChainPositionStore.setMiningPositions(message.playerUuid, message.mining);
                    ChainPositionStore.setInteractionPositions(message.playerUuid, message.interaction);
                    ChainPositionStore.setPlantingPositions(message.playerUuid, message.planting);
                } catch (Exception e) {
                    Onekeyminer.LOGGER.error("处理 ChainPositionsPacket 时出错: {}", e.getMessage(), e);
                }
            });
            return null;
        }
    }
}
