package org.xiyu.onekeyminer.fabric;

import net.minecraft.resources.ResourceLocation;
import org.xiyu.onekeyminer.OneKeyMiner;

/** Side-neutral networking constants safe to load on a dedicated server. */
public final class FabricNetworkingIds {

    public static final int WIRE_VERSION = 1;
    public static final int MAX_SHAPE_ID_LENGTH = 256;

    public static final ResourceLocation CLIENT_STATE =
            new ResourceLocation(OneKeyMiner.MOD_ID, "client_state_v1");

    /** Legacy channels retained for rolling upgrades within the 1.20.1 branch. */
    public static final ResourceLocation LEGACY_CHAIN_KEY_STATE =
            new ResourceLocation(OneKeyMiner.MOD_ID, "chain_key_state");
    public static final ResourceLocation LEGACY_TELEPORT_SETTINGS =
            new ResourceLocation(OneKeyMiner.MOD_ID, "teleport_settings");

    private FabricNetworkingIds() {
    }
}
