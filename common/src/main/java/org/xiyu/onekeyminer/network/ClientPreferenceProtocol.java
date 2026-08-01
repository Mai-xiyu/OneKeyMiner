package org.xiyu.onekeyminer.network;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.MinerConfig;
import org.xiyu.onekeyminer.mining.MiningStateManager;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

/** Shared validation and policy application for every loader's wire adapter. */
public final class ClientPreferenceProtocol {
    public static final int WIRE_VERSION = 3;
    public static final int CAP_SHAPE_SELECTION = 1;
    public static final int CAP_TELEPORT_DROPS = 1 << 1;
    public static final int CAP_TELEPORT_EXP = 1 << 2;
    public static final int SUPPORTED_CAPABILITIES =
            CAP_SHAPE_SELECTION | CAP_TELEPORT_DROPS | CAP_TELEPORT_EXP;

    private static final PreferencePacketGuard PACKET_GUARD = new PreferencePacketGuard(200);

    private ClientPreferenceProtocol() {
    }

    record TeleportDecision(
            boolean requestedDrops,
            boolean requestedExperience,
            boolean appliedDrops,
            boolean appliedExperience
    ) {
    }

    static TeleportDecision decideTeleport(
            MinerConfig serverConfig,
            boolean requestedDrops,
            boolean requestedExperience
    ) {
        return new TeleportDecision(
                requestedDrops,
                requestedExperience,
                serverConfig.isDropTeleportEnabled(requestedDrops),
                serverConfig.isExperienceTeleportEnabled(requestedExperience)
        );
    }

    /** Returns {@code null} when the packet is rejected and must not be acknowledged. */
    public static ClientPreferenceAck applyOnServer(
            ServerPlayer player,
            int wireVersion,
            int sequence,
            boolean holding,
            String requestedShapeId,
            boolean requestedTeleportDrops,
            boolean requestedTeleportExp
    ) {
        long tick = player.level().getServer().getTickCount();
        if (!PACKET_GUARD.tryAcquire(player.getUUID(), tick)) {
            return null;
        }
        if (wireVersion != WIRE_VERSION || sequence <= 0) {
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

        MinerConfig serverConfig = ConfigManager.getConfigSnapshot();
        TeleportDecision teleport = decideTeleport(
                serverConfig,
                requestedTeleportDrops,
                requestedTeleportExp
        );
        MiningStateManager.updatePreferences(
                player.getUUID(),
                holding,
                shapeId,
                teleport.requestedDrops(),
                teleport.requestedExperience()
        );
        return new ClientPreferenceAck(
                WIRE_VERSION,
                sequence,
                shapeId.toString(),
                teleport.appliedDrops(),
                teleport.appliedExperience(),
                SUPPORTED_CAPABILITIES
        );
    }

    public static void clearPlayer(java.util.UUID playerId) {
        PACKET_GUARD.clear(playerId);
    }

    public static void clearAll() {
        PACKET_GUARD.clearAll();
    }
}
