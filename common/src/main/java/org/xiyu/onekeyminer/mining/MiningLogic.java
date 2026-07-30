package org.xiyu.onekeyminer.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.xiyu.onekeyminer.chain.ChainActionLogic;
import org.xiyu.onekeyminer.chain.ChainActionResult;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.platform.PlatformServices;

import java.util.List;

/**
 * Compatibility facade for the pre-1.21.9 mining API.
 *
 * @deprecated Use {@link ChainActionLogic#onBlockBreak(ServerPlayer, Level, BlockPos, BlockState)}
 * instead. Keeping one authoritative implementation prevents loader and API entry points from
 * drifting apart.
 */
@Deprecated(forRemoval = false)
public final class MiningLogic {
    private MiningLogic() {
    }

    /**
     * Executes the current chain-mining implementation through the legacy void-returning entry
     * point.
     */
    public static void onBlockBreak(
            ServerPlayer player,
            Level level,
            BlockPos originPos,
            BlockState originState
    ) {
        ChainActionResult result = ChainActionLogic.onBlockBreak(
                player,
                level,
                originPos,
                originState
        );
        if (ConfigManager.getConfig().showStats && result.isSuccess()) {
            PlatformServices.getInstance().sendChainActionMessage(
                    player,
                    "mining",
                    result.totalCount()
            );
        }
    }

    /**
     * Legacy result type retained for binary/source compatibility with integrations.
     *
     * @deprecated Use {@link ChainActionResult}.
     */
    @Deprecated(forRemoval = false)
    public record MiningResult(
            List<BlockPos> minedBlocks,
            int totalMined,
            StopReason stopReason,
            int expCollected
    ) {
        public MiningResult {
            minedBlocks = minedBlocks == null
                    ? List.of()
                    : minedBlocks.stream().map(BlockPos::immutable).toList();
            stopReason = stopReason == null ? StopReason.CANCELLED : stopReason;
            totalMined = minedBlocks.size();
        }

        public MiningResult(
                List<BlockPos> minedBlocks,
                int totalMined,
                StopReason stopReason
        ) {
            this(minedBlocks, totalMined, stopReason, 0);
        }
    }

    /**
     * Legacy stop reasons retained for source compatibility.
     *
     * @deprecated Use {@link ChainActionResult.StopReason}.
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
