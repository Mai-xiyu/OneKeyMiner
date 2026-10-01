package org.xiyu.yee.onekeyminer.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import com.mojang.authlib.GameProfile;
import org.xiyu.yee.onekeyminer.capability.ChainModeCapability;
import org.xiyu.yee.onekeyminer.chain.ChainHandler;
import org.xiyu.onekeyminer.legacy.core.LegacyChainSearch;
import org.xiyu.onekeyminer.legacy.core.LegacyPosition;
import org.xiyu.yee.onekeyminer.Onekeyminer;

import java.util.List;
import java.util.UUID;

/** Server-side tests for the 1.18 Forge adapter and shared traversal. */
@GameTestHolder(Onekeyminer.MODID)
public final class LegacyGameTests {
    private LegacyGameTests() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(LegacyGameTests.class);
    }

    @GameTest(templateNamespace = Onekeyminer.MODID, template = "empty3x3x3", timeoutTicks = 20)
    public static void connectedSearchSmoke(GameTestHelper helper) {
        BlockPos center = new BlockPos(1, 1, 1);
        helper.setBlock(center, Blocks.STONE);
        helper.setBlock(center.east(), Blocks.STONE);
        helper.setBlock(center.west(), Blocks.STONE);
        helper.assertBlockPresent(Blocks.STONE, center);

        LegacyPosition start = new LegacyPosition(0, 64, 0);
        List<LegacyPosition> positions = LegacyChainSearch.findConnected(
                start, 3, 1, false,
                position -> position.getY() == 64 && Math.abs(position.getX()) <= 1
                        && position.getZ() == 0);
        if (positions.size() != 3) {
            helper.fail("Expected three connected positions, got " + positions.size());
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = Onekeyminer.MODID, template = "empty3x3x3", timeoutTicks = 20)
    public static void chainMiningBreaksConnectedBlocks(GameTestHelper helper) {
        BlockPos origin = new BlockPos(1, 1, 1);
        BlockPos neighbor = origin.east();
        helper.setBlock(origin, Blocks.STONE);
        helper.setBlock(neighbor, Blocks.STONE);

        ServerLevel level = helper.getLevel();
        FakePlayer player = FakePlayerFactory.get(level,
                new GameProfile(UUID.randomUUID(), "onekeyminer-gametest"));
        BlockPos worldOrigin = helper.absolutePos(origin);
        player.setPos(worldOrigin.getX() + 0.5, worldOrigin.getY() + 1.0, worldOrigin.getZ() + 0.5);
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        ChainModeCapability.setChainMode(player, true);

        boolean handled = ChainHandler.tryChainMine(
                player, worldOrigin, Blocks.STONE.defaultBlockState(), tool);
        if (!handled) {
            helper.fail("Expected the server chain-mining entry point to handle the origin block");
            return;
        }

        helper.assertBlockPresent(Blocks.AIR, origin);
        helper.assertBlockPresent(Blocks.AIR, neighbor);
        helper.succeed();
    }
}
