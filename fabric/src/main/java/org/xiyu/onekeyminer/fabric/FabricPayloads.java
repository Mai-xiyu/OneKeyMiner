package org.xiyu.onekeyminer.fabric;

import net.minecraft.network.FriendlyByteBuf;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;

/** Canonical bounded codecs for Fabric's legacy custom-payload API. */
public final class FabricPayloads {
    private FabricPayloads() {
    }

    public record ClientPreferences(
            int wireVersion,
            int sequence,
            boolean holding,
            String shapeId,
            boolean teleportDrops,
            boolean teleportExp
    ) {
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(wireVersion);
            buf.writeVarInt(sequence);
            buf.writeBoolean(holding);
            buf.writeUtf(shapeId, FabricNetworkingIds.MAX_SHAPE_ID_LENGTH);
            buf.writeBoolean(teleportDrops);
            buf.writeBoolean(teleportExp);
        }

        public static ClientPreferences read(FriendlyByteBuf buf) {
            return new ClientPreferences(
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readBoolean(),
                    buf.readUtf(FabricNetworkingIds.MAX_SHAPE_ID_LENGTH),
                    buf.readBoolean(),
                    buf.readBoolean()
            );
        }
    }

    public record ServerPreferencesAck(
            int wireVersion,
            int sequence,
            boolean serverEnabled,
            String appliedShapeId,
            int maxBlocksApplied,
            int maxDistanceApplied,
            boolean allowDiagonalApplied,
            boolean teleportDropsApplied,
            boolean teleportExpApplied,
            int capabilities
    ) {
        public ServerPreferencesAck(ClientPreferenceAck ack) {
            this(
                    ack.wireVersion(),
                    ack.sequence(),
                    ack.serverEnabled(),
                    ack.appliedShapeId(),
                    ack.maxBlocksApplied(),
                    ack.maxDistanceApplied(),
                    ack.allowDiagonalApplied(),
                    ack.teleportDropsApplied(),
                    ack.teleportExpApplied(),
                    ack.capabilities()
            );
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(wireVersion);
            buf.writeVarInt(sequence);
            buf.writeBoolean(serverEnabled);
            buf.writeUtf(appliedShapeId, FabricNetworkingIds.MAX_SHAPE_ID_LENGTH);
            buf.writeVarInt(maxBlocksApplied);
            buf.writeVarInt(maxDistanceApplied);
            buf.writeBoolean(allowDiagonalApplied);
            buf.writeBoolean(teleportDropsApplied);
            buf.writeBoolean(teleportExpApplied);
            buf.writeVarInt(capabilities);
        }

        public static ServerPreferencesAck read(FriendlyByteBuf buf) {
            return new ServerPreferencesAck(
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readBoolean(),
                    buf.readUtf(FabricNetworkingIds.MAX_SHAPE_ID_LENGTH),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readVarInt()
            );
        }

        public ClientPreferenceAck toCommon() {
            return new ClientPreferenceAck(
                    wireVersion,
                    sequence,
                    serverEnabled,
                    appliedShapeId,
                    maxBlocksApplied,
                    maxDistanceApplied,
                    allowDiagonalApplied,
                    teleportDropsApplied,
                    teleportExpApplied,
                    capabilities
            );
        }
    }
}
