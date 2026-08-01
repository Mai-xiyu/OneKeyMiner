package org.xiyu.onekeyminer.network;

import net.minecraft.network.FriendlyByteBuf;

/** Shared strict framing check for loader-specific preference codecs. */
public final class PreferenceCodecGuard {
    private PreferenceCodecGuard() {
    }

    public static void requireFullyConsumed(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Unexpected trailing preference payload bytes");
        }
    }
}
