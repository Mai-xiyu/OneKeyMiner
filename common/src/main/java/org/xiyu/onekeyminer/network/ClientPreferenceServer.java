package org.xiyu.onekeyminer.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.mining.MiningStateManager;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

import java.util.UUID;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Shared validation and authoritative policy application for both wire adapters. */
public final class ClientPreferenceServer {
    private static final PreferencePacketGuard PACKET_GUARD = new PreferencePacketGuard(600);
    private static final ConcurrentHashMap<UUID, AppliedSnapshot> APPLIED_SNAPSHOTS =
            new ConcurrentHashMap<>();

    private ClientPreferenceServer() {
    }

    /** Returns {@code null} for a rejected packet, which must not be acknowledged. */
    public static ClientPreferenceAck apply(
            ServerPlayer player,
            int wireVersion,
            int sequence,
            boolean holding,
            String requestedShapeId,
            boolean requestedTeleportDrops,
            boolean requestedTeleportExp
    ) {
        if (player == null || player.getServer() == null) {
            return null;
        }
        long tick = player.getServer().getTickCount();
        UUID playerId = player.getUUID();
        if (!PACKET_GUARD.tryAcquire(playerId, tick)) {
            return null;
        }
        if (wireVersion != ClientPreferenceProtocol.WIRE_VERSION || sequence <= 0) {
            if (PACKET_GUARD.shouldLogInvalid(playerId, tick)) {
                OneKeyMiner.LOGGER.warn(
                        "Ignoring invalid preference packet from {} (wire={}, sequence={})",
                        player.getGameProfile().getName(), wireVersion, sequence
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
                        player.getGameProfile().getName()
                );
            }
            return null;
        }
        if (admission == ClientPreferenceSequence.Admission.STALE) {
            if (PACKET_GUARD.shouldLogInvalid(playerId, tick)) {
                OneKeyMiner.LOGGER.warn(
                        "Ignoring stale preference sequence {} from {}",
                        sequence,
                        player.getGameProfile().getName()
                );
            }
            return null;
        }

        ResourceLocation shapeId = requestedShapeId == null
                ? null
                : ResourceLocation.tryParse(requestedShapeId);
        if (!ShapeRegistry.isRegistered(shapeId)) {
            if (PACKET_GUARD.shouldLogInvalid(playerId, tick)) {
                OneKeyMiner.LOGGER.warn(
                        "Replacing invalid shape preference '{}' from {} with the server default",
                        requestedShapeId, player.getGameProfile().getName()
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
                playerId,
                holding,
                shapeId,
                policy.teleportDropsApplied(),
                policy.teleportExpApplied()
        );
        ClientPreferenceAck acknowledgement = new ClientPreferenceAck(
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
