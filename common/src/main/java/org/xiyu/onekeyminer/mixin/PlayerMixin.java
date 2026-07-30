package org.xiyu.onekeyminer.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.xiyu.onekeyminer.chain.ServerUseBridge;

@Mixin(Player.class)
abstract class PlayerMixin {

    @Redirect(
            method = "interactOn(Lnet/minecraft/world/entity/Entity;"
                    + "Lnet/minecraft/world/InteractionHand;)"
                    + "Lnet/minecraft/world/InteractionResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;"
                            + "interact(Lnet/minecraft/world/entity/player/"
                            + "Player;Lnet/minecraft/world/InteractionHand;)"
                            + "Lnet/minecraft/world/InteractionResult;"
            )
    )
    private InteractionResult onekeyminer$interact(
            Entity target,
            Player player,
            InteractionHand hand
    ) {
        return ServerUseBridge.interact(player, target, hand);
    }

    @Redirect(
            method = "interactOn(Lnet/minecraft/world/entity/Entity;"
                    + "Lnet/minecraft/world/InteractionHand;)"
                    + "Lnet/minecraft/world/InteractionResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;"
                            + "interactLivingEntity("
                            + "Lnet/minecraft/world/entity/player/Player;"
                            + "Lnet/minecraft/world/entity/LivingEntity;"
                            + "Lnet/minecraft/world/InteractionHand;)"
                            + "Lnet/minecraft/world/InteractionResult;"
            )
    )
    private InteractionResult onekeyminer$interactLivingEntity(
            ItemStack item,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        return ServerUseBridge.interactLivingEntity(
                item,
                player,
                target,
                hand
        );
    }
}
