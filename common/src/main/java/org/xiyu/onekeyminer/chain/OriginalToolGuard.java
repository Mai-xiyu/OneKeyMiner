package org.xiyu.onekeyminer.chain;

import net.minecraft.world.item.ItemStack;

/** Validates that deferred mining still targets the stack that broke the origin. */
final class OriginalToolGuard {

    private OriginalToolGuard() {
    }

    static boolean matchesAfterBreak(ItemStack original, ItemStack current) {
        if (original == null || current == null) {
            return false;
        }
        if (original.isEmpty() || current.isEmpty()) {
            return original.isEmpty() && current.isEmpty();
        }
        if (original.getCount() != current.getCount()
                || !ItemStack.isSameItem(original, current)) {
            return false;
        }

        ItemStack normalizedOriginal = original.copy();
        if (normalizedOriginal.isDamageableItem()) {
            // The successful origin break may only change ordinary durability.
            normalizedOriginal.setDamageValue(current.getDamageValue());
        }
        return ItemStack.isSameItemSameComponents(normalizedOriginal, current);
    }
}
