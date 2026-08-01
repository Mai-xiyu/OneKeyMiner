package org.xiyu.onekeyminer.network;

/** Pure server-policy evaluation for client-owned preferences. */
public final class ServerPreferencePolicy {
    private ServerPreferencePolicy() {
    }

    public static Result apply(
            boolean requestedTeleportDrops,
            boolean requestedTeleportExp,
            boolean allowClientTeleportDrops,
            boolean allowClientTeleportExp
    ) {
        return new Result(
                requestedTeleportDrops,
                requestedTeleportExp,
                requestedTeleportDrops && allowClientTeleportDrops,
                requestedTeleportExp && allowClientTeleportExp,
                ClientPreferenceProtocol.SUPPORTED_CAPABILITIES
        );
    }

    public record Result(
            boolean teleportDropsRequested,
            boolean teleportExpRequested,
            boolean teleportDropsApplied,
            boolean teleportExpApplied,
            int capabilities
    ) {
        public boolean supports(int capability) {
            return (capabilities & capability) == capability;
        }
    }
}
