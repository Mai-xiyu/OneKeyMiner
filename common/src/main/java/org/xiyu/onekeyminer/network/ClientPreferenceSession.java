package org.xiyu.onekeyminer.network;

import org.xiyu.onekeyminer.config.MinerConfig;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Non-persistent client view of the latest server-authoritative ACK.
 * Dedicated servers may load this class, but never populate it.
 */
public final class ClientPreferenceSession {
    private static final AtomicReference<ClientPreferenceAck> LAST_ACK =
            new AtomicReference<>();

    private ClientPreferenceSession() {
    }

    public static void accept(ClientPreferenceAck ack) {
        LAST_ACK.set(Objects.requireNonNull(ack, "ack"));
    }

    public static void clear() {
        LAST_ACK.set(null);
    }

    public static Optional<ClientPreferenceAck> lastAck() {
        return Optional.ofNullable(LAST_ACK.get());
    }

    /** Resolves preview inputs without overwriting the client's saved request. */
    public static PreviewPolicy resolvePreviewPolicy(MinerConfig localConfig) {
        Objects.requireNonNull(localConfig, "localConfig");
        ClientPreferenceAck ack = LAST_ACK.get();
        if (ack == null
                || !ack.supports(ClientPreferenceProtocol.CAP_SERVER_PREVIEW_POLICY)) {
            return new PreviewPolicy(
                    localConfig.enabled,
                    localConfig.selectedShape,
                    localConfig.maxBlocks,
                    localConfig.maxDistance,
                    localConfig.allowDiagonal,
                    false
            );
        }
        return new PreviewPolicy(
                ack.serverEnabled(),
                ack.appliedShapeId(),
                ack.maxBlocksApplied(),
                ack.maxDistanceApplied(),
                ack.allowDiagonalApplied(),
                true
        );
    }

    public record PreviewPolicy(
            boolean enabled,
            String shapeId,
            int maxBlocks,
            int maxDistance,
            boolean allowDiagonal,
            boolean serverAcknowledged
    ) {
        public PreviewPolicy {
            shapeId = Objects.requireNonNull(shapeId, "shapeId");
        }
    }
}
