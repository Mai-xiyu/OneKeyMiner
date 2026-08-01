package org.xiyu.onekeyminer.fabric;

import net.minecraft.resources.ResourceLocation;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.network.ClientPreferenceProtocol;

/** Fabric 1.20.1 raw-payload channel identifiers for wire version 3. */
public final class FabricNetworkingIds {
    public static final int WIRE_VERSION = ClientPreferenceProtocol.WIRE_VERSION;
    public static final int MAX_SHAPE_ID_LENGTH = ClientPreferenceProtocol.MAX_SHAPE_ID_LENGTH;
    public static final ResourceLocation CLIENT_PREFERENCES = new ResourceLocation(
            OneKeyMiner.MOD_ID,
            "client_preferences_v" + WIRE_VERSION
    );
    public static final ResourceLocation SERVER_PREFERENCES_ACK = new ResourceLocation(
            OneKeyMiner.MOD_ID,
            "server_preferences_ack_v" + WIRE_VERSION
    );

    private FabricNetworkingIds() {
    }
}
