package org.xiyu.yee.onekeyminer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.IConfigElement;
import org.xiyu.yee.onekeyminer.Onekeyminer;
import org.xiyu.yee.onekeyminer.config.CommonConfig;
import org.xiyu.yee.onekeyminer.config.ClientConfig;
import org.xiyu.yee.onekeyminer.config.ServerConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Collections;

/**
 * Forge 1.12.2 Config GUI Factory
 * 让 Mod 列表的 Config 按钮可以打开配置界面
 */
public class ModGuiFactory implements IModGuiFactory {
    
    @Override
    public void initialize(Minecraft minecraftInstance) {
        // 初始化逻辑（可选）
    }

    @Override
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        // 合并三个配置文件的所有 category 到一个 GUI
        List<IConfigElement> elements = new ArrayList<>();
        
        // 添加通用配置（general category）
        if (CommonConfig.getConfig() != null && CommonConfig.getConfig().hasCategory("general")) {
            elements.addAll(new ConfigElement(CommonConfig.getConfig().getCategory("general")).getChildElements());
        }
        
        // 添加客户端配置（client category）
        if (ClientConfig.getConfig() != null && ClientConfig.getConfig().hasCategory("client")) {
            elements.addAll(new ConfigElement(ClientConfig.getConfig().getCategory("client")).getChildElements());
        }
        
        // 添加服务器配置（server category）
        if (ServerConfig.getConfig() != null && ServerConfig.getConfig().hasCategory("server")) {
            elements.addAll(new ConfigElement(ServerConfig.getConfig().getCategory("server")).getChildElements());
        }
        
        // 创建配置 GUI
        // 参数说明:
        // - parentScreen: 父界面
        // - elements: 配置元素列表
        // - modID: 模组ID
        // - allRequireWorldRestart: 是否所有配置都需要重启世界
        // - allRequireMcRestart: 是否所有配置都需要重启游戏
        // - title: 界面标题（可以是本地化key）
        return new GuiConfig(
            parentScreen,
            elements,
            Onekeyminer.MODID,
            false,  // 不需要重启世界
            false,  // 不需要重启游戏
            "config.onekeyminer.title"  // 使用本地化标题
        );
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        // 运行时可修改的配置分类（可选，返回空集合）
        return Collections.emptySet();
    }
}
