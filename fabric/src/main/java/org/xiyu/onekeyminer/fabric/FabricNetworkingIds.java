package org.xiyu.onekeyminer.fabric;

import net.minecraft.resources.ResourceLocation;
import org.xiyu.onekeyminer.OneKeyMiner;

/** Side-neutral Fabric networking constants. */
public final class FabricNetworkingIds {

    public static final int WIRE_VERSION =
            org.xiyu.onekeyminer.network.ClientPreferenceProtocol.WIRE_VERSION;
    public static final int MAX_SHAPE_ID_LENGTH =
            org.xiyu.onekeyminer.shape.ShapeRegistry.MAX_SHAPE_ID_LENGTH;

    public static final ResourceLocation CLIENT_PREFERENCES =
            new ResourceLocation(OneKeyMiner.MOD_ID, "client_preferences_v2");
    public static final ResourceLocation SERVER_PREFERENCES_ACK =
            new ResourceLocation(OneKeyMiner.MOD_ID, "server_preferences_ack_v2");

    private FabricNetworkingIds() {
    }
}
