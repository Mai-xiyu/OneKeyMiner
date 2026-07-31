package org.xiyu.onekeyminer.forge.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.xiyu.onekeyminer.chain.ServerUseBridge;

@Mixin(value = ForgeHooks.class, remap = false)
abstract class ForgeHooksMixin {

    @Redirect(
            method = "onInteractEntityAt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;"
                            + "interactAt(Lnet/minecraft/world/entity/player/"
                            + "Player;Lnet/minecraft/world/phys/Vec3;"
                            + "Lnet/minecraft/world/InteractionHand;)"
                            + "Lnet/minecraft/world/InteractionResult;",
                    remap = true
            ),
            remap = false
    )
    private static InteractionResult onekeyminer$interactAt(
            Entity target,
            Player player,
            Vec3 location,
            InteractionHand hand
    ) {
        return ServerUseBridge.interactAt(
                player,
                target,
                location,
                hand
        );
    }
}
