package org.xiyu.onekeyminer.forge;

import net.minecraft.network.FriendlyByteBuf;

/** Codec checks kept independent from Forge channel bootstrap for unit tests. */
final class ForgePreferenceCodec {
    private ForgePreferenceCodec() {
    }

    static void requireFullyConsumed(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Unexpected trailing preference payload bytes");
        }
    }
}
