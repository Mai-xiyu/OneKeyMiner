package org.xiyu.onekeyminer.legacy.fabric.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xiyu.onekeyminer.legacy.fabric.LegacyFabricMiner;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class LegacyInteractionMixin {
    @Unique private BlockState onekeyminer$before;
    @Unique private ItemStack onekeyminer$toolBefore;

    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void onekeyminer$before(ServerPlayerEntity player, World world, ItemStack stack, Hand hand,
                                     BlockHitResult hit, CallbackInfoReturnable<ActionResult> callback) {
        onekeyminer$before = world.getBlockState(hit.getBlockPos());
        onekeyminer$toolBefore = stack.copy();
    }

    @Inject(method = "interactBlock", at = @At("RETURN"))
    private void onekeyminer$after(ServerPlayerEntity player, World world, ItemStack stack, Hand hand,
                                    BlockHitResult hit, CallbackInfoReturnable<ActionResult> callback) {
        LegacyFabricMiner.afterInteraction(player, world, hand, hit, onekeyminer$before,
                onekeyminer$toolBefore, callback.getReturnValue().isAccepted());
    }
}
