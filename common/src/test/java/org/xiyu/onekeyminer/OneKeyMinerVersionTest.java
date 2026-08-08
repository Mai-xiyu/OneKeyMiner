package org.xiyu.onekeyminer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class OneKeyMinerVersionTest {

    @Test
    void usesManifestVersionWhenPresent() {
        assertEquals("9.8.7", RuntimeVersion.resolve("9.8.7"));
    }

    @Test
    void usesReleaseFallbackWhenManifestVersionIsUnavailable() {
        assertEquals("1.6.8", RuntimeVersion.resolve(null));
        assertEquals("1.6.8", RuntimeVersion.resolve("  "));
    }
}
