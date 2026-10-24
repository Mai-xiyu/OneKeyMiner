package org.xiyu.onekeyminer.legacy.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.AliasedBlockItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.Blocks;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xiyu.onekeyminer.legacy.core.LegacyChainSearch;
import org.xiyu.onekeyminer.legacy.core.LegacyPosition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class LegacyFabricMiner implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("OneKeyMiner");
    public static final Identifier KEY_CHANNEL = new Identifier("onekeyminer", "legacy_key_v1");
    public static final Identifier POLICY_CHANNEL = new Identifier("onekeyminer", "legacy_policy_v1");
    private static final Map<UUID, KeyState> KEYS = new HashMap<>();
    private static final Map<UUID, ItemStack> BREAK_TOOLS = new HashMap<>();
    private static final Set<UUID> EXECUTING = new HashSet<>();

    @Override
    public void onInitialize() {
        LegacyFabricConfig.load();
        ServerPlayNetworking.registerGlobalReceiver(KEY_CHANNEL, (server, player, handler, buffer, responseSender) -> {
            if (buffer.readableBytes() != 1) return;
            boolean active = buffer.readBoolean();
            server.execute(() -> {
                if (server.getPlayerManager().getPlayer(player.getUuid()) == player) {
                    KEYS.put(player.getUuid(), new KeyState(active, server.getTicks()));
                }
            });
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            LegacyFabricConfig config = LegacyFabricConfig.get();
            PacketByteBuf buffer = PacketByteBufs.create();
            buffer.writeInt(config.enabled ? config.maxBlocks : 0);
            buffer.writeInt(config.maxDistance);
            buffer.writeBoolean(config.allowDiagonal);
            ServerPlayNetworking.send(handler.player, POLICY_CHANNEL, buffer);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            KEYS.remove(handler.player.getUuid());
            EXECUTING.remove(handler.player.getUuid());
            BREAK_TOOLS.remove(handler.player.getUuid());
        });
        PlayerBlockBreakEvents.BEFORE.register((world, player, position, state, blockEntity) -> {
            if (player instanceof ServerPlayerEntity && canChain((ServerPlayerEntity) player)) {
                BREAK_TOOLS.put(player.getUuid(), player.getMainHandStack().copy());
            }
            return true;
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, position, state, blockEntity) -> {
            if (world instanceof ServerWorld && player instanceof ServerPlayerEntity) {
                mine((ServerPlayerEntity) player, (ServerWorld) world, position, state);
            }
        });
    }

    public static List<BlockPos> targets(World world, BlockPos origin, BlockState original,
                                         int maxBlocks, int maxDistance, boolean diagonal) {
        List<LegacyPosition> positions = LegacyChainSearch.findConnected(
                new LegacyPosition(origin.getX(), origin.getY(), origin.getZ()),
                Math.max(1, Math.min(10240, maxBlocks)), Math.max(1, Math.min(128, maxDistance)), diagonal,
                candidate -> {
                    BlockPos position = new BlockPos(candidate.getX(), candidate.getY(), candidate.getZ());
                    return position.equals(origin) || world.isChunkLoaded(position)
                            && world.getBlockState(position).getBlock() == original.getBlock();
                });
        List<BlockPos> result = new ArrayList<>();
        for (LegacyPosition position : positions) {
            result.add(new BlockPos(position.getX(), position.getY(), position.getZ()));
        }
        return result;
    }

    private static boolean canChain(ServerPlayerEntity player) {
        LegacyFabricConfig config = LegacyFabricConfig.get();
        KeyState state = KEYS.get(player.getUuid());
        return config.enabled && state != null && state.active
                && player.getServer().getTicks() - state.tick < 100
                && !player.isSpectator() && !EXECUTING.contains(player.getUuid());
    }

    private static boolean canContinue(ServerPlayerEntity player, ItemStack originalTool, Hand hand) {
        if (player.isCreative()) return true;
        LegacyFabricConfig config = LegacyFabricConfig.get();
        ItemStack tool = player.getStackInHand(hand);
        return (!config.consumeHunger || player.getHungerManager().getFoodLevel() > config.minHungerLevel)
                && (config.allowBareHand || !tool.isEmpty())
                && (originalTool.isEmpty() || !tool.isEmpty() && tool.getItem() == originalTool.getItem())
                && (!tool.isDamageable() || tool.getMaxDamage() - tool.getDamage() > config.preserveDurability);
    }

    private static void mine(ServerPlayerEntity player, ServerWorld world, BlockPos origin, BlockState state) {
        if (!canChain(player) || state.isAir() || !allowed(state)) return;
        LegacyFabricConfig config = LegacyFabricConfig.get();
        ItemStack capturedTool = BREAK_TOOLS.remove(player.getUuid());
        ItemStack tool = capturedTool == null ? player.getMainHandStack().copy() : capturedTool;
        EXECUTING.add(player.getUuid());
        try {
            for (BlockPos target : targets(world, origin, state, config.maxBlocks, config.maxDistance, config.allowDiagonal)) {
                if (target.equals(origin)) continue;
                if (!canContinue(player, tool, Hand.MAIN_HAND)) break;
                BlockState current = world.getBlockState(target);
                if (current.getBlock() != state.getBlock() || !allowed(current) || !world.canPlayerModifyAt(player, target)) continue;
                if (player.interactionManager.tryBreakBlock(target) && !player.isCreative() && config.consumeHunger) {
                    player.addExhaustion(0.025f);
                }
            }
        } finally {
            EXECUTING.remove(player.getUuid());
        }
    }

    public static void afterInteraction(ServerPlayerEntity player, World world, Hand hand,
                                         BlockHitResult hit, BlockState before, ItemStack toolBefore, boolean accepted) {
        if (!accepted || !(world instanceof ServerWorld) || !canChain(player)) return;
        BlockPos origin = hit.getBlockPos();
        BlockState after = world.getBlockState(origin);
        boolean changed = before != after;
        boolean placed = !changed && toolBefore.getItem() instanceof AliasedBlockItem
                && (before.getBlock() instanceof FarmlandBlock || before.getBlock() == Blocks.SOUL_SAND)
                && !world.getBlockState(origin.offset(hit.getSide())).isAir();
        boolean supportedTool = toolBefore.getItem() instanceof AxeItem || toolBefore.getItem() instanceof HoeItem
                || toolBefore.getItem() instanceof ShovelItem || toolBefore.getItem() instanceof ShearsItem;
        changed = changed && supportedTool;
        LegacyFabricConfig config = LegacyFabricConfig.get();
        if ((!changed || !config.enableInteraction) && (!placed || !config.enablePlanting)) return;
        if (!allowed(before)) return;
        ItemStack activeTool = player.getStackInHand(hand);
        if (toolBefore.isEmpty() || activeTool.isEmpty() || activeTool.getItem() != toolBefore.getItem()) return;
        List<BlockPos> targets = targets(world, origin, before, config.maxBlocks, config.maxDistance, false);
        EXECUTING.add(player.getUuid());
        try {
            for (BlockPos target : targets) {
                if (target.equals(origin) || target.getY() != origin.getY()) continue;
                if (!canContinue(player, toolBefore, hand)) break;
                BlockState state = world.getBlockState(target);
                if (state != before || !allowed(state) || !world.canPlayerModifyAt(player, target)) continue;
                if (placed && !world.getBlockState(target.offset(hit.getSide())).isAir()) continue;
                BlockHitResult targetHit = new BlockHitResult(hit.getPos().add(
                        target.getX() - origin.getX(), 0, target.getZ() - origin.getZ()), hit.getSide(), target, hit.isInsideBlock());
                player.interactionManager.interactBlock(player, world, player.getStackInHand(hand), hand, targetHit);
            }
        } finally {
            EXECUTING.remove(player.getUuid());
        }
    }

    private static boolean allowed(BlockState state) {
        return !LegacyFabricConfig.get().blacklist.contains(Registry.BLOCK.getId(state.getBlock()).toString());
    }

    private static final class KeyState {
        private final boolean active;
        private final int tick;

        private KeyState(boolean active, int tick) {
            this.active = active;
            this.tick = tick;
        }
    }
}
