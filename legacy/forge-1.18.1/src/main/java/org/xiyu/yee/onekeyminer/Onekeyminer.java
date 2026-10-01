package org.xiyu.yee.onekeyminer;


import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xiyu.yee.onekeyminer.client.KeyBindings;
import org.xiyu.yee.onekeyminer.config.ClientConfig;
import org.xiyu.yee.onekeyminer.config.CommonConfig;
import org.xiyu.yee.onekeyminer.config.ServerConfig;

@Mod(Onekeyminer.MODID)
public class Onekeyminer {
    public static final String MODID = "onekeyminer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);


    public Onekeyminer() {
        LOGGER.info("初始化一键连锁模组");

        IEventBus modEventBus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();

        // 注册配置
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);



        // 注册设置事件
        modEventBus.addListener(this::commonSetup);

        // 注册客户端事件（只在客户端加载）
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::clientSetup);

        }

        LOGGER.info("注册连锁挖掘、连锁互动和连锁种植功能完成");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("执行 CommonSetup：注册网络处理器等");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("设置客户端功能");
        event.enqueueWork(KeyBindings::registerClientTick);
        System.out.println("\n░███     ░███            ░██              ░██    ░██ ░██   ░██     ░██            \n░████   ░████                              ░██  ░██         ░██   ░██             \n░██░██ ░██░██  ░██████   ░██                ░██░██   ░██     ░██ ░██   ░██    ░██ \n░██ ░████ ░██       ░██  ░██   ░██████       ░███    ░██      ░████    ░██    ░██ \n░██  ░██  ░██  ░███████  ░██                ░██░██   ░██       ░██     ░██    ░██ \n░██       ░██ ░██   ░██  ░██               ░██  ░██  ░██       ░██     ░██   ░███ \n░██       ░██  ░█████░██ ░██              ░██    ░██ ░██       ░██      ░█████░██ \n");
    }
}
