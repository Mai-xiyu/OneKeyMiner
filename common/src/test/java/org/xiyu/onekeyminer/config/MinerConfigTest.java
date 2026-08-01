package org.xiyu.onekeyminer.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class MinerConfigTest {
    @Test
    void copyIncludesPreviouslyOmittedFieldsAndOwnsCollections() {
        MinerConfig source = new MinerConfig();
        source.maxBlocksCreative = 777;
        source.hungerPerBlock = 0.75f;
        source.strictBlockMatching = true;
        source.customWhitelist = new ArrayList<>(List.of("minecraft:stone"));

        MinerConfig copy = source.copy();
        source.customWhitelist.add("minecraft:dirt");

        assertEquals(777, copy.maxBlocksCreative);
        assertEquals(0.75f, copy.hungerPerBlock);
        assertTrue(copy.strictBlockMatching);
        assertEquals(List.of("minecraft:stone"), copy.customWhitelist);
    }

    @Test
    void copyToleratesNullCollectionsFromMalformedJson() {
        MinerConfig source = new MinerConfig();
        source.customWhitelist = null;
        source.seedBlacklist = null;

        MinerConfig copy = assertDoesNotThrow(source::copy);

        assertNotNull(copy.customWhitelist);
        assertNotNull(copy.seedBlacklist);
    }
}
