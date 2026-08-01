package org.xiyu.onekeyminer.forge.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.xiyu.onekeyminer.chain.ServerUseBridge;

@Mixin(ServerPlayerGameMode.class)
abstract class ForgeServerPlayerGameModeMixin {

    @Redirect(
            method = "useItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;"
                            + "onItemUseFirst(Lnet/minecraft/world/item/"
                            + "context/UseOnContext;)"
                            + "Lnet/minecraft/world/InteractionResult;"
            )
    )
    private InteractionResult onekeyminer$onItemUseFirst(
            ItemStack item,
            UseOnContext context
    ) {
        Player player = context.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return item.onItemUseFirst(context);
        }
        BlockHitResult hitResult = new BlockHitResult(
                context.getClickLocation(),
                context.getClickedFace(),
                context.getClickedPos(),
                context.isInside()
        );
        return ServerUseBridge.runBlockUse(
                serverPlayer,
                context.getLevel(),
                item,
                context.getHand(),
                hitResult,
                () -> item.onItemUseFirst(context)
        );
    }
}
