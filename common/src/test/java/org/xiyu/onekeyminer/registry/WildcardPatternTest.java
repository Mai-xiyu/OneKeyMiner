package org.xiyu.onekeyminer.registry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildcardPatternTest {

    @Test
    void treatsNonWildcardRegexCharactersLiterally() {
        assertTrue(WildcardPattern.matches("minecraft:diamond_ore", "minecraft:*_ore"));
        assertFalse(WildcardPattern.matches("minecraft:stone", "minecraft:*_ore"));
        assertFalse(WildcardPattern.matches("minecraft:stone", "minecraft:*["));
        assertFalse(WildcardPattern.matches("minecraft:stick", "minecraft:*("));
    }

    @Test
    void supportsSingleCharacterWildcard() {
        assertTrue(WildcardPattern.matches("minecraft:stone", "minecraft:ston?"));
        assertFalse(WildcardPattern.matches("minecraft:stones", "minecraft:ston?"));
    }
}
