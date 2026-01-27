package org.xiyu.onekeyminer.forge;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import org.xiyu.onekeyminer.OneKeyMiner;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.MinerConfig;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Forge 配置界面工厂 (国际化修复版)
 * <p>提供分页的图形化配置界面，使用翻译键支持多语言。</p>
 * @author OneKeyMiner Team
 * @version 1.2.0
 * @since Minecraft 1.21.9
 */
public class ForgeConfigScreen {
    
    public static void register(ModLoadingContext context) {
        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> createConfigScreen(parent)
                )
        );
        OneKeyMiner.LOGGER.debug("已注册 Forge 配置界面");
    }
    
    private static Screen createConfigScreen(Screen parent) {
        return new SimpleConfigScreen(parent);
    }
    
    private static class SimpleConfigScreen extends Screen {
        
        private final Screen parent;
        private final MinerConfig configCopy;
        private int currentPage = 0;
        private final int totalPages = 3;
        
        protected SimpleConfigScreen(Screen parent) {
            super(Component.translatable("config.onekeyminer.title"));
            this.parent = parent;
            this.configCopy = ConfigManager.getConfig().copy();
        }
        
        @Override
        protected void init() {
            super.init();
            this.clearWidgets();
            
            int centerX = this.width / 2;
            int startY = 40;
            int buttonWidth = 200;
            int buttonHeight = 20;
            int spacing = 24;
            
            switch (currentPage) {
                case 0: initPageGeneral(centerX, startY, buttonWidth, buttonHeight, spacing); break;
                case 1: initPageConsumption(centerX, startY, buttonWidth, buttonHeight, spacing); break;
                case 2: initPageAdvanced(centerX, startY, buttonWidth, buttonHeight, spacing); break;
            }
            
            // === 底部导航栏 ===
            int bottomY = this.height - 30;
            
            // 上一页
            Button prevBtn = Button.builder(Component.literal("<"), b -> {
                if (currentPage > 0) {
                    currentPage--;
                    this.init();
                }
            }).bounds(centerX - 155, bottomY, 20, buttonHeight).build();
            prevBtn.active = currentPage > 0;
            this.addRenderableWidget(prevBtn);
            
            // 下一页
            Button nextBtn = Button.builder(Component.literal(">"), b -> {
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    this.init();
                }
            }).bounds(centerX + 135, bottomY, 20, buttonHeight).build();
            nextBtn.active = currentPage < totalPages - 1;
            this.addRenderableWidget(nextBtn);
            
            // 保存 (使用 gui.done 或自定义保存键)
            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.done").withStyle(ChatFormatting.GREEN),
                    button -> {
                        ConfigManager.updateConfig(configCopy);
                        this.onClose();
                    }
            ).bounds(centerX - 125, bottomY, 120, buttonHeight).build());
            
            // 取消 (使用 gui.cancel)
            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.cancel"),
                    button -> this.onClose()
            ).bounds(centerX + 5, bottomY, 120, buttonHeight).build());
        }
        
        // === 第一页：基础设置 ===
        private void initPageGeneral(int x, int y, int w, int h, int s) {
            int i = 0;
            
            // 启用模组
            addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.enabled", 
                () -> configCopy.enabled, v -> configCopy.enabled = v);

            // 挖掘形状
            this.addRenderableWidget(Button.builder(
                getEnumMessage("config.onekeyminer.option.shape_mode", "onekeyminer.shape_mode." + configCopy.shapeMode.getId()),
                b -> {
                    MinerConfig.ShapeMode[] values = MinerConfig.ShapeMode.values();
                    int nextIndex = (configCopy.shapeMode.ordinal() + 1) % values.length;
                    configCopy.shapeMode = values[nextIndex];
                    b.setMessage(getEnumMessage("config.onekeyminer.option.shape_mode", "onekeyminer.shape_mode." + configCopy.shapeMode.getId()));
                }
            ).bounds(x - w / 2, y + s * i++, w, h).build());
            
            // 最大方块数
            this.addRenderableWidget(Button.builder(
                getValueMessage("config.onekeyminer.option.max_blocks", configCopy.maxBlocks),
                b -> {
                    int[] presets = {16, 32, 64, 128, 256, 512, 1000};
                    configCopy.maxBlocks = cycleValue(configCopy.maxBlocks, presets);
                    b.setMessage(getValueMessage("config.onekeyminer.option.max_blocks", configCopy.maxBlocks));
                }
            ).bounds(x - w / 2, y + s * i++, w, h).build());
            
            // 最大距离
            this.addRenderableWidget(Button.builder(
                getValueMessage("config.onekeyminer.option.max_distance", configCopy.maxDistance),
                b -> {
                    int[] presets = {8, 16, 32, 64};
                    configCopy.maxDistance = cycleValue(configCopy.maxDistance, presets);
                    b.setMessage(getValueMessage("config.onekeyminer.option.max_distance", configCopy.maxDistance));
                }
            ).bounds(x - w / 2, y + s * i++, w, h).build());
            
            // 对角线连锁
            addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.allow_diagonal", 
                () -> configCopy.allowDiagonal, v -> configCopy.allowDiagonal = v);
        }
        
        // === 第二页：消耗设置 ===
        private void initPageConsumption(int x, int y, int w, int h, int s) {
            int i = 0;
            
            // 消耗耐久
            addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.consume_durability", 
                () -> configCopy.consumeDurability, v -> configCopy.consumeDurability = v);
            
            // 低耐久保护
            addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.stop_low_durability", 
                () -> configCopy.stopOnLowDurability, v -> configCopy.stopOnLowDurability = v);
            
            // 保留耐久
            this.addRenderableWidget(Button.builder(
                getValueMessage("config.onekeyminer.option.preserve_durability", configCopy.preserveDurability),
                b -> {
                    int[] presets = {1, 5, 10, 20, 50};
                    configCopy.preserveDurability = cycleValue(configCopy.preserveDurability, presets);
                    b.setMessage(getValueMessage("config.onekeyminer.option.preserve_durability", configCopy.preserveDurability));
                }
            ).bounds(x - w / 2, y + s * i++, w, h).build());
            
            // 消耗饥饿
            addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.consume_hunger", 
                () -> configCopy.consumeHunger, v -> configCopy.consumeHunger = v);
            
            // 最低饥饿
            this.addRenderableWidget(Button.builder(
                getValueMessage("config.onekeyminer.option.min_hunger", configCopy.minHungerLevel),
                b -> {
                    int[] presets = {0, 2, 6, 10, 14};
                    configCopy.minHungerLevel = cycleValue(configCopy.minHungerLevel, presets);
                    b.setMessage(getValueMessage("config.onekeyminer.option.min_hunger", configCopy.minHungerLevel));
                }
            ).bounds(x - w / 2, y + s * i++, w, h).build());
            
            // 连锁任意方块
            addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.mine_all", 
                () -> configCopy.mineAllBlocks, v -> configCopy.mineAllBlocks = v);
            
            // 允许空手
            addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.allow_bare_hand", 
                () -> configCopy.allowBareHand, v -> configCopy.allowBareHand = v);
        }
        
        // === 第三页：高级设置 ===
        private void initPageAdvanced(int x, int y, int w, int h, int s) {
            int i = 0;
            
            // 连锁交互
            addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.enable_interaction", 
                () -> configCopy.enableInteraction, v -> configCopy.enableInteraction = v);
            
            // 连锁种植
            addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.enable_planting", 
                () -> configCopy.enablePlanting, v -> configCopy.enablePlanting = v);
            
            // 传送掉落物
            addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.teleport_drops", 
                () -> configCopy.teleportDrops, v -> configCopy.teleportDrops = v);
            
            // 传送经验
            addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.teleport_exp", 
                () -> configCopy.teleportExp, v -> configCopy.teleportExp = v);
            
            // 播放音效
            addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.play_sound", 
                () -> configCopy.playSound, v -> configCopy.playSound = v);
            
            // 严格匹配
            addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.strict_match", 
                () -> configCopy.requireExactMatch, v -> configCopy.requireExactMatch = v);
        }
        
        // === 辅助方法 ===
        
        /**
         * 添加布尔值切换按钮
         * @param key 翻译键名
         * @param getter 获取当前值的 Supplier
         * @param setter 设置新值的 Consumer
         */
        private void addBoolButton(int x, int y, int w, int h, String key, Supplier<Boolean> getter, Consumer<Boolean> setter) {
            this.addRenderableWidget(Button.builder(
                getBoolMessage(key, getter.get()),
                button -> {
                    boolean newState = !getter.get();
                    setter.accept(newState);
                    button.setMessage(getBoolMessage(key, newState));
                }
            ).bounds(x - w / 2, y, w, h).build());
        }
        
        // 构造布尔显示的 Component： "键名: 是/否"
        private Component getBoolMessage(String key, boolean value) {
            return Component.translatable(key).append(": ")
                    .append(Component.translatable(value ? "gui.yes" : "gui.no")
                    .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        
        // 构造数值显示的 Component： "键名: 数值"
        private Component getValueMessage(String key, int value) {
            return Component.translatable(key).append(": " + value);
        }
        
        // 构造枚举显示的 Component： "键名: 枚举翻译"
        private Component getEnumMessage(String key, String enumTranslationKey) {
            return Component.translatable(key).append(": ")
                    .append(Component.translatable(enumTranslationKey).withStyle(ChatFormatting.YELLOW));
        }
        
        private int cycleValue(int current, int[] presets) {
            for (int i = 0; i < presets.length; i++) {
                if (presets[i] >= current) {
                    return presets[(i + 1) % presets.length];
                }
            }
            return presets[0];
        }

        @Override
        public void onClose() {
            this.minecraft.setScreen(parent);
        }
        
        @Override
        public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
            guiGraphics.drawCenteredString(this.font, Component.literal((currentPage + 1) + " / " + totalPages), this.width / 2, this.height - 45, 0xAAAAAA);
        }
    }
}