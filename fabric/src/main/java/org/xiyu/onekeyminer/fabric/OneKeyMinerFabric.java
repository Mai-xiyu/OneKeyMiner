package org.xiyu.onekeyminer.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.mining.MiningStateManager;
import org.xiyu.onekeyminer.platform.PlatformServices;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

/**
 * Fabric common entry point.
 */
public class OneKeyMinerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PlatformServices.setInstance(new FabricPlatformServices());
        OneKeyMiner.init();
        FabricEventHandler.register();
        registerNetworking();
        OneKeyMiner.LOGGER.info("OneKeyMiner Fabric initialized");
    }

    private void registerNetworking() {
        // 注册按键状态包（客户端到服务端）
        PayloadTypeRegistry.serverboundPlay().register(
                FabricPayloads.ChainKeyState.TYPE,
                FabricPayloads.ChainKeyState.STREAM_CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
                FabricPayloads.TeleportSettings.TYPE,
                FabricPayloads.TeleportSettings.STREAM_CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                FabricPayloads.ChainKeyState.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    if (context.player() != null) {
                        PlatformServices.getInstance().setChainModeActive(context.player(), payload.holding());
                        Identifier id = Identifier.tryParse(payload.shapeId());
                        if (id != null && ShapeRegistry.getShape(id) != null) {
                            MiningStateManager.setPlayerShape(context.player(), id);
                        }
                    }
                })
        );

        ServerPlayNetworking.registerGlobalReceiver(
                FabricPayloads.TeleportSettings.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    if (context.player() != null) {
                        MiningStateManager.setTeleportDrops(context.player(), payload.teleportDrops());
                        MiningStateManager.setTeleportExp(context.player(), payload.teleportExp());
                    }
                })
        );
    }
}
