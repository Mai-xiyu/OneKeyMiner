package org.xiyu.yee.onekeyminer.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.loading.FMLPaths;
import org.xiyu.yee.onekeyminer.Onekeyminer;

import java.io.File;
import java.nio.file.Path;
import java.lang.reflect.Method;

public class ConfigUtils {
    /**
     * 保存配置到文件
     * @param spec 配置规范
     */
    public static void saveConfig(ForgeConfigSpec spec) {
        try {
            // 获取配置文件路径
            Path configPath = getConfigPath();
            File configFile = configPath.toFile();
            
            // 尝试多种反射方法保存配置
            boolean saved = false;
            String errorMessage = "";

            // 尝试方法1：save(File)
            try {
                Method saveMethod = spec.getClass().getMethod("save", File.class);
                saveMethod.invoke(spec, configFile);
                saved = true;
            } catch (Exception e1) {
                errorMessage += "方法1失败: " + e1.getMessage() + "; ";
            }

            // 尝试方法2：save()
            if (!saved) {
                try {
                    Method saveMethod = spec.getClass().getMethod("save");
                    saveMethod.invoke(spec);
                    saved = true;
                } catch (Exception e2) {
                    errorMessage += "方法2失败: " + e2.getMessage() + "; ";
                }
            }

            // 尝试方法3：使用公共父类或接口的方法
            if (!saved) {
                try {
                    Method saveMethod = spec.getClass().getMethod("saveToFile", File.class);
                    saveMethod.invoke(spec, configFile);
                    saved = true;
                } catch (Exception e3) {
                    errorMessage += "方法3失败: " + e3.getMessage() + "; ";
                }
            }

            if (!saved) {
                throw new RuntimeException("无法保存配置: " + errorMessage);
            }
            
            Onekeyminer.LOGGER.info("配置已成功保存到: {}", configPath);
        } catch (Exception e) {
            Onekeyminer.LOGGER.error("保存配置时发生最终错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取配置文件路径
     */
    private static Path getConfigPath() {
        // 这里使用 Forge 的配置目录
        return FMLPaths.CONFIGDIR.get()
            .resolve(Onekeyminer.MODID + "-common.toml");
    }

    /**
     * 加载配置（如果需要）
     * @param spec 配置规范
     */
    public static void loadConfig(ForgeConfigSpec spec) {
        try {
            Path configPath = getConfigPath();
            File configFile = configPath.toFile();
            
            if (configFile.exists()) {
                // 尝试多种反射方法加载配置
                boolean loaded = false;
                String errorMessage = "";

                // 尝试方法1：load(File)
                try {
                    Method loadMethod = spec.getClass().getMethod("load", File.class);
                    loadMethod.invoke(spec, configFile);
                    loaded = true;
                } catch (Exception e1) {
                    errorMessage += "方法1失败: " + e1.getMessage() + "; ";
                }

                // 尝试方法2：load()
                if (!loaded) {
                    try {
                        Method loadMethod = spec.getClass().getMethod("load");
                        loadMethod.invoke(spec);
                        loaded = true;
                    } catch (Exception e2) {
                        errorMessage += "方法2失败: " + e2.getMessage() + "; ";
                    }
                }

                // 尝试方法3：使用公共父类或接口的方法
                if (!loaded) {
                    try {
                        Method loadMethod = spec.getClass().getMethod("loadFromFile", File.class);
                        loadMethod.invoke(spec, configFile);
                        loaded = true;
                    } catch (Exception e3) {
                        errorMessage += "方法3失败: " + e3.getMessage() + "; ";
                    }
                }

                if (!loaded) {
                    throw new RuntimeException("无法加载配置: " + errorMessage);
                }
                
                Onekeyminer.LOGGER.info("配置已从 {} 加载", configPath);
            }
        } catch (Exception e) {
            Onekeyminer.LOGGER.error("加载配置时发生最终错误: {}", e.getMessage(), e);
        }
    }
} 