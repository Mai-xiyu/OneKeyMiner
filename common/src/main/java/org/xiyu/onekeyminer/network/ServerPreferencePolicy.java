package org.xiyu.onekeyminer.network;

/** Pure intersection of client requests and dedicated-server policy. */
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
                requestedTeleportDrops && allowClientTeleportDrops,
                requestedTeleportExp && allowClientTeleportExp,
                ClientPreferenceProtocol.SUPPORTED_CAPABILITIES
        );
    }

    public record Result(
            boolean teleportDropsApplied,
            boolean teleportExpApplied,
            int capabilities
    ) {
    }
}
