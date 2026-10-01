package org.xiyu.yee.onekeyminer.network;
import net.minecraft.network.PacketBuffer;


import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;
import org.xiyu.yee.onekeyminer.Onekeyminer;
import org.xiyu.yee.onekeyminer.api.ChainPositionStore;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端 -> 客户端 同步玩家连锁位置信息包
 */
public class ChainPositionsPacket implements IOneKeyPacket {
    private final String playerUuid;
    private final boolean active;
    private final List<BlockPos> mining;
    private final List<BlockPos> interaction;
    private final List<BlockPos> planting;

    public ChainPositionsPacket(String playerUuid, boolean active, List<BlockPos> mining, List<BlockPos> interaction, List<BlockPos> planting) {
        this.playerUuid = playerUuid;
        this.active = active;
        this.mining = mining == null ? new ArrayList<>() : mining;
        this.interaction = interaction == null ? new ArrayList<>() : interaction;
        this.planting = planting == null ? new ArrayList<>() : planting;
    }

    public static ChainPositionsPacket fromNetwork(net.minecraft.network.PacketBuffer buf) {
        String uuid = buf.readUtf();
        boolean active = buf.readBoolean();

        int m = buf.readInt();
        List<BlockPos> mining = new ArrayList<>();
        for (int i = 0; i < m; i++) mining.add(buf.readBlockPos());

        int it = buf.readInt();
        List<BlockPos> interaction = new ArrayList<>();
        for (int i = 0; i < it; i++) interaction.add(buf.readBlockPos());

        int p = buf.readInt();
        List<BlockPos> planting = new ArrayList<>();
        for (int i = 0; i < p; i++) planting.add(buf.readBlockPos());

        return new ChainPositionsPacket(uuid, active, mining, interaction, planting);
    }

    @Override
    public void write(net.minecraft.network.PacketBuffer buf) {
        buf.writeUtf(playerUuid);
        buf.writeBoolean(active);

        buf.writeInt(mining.size());
        for (BlockPos b : mining) buf.writeBlockPos(b);

        buf.writeInt(interaction.size());
        for (BlockPos b : interaction) buf.writeBlockPos(b);

        buf.writeInt(planting.size());
        for (BlockPos b : planting) buf.writeBlockPos(b);
    }

    @Override
    public ResourceLocation id() {
        return NetworkHandler.CHAIN_POSITIONS_ID;
    }

    @Override
    public Object type() {
        return NetworkHandler.CHAIN_POSITIONS_TYPE;
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleOnClient(ChainPositionsPacket packet, net.minecraftforge.fml.network.NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            try {
                ChainPositionStore.setChainMode(packet.playerUuid, packet.active);
                ChainPositionStore.setMiningPositions(packet.playerUuid, packet.mining);
                ChainPositionStore.setInteractionPositions(packet.playerUuid, packet.interaction);
                ChainPositionStore.setPlantingPositions(packet.playerUuid, packet.planting);
            } catch (Exception e) {
                Onekeyminer.LOGGER.error("处理 ChainPositionsPacket 时出错: {}", e.getMessage(), e);
            }
        });
        context.setPacketHandled(true);
    }
}
