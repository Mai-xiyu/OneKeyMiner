package org.xiyu.onekeyminer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class OneKeyMinerVersionTest {

    @Test
    void usesManifestVersionWhenReleaseBuildOverridesVersion() {
        assertEquals("2.0.0-rc.1", OneKeyMiner.resolveVersion("2.0.0-rc.1"));
    }

    @Test
    void fallsBackForDevelopmentClasspath() {
        assertEquals("1.6.7", OneKeyMiner.resolveVersion(null));
        assertEquals("1.6.7", OneKeyMiner.resolveVersion("  "));
    }
}
