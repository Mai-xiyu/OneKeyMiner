package org.xiyu.onekeyminer.chain;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OriginalToolGuardTest {

    @BeforeAll
    static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void acceptsOnlyTheSameFunctionalStackAfterNormalDurabilityDamage() {
        ItemStack original = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack damaged = original.copy();
        damaged.setDamageValue(1);

        assertTrue(OriginalToolGuard.matchesAfterBreak(original, damaged));
        assertTrue(OriginalToolGuard.matchesAfterBreak(ItemStack.EMPTY, ItemStack.EMPTY));

        ItemStack renamedReplacement = damaged.copy();
        renamedReplacement.set(DataComponents.CUSTOM_NAME, Component.literal("replacement"));
        assertFalse(OriginalToolGuard.matchesAfterBreak(original, renamedReplacement));
    }

    @Test
    void rejectsBrokenSwappedOrCountChangedToolsBeforeDerivedMining() {
        ItemStack original = new ItemStack(Items.DIAMOND_PICKAXE);

        assertFalse(OriginalToolGuard.matchesAfterBreak(original, ItemStack.EMPTY));
        assertFalse(OriginalToolGuard.matchesAfterBreak(
                original,
                new ItemStack(Items.IRON_PICKAXE)
        ));

        ItemStack invalidCount = original.copy();
        invalidCount.setCount(2);
        assertFalse(OriginalToolGuard.matchesAfterBreak(original, invalidCount));
    }
}
