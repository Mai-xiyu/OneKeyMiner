package org.xiyu.onekeyminer.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.shape.ShapeRegistry;

/** Fabric client key bindings and complete preference snapshots. */
@Environment(EnvType.CLIENT)
public final class KeyBindings {

    public static KeyMapping CHAIN_MINING_KEY;
    public static KeyMapping OPEN_CONFIG;

    private static boolean wasKeyDown;

    private KeyBindings() {
    }

    public static void register() {
        CHAIN_MINING_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.onekeyminer.hold",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                "key.categories.onekeyminer"
        ));
        OPEN_CONFIG = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.onekeyminer.config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "key.categories.onekeyminer"
        ));

        registerKeyHandler();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            resetConnectionState();
            client.execute(KeyBindings::sendCurrentState);
        });
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> resetConnectionState()
        );
    }

    public static void sendCurrentState() {
        var config = ConfigManager.getConfig();
        try {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.getConnection() == null) {
                return;
            }

            boolean keyDown = CHAIN_MINING_KEY != null && CHAIN_MINING_KEY.isDown();
            String shapeId = getSafeShapeId(config.selectedShape);
            if (ClientPlayNetworking.canSend(FabricNetworkingIds.CLIENT_STATE)) {
                FriendlyByteBuf buf = PacketByteBufs.create();
                buf.writeByte(FabricNetworkingIds.WIRE_VERSION);
                buf.writeBoolean(keyDown);
                buf.writeUtf(
                        shapeId,
                        FabricNetworkingIds.MAX_SHAPE_ID_LENGTH
                );
                buf.writeBoolean(config.teleportDrops);
                buf.writeBoolean(config.teleportExp);
                ClientPlayNetworking.send(FabricNetworkingIds.CLIENT_STATE, buf);
                return;
            }

            if (ClientPlayNetworking.canSend(FabricNetworkingIds.LEGACY_CHAIN_KEY_STATE)) {
                FriendlyByteBuf keyBuf = PacketByteBufs.create();
                keyBuf.writeBoolean(keyDown);
                keyBuf.writeUtf(
                        shapeId,
                        FabricNetworkingIds.MAX_SHAPE_ID_LENGTH
                );
                ClientPlayNetworking.send(
                        FabricNetworkingIds.LEGACY_CHAIN_KEY_STATE,
                        keyBuf
                );
            }
            if (ClientPlayNetworking.canSend(FabricNetworkingIds.LEGACY_TELEPORT_SETTINGS)) {
                FriendlyByteBuf settingsBuf = PacketByteBufs.create();
                settingsBuf.writeBoolean(config.teleportDrops);
                settingsBuf.writeBoolean(config.teleportExp);
                ClientPlayNetworking.send(
                        FabricNetworkingIds.LEGACY_TELEPORT_SETTINGS,
                        settingsBuf
                );
            }
        } catch (RuntimeException ignored) {
            // The connection may disappear between canSend and send.
        }
    }

    public static void resetConnectionState() {
        wasKeyDown = false;
    }

    private static String getSafeShapeId(String configuredShapeId) {
        if (configuredShapeId != null
                && configuredShapeId.length() <= FabricNetworkingIds.MAX_SHAPE_ID_LENGTH) {
            ResourceLocation parsed = ResourceLocation.tryParse(configuredShapeId);
            if (parsed != null && ShapeRegistry.isRegistered(parsed)) {
                return configuredShapeId;
            }
        }
        return ShapeRegistry.DEFAULT_SHAPE_ID.toString();
    }

    private static void registerKeyHandler() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(
                client -> {
                    boolean keyDown = CHAIN_MINING_KEY.isDown();
                    if (keyDown != wasKeyDown) {
                        wasKeyDown = keyDown;
                        sendCurrentState();
                    }

                    while (OPEN_CONFIG.consumeClick()) {
                        client.setScreen(new FabricConfigScreen(client.screen));
                    }
                }
        );
    }
}
