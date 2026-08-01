package org.xiyu.onekeyminer.network;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.mining.MiningStateManager;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

import java.util.UUID;

/** Shared validation and policy application for every loader's wire adapter. */
public final class ClientPreferenceServer {
    private static final PreferencePacketGuard PACKET_GUARD = new PreferencePacketGuard(600);

    private ClientPreferenceServer() {
    }

    /** Returns {@code null} when the packet is rejected and must not be acknowledged. */
    public static ClientPreferenceAck apply(
            ServerPlayer player,
            int wireVersion,
            int sequence,
            boolean holding,
            String requestedShapeId,
            boolean requestedTeleportDrops,
            boolean requestedTeleportExp
    ) {
        var server = player.level().getServer();
        if (server == null) {
            return null;
        }
        long tick = server.getTickCount();
        if (!PACKET_GUARD.tryAcquire(player.getUUID(), tick)) {
            return null;
        }
        if (wireVersion != ClientPreferenceProtocol.WIRE_VERSION || sequence <= 0) {
            if (PACKET_GUARD.shouldLogInvalid(player.getUUID(), tick)) {
                OneKeyMiner.LOGGER.warn(
                        "Ignoring invalid client preference packet from {} (wire={}, sequence={})",
                        player.getGameProfile().name(),
                        wireVersion,
                        sequence
                );
            }
            return null;
        }

        Identifier shapeId = Identifier.tryParse(requestedShapeId);
        if (shapeId == null || !ShapeRegistry.isRegistered(shapeId)) {
            if (PACKET_GUARD.shouldLogInvalid(player.getUUID(), tick)) {
                OneKeyMiner.LOGGER.warn(
                        "Replacing invalid shape preference '{}' from {} with the server default",
                        requestedShapeId,
                        player.getGameProfile().name()
                );
            }
            shapeId = ShapeRegistry.DEFAULT_SHAPE_ID;
            if (!ShapeRegistry.isRegistered(shapeId)) {
                return null;
            }
        }

        ConfigManager.ServerPreferenceSnapshot serverPolicy =
                ConfigManager.getServerPreferenceSnapshot();
        ServerPreferencePolicy.Result policy = ServerPreferencePolicy.apply(
                requestedTeleportDrops,
                requestedTeleportExp,
                serverPolicy.allowClientTeleportDrops(),
                serverPolicy.allowClientTeleportExp()
        );
        MiningStateManager.updatePreferences(
                player.getUUID(),
                holding,
                shapeId,
                policy.teleportDropsRequested(),
                policy.teleportExpRequested()
        );
        return new ClientPreferenceAck(
                ClientPreferenceProtocol.WIRE_VERSION,
                sequence,
                serverPolicy.enabled(),
                shapeId.toString(),
                serverPolicy.maxBlocksFor(player.isCreative()),
                serverPolicy.maxDistance(),
                serverPolicy.allowDiagonal(),
                policy.teleportDropsApplied(),
                policy.teleportExpApplied(),
                policy.capabilities()
        );
    }

    public static void clearPlayer(UUID playerId) {
        PACKET_GUARD.clear(playerId);
    }

    public static void clearAll() {
        PACKET_GUARD.clearAll();
    }
}
