package org.xiyu.onekeyminer.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.ConfigSyncHelper;
import org.xiyu.onekeyminer.platform.PlatformServices;

/**
 * Forge 骞冲彴妯＄粍鍏ュ彛鐐?
 *
 * <p>璐熻矗鍦?Forge 骞冲彴涓婂垵濮嬪寲 OneKeyMiner 妯＄粍锛?
 * 鍖呮嫭娉ㄥ唽浜嬩欢鐩戝惉鍣ㄥ拰閰嶇疆鐣岄潰銆?/p>
 *
 * @author OneKeyMiner Team
 * @version 2.0.0
 * @since Minecraft 1.21.9
 */
@Mod(OneKeyMiner.MOD_ID)
public class OneKeyMinerForge {

    public OneKeyMinerForge(FMLJavaModLoadingContext context) {
        // 棣栧厛鍒濆鍖栧钩鍙版湇鍔★紙蹇呴』鍦?OneKeyMiner.init() 涔嬪墠锛?
        PlatformServices.setInstance(new ForgePlatformServices());

        // 鍒濆鍖栭€氱敤妯″潡
        OneKeyMiner.init();

        var modBusGroup = context.getModBusGroup();

        // 娉ㄥ唽鐢熷懡鍛ㄦ湡浜嬩欢
        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::onCommonSetup);

        // 瀹㈡埛绔笓鐢ㄤ簨浠?
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FMLClientSetupEvent.getBus(modBusGroup).addListener(this::onClientSetup);
            RegisterKeyMappingsEvent.getBus(modBusGroup).addListener(ForgeKeyBindings::registerKeyMappings);
            CustomizeGuiOverlayEvent.Chat.BUS.addListener(ForgeKeyBindings::renderPreviewHud);
            // 鎸夐敭鏄犲皠閫氳繃 @Mod.EventBusSubscriber 娉ㄨВ鑷姩娉ㄥ唽
            // 娉ㄥ唽閰嶇疆鐣岄潰锛堜粎瀹㈡埛绔紝閬垮厤鏈嶅姟绔姞杞紾UI绫诲鑷村穿婧冿級
            ForgeConfigScreen.register(ModLoadingContext.get());
        }

        // 娉ㄥ唽娓告垙浜嬩欢澶勭悊鍣ㄥ埌 Forge 浜嬩欢鎬荤嚎
        MinecraftForge.EVENT_BUS.register(new ForgeEventHandler());

        OneKeyMiner.LOGGER.info("OneKeyMiner Forge initialized");
    }

    /**
     * 閫氱敤璁剧疆浜嬩欢澶勭悊
     */
    private void onCommonSetup(FMLCommonSetupEvent event) {
        // 娉ㄥ唽缃戠粶鍖?
        ForgeNetworking.register();
        OneKeyMiner.LOGGER.debug("Forge 閫氱敤璁剧疆瀹屾垚");
    }

    /**
     * 瀹㈡埛绔缃簨浠跺鐞?
     */
    private void onClientSetup(FMLClientSetupEvent event) {
        // 娉ㄥ唽閰嶇疆鍚屾鍥炶皟
        ConfigSyncHelper.registerSyncCallback(() -> {
            var config = ConfigManager.getConfig();
            ForgeNetworking.sendTeleportSettings(config.teleportDrops, config.teleportExp);
            // 閰嶇疆鍙樻洿鍚庣殑鍥炶皟
        });
        // 娉ㄥ唽鎸夐敭缁戝畾
        ForgeKeyBindings.register();
        OneKeyMiner.LOGGER.debug("Forge client setup complete");
    }
}
