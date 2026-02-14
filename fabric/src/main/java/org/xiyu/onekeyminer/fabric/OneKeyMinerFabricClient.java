package org.xiyu.onekeyminer.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.preview.ChainPreviewHud;
import org.xiyu.onekeyminer.preview.ChainPreviewManager;

/**
 * Fabric 平台客户端入口点
 * 
 * <p>负责客户端专用功能的初始化，如配置界面、按键绑定、HUD 预览等。</p>
 * 
 * @author OneKeyMiner Team
 * @version 1.1.0
 * @since Minecraft 1.20.1
 */
@Environment(EnvType.CLIENT)
public class OneKeyMinerFabricClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        // 注册按键绑定（这会注册 ClientTickEvents）
        KeyBindings.register();
        
        // 注册配置界面按键处理（原生实现，不依赖 ModMenu/Cloth Config）
        registerConfigKeyHandler();
        
        // 注册预览系统
        registerPreviewSystem();
        
        OneKeyMiner.LOGGER.info("OneKeyMiner Fabric 客户端模块初始化完成");
    }
    
    /**
     * 注册配置界面快捷键处理器
     */
    private void registerConfigKeyHandler() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KeyBindings.OPEN_CONFIG.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new FabricConfigScreen(null));
                }
            }
        });
    }
    
    /**
     * 注册连锁预览系统（tick 更新 + HUD 渲染）
     */
    private void registerPreviewSystem() {
        // 预览 tick 更新
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) {
                return;
            }
            
            boolean isChainKeyDown = KeyBindings.CHAIN_MINING_KEY.isDown();
            
            BlockPos lookingAt = null;
            if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK) {
                lookingAt = ((BlockHitResult) client.hitResult).getBlockPos();
            }
            
            Direction playerFacing = client.player.getDirection();
            float playerPitch = client.player.getXRot();
            
            ChainPreviewManager.getInstance().tick(
                    client.level, lookingAt, playerFacing, playerPitch, isChainKeyDown
            );
        });
        
        // HUD 渲染
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            ChainPreviewHud.render(guiGraphics);
        });
    }
}
