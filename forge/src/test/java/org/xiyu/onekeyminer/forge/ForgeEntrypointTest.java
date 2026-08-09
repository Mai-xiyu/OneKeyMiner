package org.xiyu.onekeyminer.forge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

final class ForgeEntrypointTest {
    @Test
    void exposesTheNoArgumentConstructorRequiredByForge47() {
        assertDoesNotThrow(() -> {
            OneKeyMinerForge.class.getDeclaredConstructor();
        });
    }
}
