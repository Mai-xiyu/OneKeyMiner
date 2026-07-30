package org.xiyu.onekeyminer.neoforge.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.xiyu.onekeyminer.chain.ServerUseBridge;

@Mixin(targets = "net.minecraft.server.network.ServerGamePacketListenerImpl$1")
abstract class NeoForgeExactEntityMixin {

    @Redirect(
            method = "lambda$onInteraction$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;"
                            + "interactAt(Lnet/minecraft/world/entity/player/"
                            + "Player;Lnet/minecraft/world/phys/Vec3;"
                            + "Lnet/minecraft/world/InteractionHand;)"
                            + "Lnet/minecraft/world/InteractionResult;"
            )
    )
    private InteractionResult onekeyminer$interactAt(
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
