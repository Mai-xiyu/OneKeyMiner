package org.xiyu.onekeyminer.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.onekeyminer.chain.ChainActionLogic;

import java.util.List;

/**
 * Compatibility entry point for addons/loaders compiled against the legacy API.
 *
 * @deprecated use {@link ChainActionLogic#onBlockBreak(ServerPlayer, Level, BlockPos, BlockState)}
 */
@Deprecated(forRemoval = false)
public final class MiningLogic {
    private MiningLogic() {
    }

    public static void onBlockBreak(
            ServerPlayer player,
            Level level,
            BlockPos originPos,
            BlockState originState
    ) {
        ChainActionLogic.onBlockBreak(player, level, originPos, originState);
    }

    /**
     * @deprecated retained for source/binary compatibility with legacy addons.
     */
    @Deprecated(forRemoval = false)
    public record MiningResult(
            List<BlockPos> minedBlocks,
            int totalMined,
            StopReason stopReason,
            int expCollected
    ) {
        public MiningResult(List<BlockPos> minedBlocks, int totalMined, StopReason stopReason) {
            this(minedBlocks, totalMined, stopReason, 0);
        }
    }

    /**
     * @deprecated retained for source/binary compatibility with legacy addons.
     */
    @Deprecated(forRemoval = false)
    public enum StopReason {
        COMPLETED("completed"),
        MAX_BLOCKS("max blocks"),
        LOW_DURABILITY("low durability"),
        TOOL_BROKEN("tool broken"),
        LOW_HUNGER("low hunger"),
        CANCELLED("cancelled");

        private final String message;

        StopReason(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
