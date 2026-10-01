package org.xiyu.yee.onekeyminer.config;

import org.xiyu.yee.onekeyminer.Onekeyminer;

public class ConfigUtils {
    /**
     * 保存配置到文件
     */
    public static void saveConfig(Object spec) {
        try {
            CommonConfig.save();
            ServerConfig.save();
            ClientConfig.save();
            Onekeyminer.LOGGER.info("配置已成功保存");
        } catch (Exception e) {
            Onekeyminer.LOGGER.error("保存配置时发生错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 加载配置(如果需要)
     */
    public static void loadConfig(Object spec) {
        try {
            // 在1.12.2中,配置在init时已经加载
            Onekeyminer.LOGGER.info("配置加载完成");
        } catch (Exception e) {
            Onekeyminer.LOGGER.error("加载配置时发生错误: {}", e.getMessage(), e);
        }
    }
} 