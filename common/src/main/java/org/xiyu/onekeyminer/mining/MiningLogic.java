package org.xiyu.onekeyminer.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.onekeyminer.chain.ChainActionLogic;

/**
 * Legacy mining entry point kept for binary-compatible loader integrations.
 */
public final class MiningLogic {
    private MiningLogic() {
    }

    @Deprecated(forRemoval = true)
    public static void onBlockBreak(
            ServerPlayer player,
            Level level,
            BlockPos originPos,
            BlockState originState
    ) {
        ChainActionLogic.onBlockBreak(player, level, originPos, originState);
    }
}