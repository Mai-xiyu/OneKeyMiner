package org.xiyu.onekeyminer.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.xiyu.onekeyminer.chain.ServerUseBridge;

@Mixin(Player.class)
abstract class PlayerMixin {

    @WrapOperation(
            method = "interactOn(Lnet/minecraft/world/entity/Entity;"
                    + "Lnet/minecraft/world/InteractionHand;"
                    + "Lnet/minecraft/world/phys/Vec3;)"
                    + "Lnet/minecraft/world/InteractionResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;"
                            + "interact(Lnet/minecraft/world/entity/player/"
                            + "Player;Lnet/minecraft/world/InteractionHand;"
                            + "Lnet/minecraft/world/phys/Vec3;)"
                            + "Lnet/minecraft/world/InteractionResult;"
            )
    )
    private InteractionResult onekeyminer$interact(
            Entity target,
            Player player,
            InteractionHand hand,
            Vec3 location,
            Operation<InteractionResult> original
    ) {
        return ServerUseBridge.interact(
                player,
                target,
                hand,
                location,
                () -> original.call(target, player, hand, location)
        );
    }

    @WrapOperation(
            method = "interactOn(Lnet/minecraft/world/entity/Entity;"
                    + "Lnet/minecraft/world/InteractionHand;"
                    + "Lnet/minecraft/world/phys/Vec3;)"
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
            InteractionHand hand,
            Operation<InteractionResult> original
    ) {
        return ServerUseBridge.interactLivingEntity(
                item,
                player,
                target,
                hand,
                () -> original.call(item, player, target, hand)
        );
    }
}
