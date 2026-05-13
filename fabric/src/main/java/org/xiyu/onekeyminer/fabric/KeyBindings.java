package org.xiyu.onekeyminer.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigManager;

/**
 * Fabric client key bindings and C2S sync.
 */
@Environment(EnvType.CLIENT)
public class KeyBindings {
    public static KeyMapping CHAIN_MINING_KEY;
    public static KeyMapping OPEN_CONFIG;

    private static boolean wasKeyDown = false;

    public static void register() {
        // 连锁挖矿激活按键（按住模式）
        CHAIN_MINING_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.onekeyminer.hold",             // 翻译键
                InputConstants.Type.KEYSYM,         // 输入类型
                GLFW.GLFW_KEY_GRAVE_ACCENT,        // 默认按键（`键）
                KeyMapping.Category.GAMEPLAY       // 使用游戏玩法分类
        ));
        
        // 打开配置界面（默认未绑定）
        OPEN_CONFIG = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.onekeyminer.config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KeyMapping.Category.GAMEPLAY
        ));

        registerKeyHandler();
    }

    public static void sendTeleportSettings(boolean teleportDrops, boolean teleportExp) {
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.getConnection() != null) {
                ClientPlayNetworking.send(new TeleportSettingsPayload(teleportDrops, teleportExp));
            }
        } catch (Exception e) {
            OneKeyMiner.LOGGER.debug("Failed to send teleport settings: {}", e.getMessage());
        }
    }

    private static void registerKeyHandler() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean isKeyDown = CHAIN_MINING_KEY.isDown();
            if (isKeyDown != wasKeyDown) {
                wasKeyDown = isKeyDown;
                if (client.getConnection() != null) {
                    try {
                        ClientPlayNetworking.send(new ChainKeyStatePayload(
                                isKeyDown,
                                ConfigManager.getConfig().selectedShape
                        ));
                    } catch (Exception e) {
                        OneKeyMiner.LOGGER.error("Failed to send chain key state: {}", e.getMessage());
                    }
                }
            }

            while (OPEN_CONFIG.consumeClick()) {
                client.gui.setScreen(new FabricConfigScreen(client.gui.screen()));
            }
        });
    }

    public record ChainKeyStatePayload(boolean holding, String shapeId) implements CustomPacketPayload {
        public static final Identifier ID = Identifier.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "chain_key_state");
        public static final CustomPacketPayload.Type<ChainKeyStatePayload> TYPE = new CustomPacketPayload.Type<>(ID);
        public static final StreamCodec<FriendlyByteBuf, ChainKeyStatePayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeBoolean(payload.holding);
                    buf.writeUtf(payload.shapeId == null ? "" : payload.shapeId);
                },
                buf -> new ChainKeyStatePayload(buf.readBoolean(), buf.readUtf(256))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record TeleportSettingsPayload(boolean teleportDrops, boolean teleportExp) implements CustomPacketPayload {
        public static final Identifier ID = Identifier.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "teleport_settings");
        public static final CustomPacketPayload.Type<TeleportSettingsPayload> TYPE = new CustomPacketPayload.Type<>(ID);
        public static final StreamCodec<FriendlyByteBuf, TeleportSettingsPayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeBoolean(payload.teleportDrops);
                    buf.writeBoolean(payload.teleportExp);
                },
                buf -> new TeleportSettingsPayload(buf.readBoolean(), buf.readBoolean())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
