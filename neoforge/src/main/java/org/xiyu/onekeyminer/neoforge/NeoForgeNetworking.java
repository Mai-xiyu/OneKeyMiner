package org.xiyu.onekeyminer.neoforge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.platform.PlatformServices;

/**
 * NeoForge 网络包注册
 * 
 * <p>注册客户端到服务端的按键状态同步包。</p>
 * 
 * @author OneKeyMiner Team
 * @version 1.0.0
 * @since Minecraft 1.21.9
 */
public class NeoForgeNetworking {
    
    /**
     * 按键状态网络包
     */
    public record ChainKeyStatePayload(boolean holding) implements CustomPacketPayload {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "chain_key_state");
        public static final CustomPacketPayload.Type<ChainKeyStatePayload> TYPE = new CustomPacketPayload.Type<>(ID);
        
        public static final StreamCodec<FriendlyByteBuf, ChainKeyStatePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, ChainKeyStatePayload::holding,
                ChainKeyStatePayload::new
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * 注册网络包处理器（在 MOD 事件总线上调用）
     */
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(OneKeyMiner.MOD_ID);
        
        // 注册按键状态包（客户端到服务端）
        registrar.playToServer(
                ChainKeyStatePayload.TYPE,
                ChainKeyStatePayload.STREAM_CODEC,
                NeoForgeNetworking::handleChainKeyState
        );
        
        OneKeyMiner.LOGGER.debug("已注册 NeoForge 网络包处理器");
    }
    
    /**
     * 发送按键状态到服务端
     */
    public static void sendKeyState(boolean pressed) {
        try {
            net.minecraft.client.Minecraft.getInstance().getConnection().send(new ChainKeyStatePayload(pressed));
        } catch (Exception e) {
            OneKeyMiner.LOGGER.debug("发送按键状态失败: {}", e.getMessage());
        }
    }
    
    /**
     * 处理按键状态包
     */
    private static void handleChainKeyState(ChainKeyStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                PlatformServices.getInstance().setChainModeActive(serverPlayer, payload.holding());
            }
        });
    }
}
