package org.xiyu.onekeyminer.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.ConfigSyncHelper;
import org.xiyu.onekeyminer.preview.ChainPreviewHud;
import org.xiyu.onekeyminer.preview.ChainPreviewManager;

/**
 * Fabric client-only entry point.
 */
@Environment(EnvType.CLIENT)
public class OneKeyMinerFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ConfigSyncHelper.registerSyncCallback(() -> {
            var config = ConfigManager.getConfig();
            KeyBindings.sendTeleportSettings(config.teleportDrops, config.teleportExp);
        });

        KeyBindings.register();
        registerPreviewSystem();

        OneKeyMiner.LOGGER.info("OneKeyMiner Fabric client initialized");
    }

    private void registerPreviewSystem() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) {
                return;
            }

            BlockPos lookingAt = null;
            if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK) {
                lookingAt = ((BlockHitResult) client.hitResult).getBlockPos();
            }

            Direction playerFacing = client.player.getDirection();
            float playerPitch = client.player.getXRot();
            ChainPreviewManager.getInstance().tick(
                    client.level,
                    lookingAt,
                    playerFacing,
                    playerPitch,
                    KeyBindings.CHAIN_MINING_KEY != null && KeyBindings.CHAIN_MINING_KEY.isDown()
            );
        });

        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> ChainPreviewHud.render(guiGraphics));
    }
}
