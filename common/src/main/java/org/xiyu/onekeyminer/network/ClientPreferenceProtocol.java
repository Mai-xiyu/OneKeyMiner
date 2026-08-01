package org.xiyu.onekeyminer.network;

/** Loader-neutral constants for the client preference protocol. */
public final class ClientPreferenceProtocol {
    public static final int WIRE_VERSION = 3;
    public static final int MAX_SHAPE_ID_LENGTH = 256;
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

    private ClientPreferenceProtocol() {
    }
}
