package org.xiyu.onekeyminer.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.mining.MiningStateManager;
import org.xiyu.onekeyminer.platform.PlatformServices;

/**
 * Fabric 平台模组入口点
 * 
 * <p>负责在 Fabric 平台上初始化 OneKeyMiner 模组，
 * 包括注册事件监听器和配置界面。</p>
 * 
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.21.9
 */
public class OneKeyMinerFabric implements ModInitializer {
    
    @Override
    public void onInitialize() {
        // 首先初始化平台服务（必须在 OneKeyMiner.init() 之前）
        PlatformServices.setInstance(new FabricPlatformServices());
        
        // 初始化通用模块
        OneKeyMiner.init();
        
        // 注册事件处理器
        FabricEventHandler.register();
        
        // 注册网络包
        registerNetworking();
        
        OneKeyMiner.LOGGER.info("OneKeyMiner Fabric 模块初始化完成");
    }
    
    /**
     * 注册网络包处理
     */
    private void registerNetworking() {
        // 注册服务端接收处理 - 按键状态包
        ServerPlayNetworking.registerGlobalReceiver(
                KeyBindings.CHAIN_KEY_STATE_ID,
                (server, player, handler, buf, responseSender) -> {
                    boolean holding = buf.readBoolean();
                    String shapeId = buf.readUtf(256);
                    server.execute(() -> {
                        PlatformServices.getInstance().setChainModeActive(player, holding);
                        try {
                            ResourceLocation shapeRL = new ResourceLocation(shapeId);
                            MiningStateManager.setPlayerShape(player, shapeRL);
                        } catch (Exception e) {
                            OneKeyMiner.LOGGER.debug("无效的形状 ID: {}", shapeId);
                        }
                    });
                }
        );
        
        // 注册服务端接收处理 - 传送设置包
        ServerPlayNetworking.registerGlobalReceiver(
                KeyBindings.TELEPORT_SETTINGS_ID,
                (server, player, handler, buf, responseSender) -> {
                    boolean teleportDrops = buf.readBoolean();
                    boolean teleportExp = buf.readBoolean();
                    server.execute(() -> {
                        MiningStateManager.setTeleportDrops(player, teleportDrops);
                        MiningStateManager.setTeleportExp(player, teleportExp);
                        OneKeyMiner.LOGGER.debug("玩家 {} 更新传送设置: 掉落物={}, 经验={}",
                                player.getName().getString(), teleportDrops, teleportExp);
                    });
                }
        );
        
        OneKeyMiner.LOGGER.debug("已注册 Fabric 网络包处理");
    }
}
