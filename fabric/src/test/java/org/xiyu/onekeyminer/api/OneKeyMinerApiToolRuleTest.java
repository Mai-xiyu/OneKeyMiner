package org.xiyu.onekeyminer.api;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.xiyu.onekeyminer.chain.ChainActionType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OneKeyMinerApiToolRuleTest {

    @Test
    void rejectsUnexecutableRuleCombinationsAndInvalidTargets() {
        assertFalse(OneKeyMinerAPI.registerToolAction(
                "minecraft:shears",
                OneKeyMinerAPI.ToolTargetType.ENTITY,
                ChainActionType.INTERACTION,
                OneKeyMinerAPI.InteractionRule.GENERIC,
                List.of("minecraft:sheep")
        ));
        assertFalse(OneKeyMinerAPI.registerToolAction(
                "minecraft:shears",
                OneKeyMinerAPI.ToolTargetType.BLOCK,
                ChainActionType.INTERACTION,
                OneKeyMinerAPI.InteractionRule.SHEARING,
                List.of("minecraft:oak_leaves")
        ));
        assertFalse(OneKeyMinerAPI.registerToolAction(
                "minecraft:iron_pickaxe",
                OneKeyMinerAPI.ToolTargetType.BLOCK,
                ChainActionType.MINING,
                null,
                List.of("not a resource location")
        ));
        assertThrows(
                IllegalArgumentException.class,
                () -> new OneKeyMinerAPI.ToolActionRule(
                        new OneKeyMinerAPI.ToolSelector(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "shears"),
                                null
                        ),
                        OneKeyMinerAPI.ToolTargetType.ENTITY,
                        ChainActionType.MINING,
                        null,
                        List.of()
                )
        );
    }

    @Test
    void acceptsAndCanUnregisterCanonicalEntityShearingRule() {
        assertTrue(OneKeyMinerAPI.registerEntityShearingRule(
                "minecraft:shears",
                "minecraft:sheep"
        ));
        OneKeyMinerAPI.ToolActionRule registered = OneKeyMinerAPI.getToolActionRules()
                .getLast();
        assertTrue(OneKeyMinerAPI.unregisterToolAction(registered));
    }

    @Test
    void acceptsAndCanUnregisterBlockHarvestingRule() {
        assertTrue(OneKeyMinerAPI.registerToolAction(
                "minecraft:iron_hoe",
                OneKeyMinerAPI.ToolTargetType.BLOCK,
                ChainActionType.HARVESTING,
                null,
                List.of("minecraft:wheat")
        ));
        OneKeyMinerAPI.ToolActionRule registered = OneKeyMinerAPI.getToolActionRules()
                .getLast();
        assertTrue(OneKeyMinerAPI.unregisterToolAction(registered));
    }
}
