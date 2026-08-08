package org.xiyu.onekeyminer.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.Objects;

/** Shared framing validation for loader-specific preference codecs. */
public final class PreferenceCodecGuard {
    private PreferenceCodecGuard() {
    }

    public static void requireFullyConsumed(FriendlyByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        if (buffer.readableBytes() != 0) {
            throw new IllegalArgumentException("Trailing bytes in preference payload");
        }
    }
}
