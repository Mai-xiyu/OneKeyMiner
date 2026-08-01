package org.xiyu.onekeyminer.fabric;

import net.minecraft.network.FriendlyByteBuf;
import org.xiyu.onekeyminer.network.ClientPreferenceAck;
import org.xiyu.onekeyminer.network.ClientPreferenceRequest;

/** Canonical Fabric 1.20.1 field order for both protocol directions. */
public final class FabricPreferenceCodec {
    private FabricPreferenceCodec() {
    }

    public static WireRequest readRequest(FriendlyByteBuf buffer) {
        WireRequest request = new WireRequest(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readUtf(FabricNetworkingIds.MAX_SHAPE_ID_LENGTH),
                buffer.readBoolean(),
                buffer.readBoolean()
        );
        requireFullyConsumed(buffer);
        return request;
    }

    public static void writeRequest(
            FriendlyByteBuf buffer,
            int sequence,
            ClientPreferenceRequest request
    ) {
        buffer.writeVarInt(FabricNetworkingIds.WIRE_VERSION);
        buffer.writeVarInt(sequence);
        buffer.writeBoolean(request.holding());
        buffer.writeUtf(request.shapeId(), FabricNetworkingIds.MAX_SHAPE_ID_LENGTH);
        buffer.writeBoolean(request.teleportDrops());
        buffer.writeBoolean(request.teleportExp());
    }

    public static ClientPreferenceAck readAck(FriendlyByteBuf buffer) {
        ClientPreferenceAck acknowledgement = new ClientPreferenceAck(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readUtf(FabricNetworkingIds.MAX_SHAPE_ID_LENGTH),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readVarInt()
        );
        requireFullyConsumed(buffer);
        return acknowledgement;
    }

    public static void writeAck(FriendlyByteBuf buffer, ClientPreferenceAck acknowledgement) {
        buffer.writeVarInt(acknowledgement.wireVersion());
        buffer.writeVarInt(acknowledgement.sequence());
        buffer.writeBoolean(acknowledgement.serverEnabled());
        buffer.writeUtf(
                acknowledgement.appliedShapeId(),
                FabricNetworkingIds.MAX_SHAPE_ID_LENGTH
        );
        buffer.writeVarInt(acknowledgement.maxBlocksApplied());
        buffer.writeVarInt(acknowledgement.maxDistanceApplied());
        buffer.writeBoolean(acknowledgement.allowDiagonalApplied());
        buffer.writeBoolean(acknowledgement.teleportDropsApplied());
        buffer.writeBoolean(acknowledgement.teleportExpApplied());
        buffer.writeVarInt(acknowledgement.capabilities());
    }

    private static void requireFullyConsumed(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Unexpected trailing preference payload bytes");
        }
    }

    public record WireRequest(
            int wireVersion,
            int sequence,
            boolean holding,
            String shapeId,
            boolean teleportDrops,
            boolean teleportExp
    ) {
        public WireRequest {
            if (shapeId == null
                    || shapeId.length() > FabricNetworkingIds.MAX_SHAPE_ID_LENGTH) {
                throw new IllegalArgumentException("Invalid shape id length");
            }
        }
    }
}
