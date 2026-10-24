package org.xiyu.yee.onekeyminer;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xiyu.yee.onekeyminer.client.KeyBindings;
import org.xiyu.yee.onekeyminer.command.ModCommands;
import org.xiyu.yee.onekeyminer.config.ClientConfig;
import org.xiyu.yee.onekeyminer.config.CommonConfig;
import org.xiyu.yee.onekeyminer.config.ServerConfig;
import org.xiyu.yee.onekeyminer.network.NetworkHandler;

import java.io.File;

@Mod(modid = Onekeyminer.MODID, name = Onekeyminer.NAME, version = Onekeyminer.VERSION,
        guiFactory = "org.xiyu.yee.onekeyminer.client.ModGuiFactory")
public class Onekeyminer {
    public static final String MODID = "onekeyminer";
    public static final String NAME = "OneKeyMiner";
    public static final String VERSION = BuildInfo.VERSION;
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.Instance(MODID)
    public static Onekeyminer instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        File configDir = event.getModConfigurationDirectory();
        CommonConfig.init(new File(configDir, MODID + "-common.cfg"));
        ServerConfig.init(new File(configDir, MODID + "-server.cfg"));
        ClientConfig.init(new File(configDir, MODID + "-client.cfg"));
        NetworkHandler.registerPayloads(event);
        LOGGER.info("OneKeyMiner configuration and network handlers initialized");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new org.xiyu.yee.onekeyminer.chain.UnifiedChainHandler());
        MinecraftForge.EVENT_BUS.register(new org.xiyu.yee.onekeyminer.capability.ChainModeCapability());
        MinecraftForge.EVENT_BUS.register(new org.xiyu.yee.onekeyminer.chain.PlantingHandler());
        MinecraftForge.EVENT_BUS.register(new org.xiyu.yee.onekeyminer.UpdateChecker());
        if (event.getSide() == Side.CLIENT) {
            initClient();
        }
        LOGGER.info("OneKeyMiner chain mining, interaction and planting handlers initialized");
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new ModCommands());
    }

    @SideOnly(Side.CLIENT)
    private void initClient() {
        KeyBindings.registerClientTick();
        MinecraftForge.EVENT_BUS.register(new KeyBindings());
    }
}
