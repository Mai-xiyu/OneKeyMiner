package org.xiyu.onekeyminer.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xiyu.onekeyminer.chain.ServerUseBridge;

/** Observes the authoritative 1.20.1 block-use result. */
@Mixin(ServerPlayerGameMode.class)
abstract class ServerPlayerGameModeMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void onekeyminer$beginUse(
            ServerPlayer player,
            Level level,
            ItemStack stack,
            InteractionHand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> callback
    ) {
        ServerUseBridge.beginBlockUse(player, level, stack, hand, hitResult);
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void onekeyminer$completeUse(
            ServerPlayer player,
            Level level,
            ItemStack stack,
            InteractionHand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<InteractionResult> callback
    ) {
        ServerUseBridge.completeBlockUse(callback.getReturnValue());
    }
}
