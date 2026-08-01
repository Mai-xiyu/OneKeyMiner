package org.xiyu.onekeyminer.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xiyu.onekeyminer.chain.ServerUseBridge;

/** Observes the exact entity whose authoritative interaction succeeded. */
@Mixin(Player.class)
abstract class PlayerMixin {
    @Inject(method = "interactOn", at = @At("HEAD"))
    private void onekeyminer$beginEntityUse(
            Entity target,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> callback
    ) {
        if ((Object) this instanceof ServerPlayer player) {
            ServerUseBridge.beginEntityUse(player, target, hand);
        }
    }

    @Inject(method = "interactOn", at = @At("RETURN"))
    private void onekeyminer$completeEntityUse(
            Entity target,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> callback
    ) {
        if ((Object) this instanceof ServerPlayer) {
            ServerUseBridge.completeEntityUse(callback.getReturnValue());
        }
    }
}
