package org.xiyu.onekeyminer.mixin;

import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.xiyu.onekeyminer.chain.ServerUseBridge;

@Mixin(ServerPlayerGameMode.class)
abstract class ServerPlayerGameModeMixin {

    @Redirect(
            method = "useItemOn(Lnet/minecraft/server/level/ServerPlayer;"
                    + "Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/world/item/ItemStack;"
                    + "Lnet/minecraft/world/InteractionHand;"
                    + "Lnet/minecraft/world/phys/BlockHitResult;)"
                    + "Lnet/minecraft/world/InteractionResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/"
                            + "BlockState;useItemOn("
                            + "Lnet/minecraft/world/item/ItemStack;"
                            + "Lnet/minecraft/world/level/Level;"
                            + "Lnet/minecraft/world/entity/player/Player;"
                            + "Lnet/minecraft/world/InteractionHand;"
                            + "Lnet/minecraft/world/phys/BlockHitResult;)"
                            + "Lnet/minecraft/world/ItemInteractionResult;"
            )
    )
    private ItemInteractionResult onekeyminer$useItemOnBlock(
            BlockState state,
            ItemStack item,
            Level level,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        return ServerUseBridge.useItemOnBlock(
                state,
                item,
                level,
                player,
                hand,
                hitResult
        );
    }

    @Redirect(
            method = "useItemOn(Lnet/minecraft/server/level/ServerPlayer;"
                    + "Lnet/minecraft/world/level/Level;"
                    + "Lnet/minecraft/world/item/ItemStack;"
                    + "Lnet/minecraft/world/InteractionHand;"
                    + "Lnet/minecraft/world/phys/BlockHitResult;)"
                    + "Lnet/minecraft/world/InteractionResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;"
                            + "useOn(Lnet/minecraft/world/item/context/"
                            + "UseOnContext;)"
                            + "Lnet/minecraft/world/InteractionResult;"
            )
    )
    private InteractionResult onekeyminer$useOn(
            ItemStack item,
            UseOnContext context
    ) {
        return ServerUseBridge.useOn(item, context);
    }
}
