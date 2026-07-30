package org.xiyu.onekeyminer.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.onekeyminer.chain.ChainActionLogic;

import java.util.List;

/**
 * Legacy facade retained for source compatibility.
 *
 * @deprecated Use {@link ChainActionLogic}; maintaining two mining engines
 * caused protocol and drop-handling fixes to diverge.
 */
@Deprecated(forRemoval = true)
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
     * Legacy result type kept so existing add-ons continue to link.
     *
     * @deprecated The active engine returns
     * {@link org.xiyu.onekeyminer.chain.ChainActionResult}.
     */
    @Deprecated(forRemoval = true)
    public record MiningResult(
            List<BlockPos> minedBlocks,
            int totalMined,
            StopReason stopReason,
            int expCollected
    ) {
        public MiningResult {
            minedBlocks = minedBlocks == null ? List.of() : List.copyOf(minedBlocks);
        }

        public MiningResult(
                List<BlockPos> minedBlocks,
                int totalMined,
                StopReason stopReason
        ) {
            this(minedBlocks, totalMined, stopReason, 0);
        }
    }

    /** @deprecated Use ChainActionResult.StopReason. */
    @Deprecated(forRemoval = true)
    public enum StopReason {
        COMPLETED("completed"),
        MAX_BLOCKS("maximum block count reached"),
        LOW_DURABILITY("tool durability is low"),
        TOOL_BROKEN("tool broke"),
        LOW_HUNGER("hunger is low"),
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
