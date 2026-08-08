package org.xiyu.onekeyminer.network;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.mining.MiningStateManager;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Shared validation and policy application for every loader's wire adapter. */
public final class ClientPreferenceProtocol {
    /**
     * Version 4 adds the server's effective preview limits to the S2C ACK.
     * Keeping a distinct version prevents 1.6.6 peers from decoding the new layout.
     */
    public static final int WIRE_VERSION = 4;
    public static final int MAX_SHAPE_ID_LENGTH = 128;
    public static final int MAX_APPLIED_BLOCKS = 10_240;
    public static final int MAX_APPLIED_DISTANCE = 128;
    public static final int CAP_SHAPE_SELECTION = 1;
    public static final int CAP_TELEPORT_DROPS = 1 << 1;
    public static final int CAP_TELEPORT_EXP = 1 << 2;
    public static final int CAP_SERVER_PREVIEW_POLICY = 1 << 3;
    public static final int SUPPORTED_CAPABILITIES =
            CAP_SHAPE_SELECTION
                    | CAP_TELEPORT_DROPS
                    | CAP_TELEPORT_EXP
                    | CAP_SERVER_PREVIEW_POLICY;

    private static final PreferencePacketGuard PACKET_GUARD = new PreferencePacketGuard(600);
    private static final ConcurrentHashMap<UUID, AppliedSnapshot> APPLIED_SNAPSHOTS =
            new ConcurrentHashMap<>();

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
            ConfigManager.ServerPreferenceSnapshot serverPolicy,
            boolean requestedDrops,
            boolean requestedExperience
    ) {
        return new TeleportDecision(
                requestedDrops,
                requestedExperience,
                serverPolicy.allowClientTeleportDrops() && requestedDrops,
                serverPolicy.allowClientTeleportExp() && requestedExperience
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
        if (player == null) {
            return null;
        }
        var server = player.level().getServer();
        if (server == null) {
            return null;
        }
        long tick = server.getTickCount();
        UUID playerId = player.getUUID();
        if (!PACKET_GUARD.tryAcquire(playerId, tick)) {
            return null;
        }
        if (wireVersion != WIRE_VERSION || sequence <= 0) {
            if (PACKET_GUARD.shouldLogInvalid(playerId, tick)) {
                OneKeyMiner.LOGGER.warn(
                        "Ignoring invalid client preference packet from {} (wire={}, sequence={})",
                        player.getGameProfile().name(),
                        wireVersion,
                        sequence
                );
            }
            return null;
        }

        AppliedSnapshot previous = APPLIED_SNAPSHOTS.get(playerId);
        ClientPreferenceSequence.Admission admission = ClientPreferenceSequence.classify(
                previous == null ? 0 : previous.sequence(),
                sequence
        );
        if (admission == ClientPreferenceSequence.Admission.RETRY) {
            if (previous.matches(
                    holding,
                    requestedShapeId,
                    requestedTeleportDrops,
                    requestedTeleportExp
            )) {
                return previous.ack();
            }
            if (PACKET_GUARD.shouldLogInvalid(playerId, tick)) {
                OneKeyMiner.LOGGER.warn(
                        "Rejected preference sequence reuse with different contents from {}",
                        player.getGameProfile().name()
                );
            }
            return null;
        }
        if (admission == ClientPreferenceSequence.Admission.STALE) {
            if (PACKET_GUARD.shouldLogInvalid(playerId, tick)) {
                OneKeyMiner.LOGGER.warn(
                        "Ignoring stale preference sequence {} from {}",
                        sequence,
                        player.getGameProfile().name()
                );
            }
            return null;
        }

        Identifier shapeId = requestedShapeId == null
                ? null
                : Identifier.tryParse(requestedShapeId);
        if (shapeId == null || !ShapeRegistry.isRegistered(shapeId)) {
            if (PACKET_GUARD.shouldLogInvalid(playerId, tick)) {
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
        TeleportDecision teleport = decideTeleport(
                serverPolicy,
                requestedTeleportDrops,
                requestedTeleportExp
        );
        MiningStateManager.updatePreferences(
                playerId,
                holding,
                shapeId,
                teleport.requestedDrops(),
                teleport.requestedExperience()
        );
        ClientPreferenceAck acknowledgement = new ClientPreferenceAck(
                WIRE_VERSION,
                sequence,
                serverPolicy.enabled(),
                shapeId.toString(),
                serverPolicy.maxBlocksFor(player.isCreative()),
                serverPolicy.maxDistance(),
                serverPolicy.allowDiagonal(),
                teleport.appliedDrops(),
                teleport.appliedExperience(),
                SUPPORTED_CAPABILITIES
        );
        APPLIED_SNAPSHOTS.put(playerId, new AppliedSnapshot(
                sequence,
                holding,
                requestedShapeId,
                requestedTeleportDrops,
                requestedTeleportExp,
                acknowledgement
        ));
        return acknowledgement;
    }

    public static void clearPlayer(UUID playerId) {
        PACKET_GUARD.clear(playerId);
        APPLIED_SNAPSHOTS.remove(playerId);
    }

    public static void clearAll() {
        PACKET_GUARD.clearAll();
        APPLIED_SNAPSHOTS.clear();
    }

    private record AppliedSnapshot(
            int sequence,
            boolean holding,
            String requestedShapeId,
            boolean requestedTeleportDrops,
            boolean requestedTeleportExp,
            ClientPreferenceAck ack
    ) {
        private boolean matches(
                boolean candidateHolding,
                String candidateShapeId,
                boolean candidateTeleportDrops,
                boolean candidateTeleportExp
        ) {
            return holding == candidateHolding
                    && Objects.equals(requestedShapeId, candidateShapeId)
                    && requestedTeleportDrops == candidateTeleportDrops
                    && requestedTeleportExp == candidateTeleportExp;
        }
    }
}
