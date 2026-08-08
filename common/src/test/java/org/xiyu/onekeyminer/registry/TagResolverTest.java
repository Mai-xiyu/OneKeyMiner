package org.xiyu.onekeyminer.registry;

import net.minecraft.SharedConstants;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TagResolverTest {

    @BeforeAll
    static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void singleCharacterWildcardIsValidatedAndMatchedConsistently() {
        assertTrue(TagResolver.isValidEntry("minecraft:ston?"));
        assertTrue(TagResolver.matchesBlock(Blocks.STONE, "minecraft:ston?"));
        assertTrue(TagResolver.matchesItem(Items.STICK, "minecraft:stic?"));
        assertFalse(TagResolver.matchesItem(Items.STICK, "minecraft:stone?"));
    }

    @Test
    void wildcardTagsAreRejectedBecauseTagKeysAreExactIdentifiers() {
        assertTrue(TagResolver.isValidEntry("#minecraft:logs"));
        assertFalse(TagResolver.isValidEntry("#minecraft:log?"));
        assertFalse(TagResolver.isValidEntry("#minecraft:*"));
    }
}
