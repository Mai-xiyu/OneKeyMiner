package org.xiyu.onekeyminer.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.xiyu.onekeyminer.config.ConfigManager;
import org.xiyu.onekeyminer.config.MinerConfig;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Fabric 原生配置界面
 * 
 * <p>使用原版 Minecraft GUI API 实现的三页式配置界面，
 * 无需依赖 Cloth Config 等第三方库。</p>
 * 
 * <p>三页布局：</p>
 * <ul>
 *   <li>第一页：基础设置（启用、形状、范围等）</li>
 *   <li>第二页：消耗设置（耐久、饥饿等）</li>
 *   <li>第三页：高级设置（交互、种植、收割、掉落等）</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public class FabricConfigScreen extends Screen {

    private final Screen parent;
    private MinerConfig configCopy;
    
    // 页面管理
    private int currentPage = 0;
    private static final int TOTAL_PAGES = 3;
    private static final String[] PAGE_NAMES = {"基础设置", "消耗设置", "高级设置"};
    
    // 布局常量
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 24;
    private static final int TOP_OFFSET = 40;
    
    public FabricConfigScreen(Screen parent) {
        super(Component.translatable("config.onekeyminer.title"));
        this.parent = parent;
    }
    
    @Override
    protected void init() {
        configCopy = ConfigManager.getConfig().copy();
        rebuildPage();
    }
    
    private void rebuildPage() {
        clearWidgets();
        
        int x = (width - BUTTON_WIDTH) / 2;
        int y = TOP_OFFSET;
        int w = BUTTON_WIDTH;
        int h = BUTTON_HEIGHT;
        int s = SPACING;
        
        switch (currentPage) {
            case 0 -> initPageBasic(x, y, w, h, s);
            case 1 -> initPageConsumption(x, y, w, h, s);
            case 2 -> initPageAdvanced(x, y, w, h, s);
        }
        
        // 导航按钮
        int navY = height - 30;
        if (currentPage > 0) {
            addRenderableWidget(Button.builder(Component.literal("< 上一页"), b -> {
                currentPage--;
                rebuildPage();
            }).bounds(width / 2 - 154, navY, 70, 20).build());
        }
        
        // 页码显示
        addRenderableWidget(Button.builder(
            Component.literal((currentPage + 1) + "/" + TOTAL_PAGES + " " + PAGE_NAMES[currentPage]),
            b -> {}
        ).bounds(width / 2 - 50, navY, 100, 20).build());
        
        if (currentPage < TOTAL_PAGES - 1) {
            addRenderableWidget(Button.builder(Component.literal("下一页 >"), b -> {
                currentPage++;
                rebuildPage();
            }).bounds(width / 2 + 84, navY, 70, 20).build());
        }
        
        // 保存 & 取消
        addRenderableWidget(Button.builder(Component.literal("§a保存"), b -> {
            ConfigManager.updateConfig(configCopy);
            ConfigManager.save();
            onClose();
        }).bounds(width / 2 - 154, navY - 24, 100, 20).build());
        
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> {
            onClose();
        }).bounds(width / 2 + 54, navY - 24, 100, 20).build());
    }
    
    // === 第一页：基础设置 ===
    private void initPageBasic(int x, int y, int w, int h, int s) {
        int i = 0;
        addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.enabled",
            () -> configCopy.enabled, v -> configCopy.enabled = v);
        addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.allow_diagonal",
            () -> configCopy.allowDiagonal, v -> configCopy.allowDiagonal = v);
        addIntButton(x, y + s * i++, w, h, "config.onekeyminer.option.max_blocks",
            () -> configCopy.maxBlocks, v -> configCopy.maxBlocks = v, 1, 1000, 8);
        addIntButton(x, y + s * i++, w, h, "config.onekeyminer.option.max_distance",
            () -> configCopy.maxDistance, v -> configCopy.maxDistance = v, 1, 64, 4);
        addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.mine_all",
            () -> configCopy.mineAllBlocks, v -> configCopy.mineAllBlocks = v);
        addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.allow_bare_hand",
            () -> configCopy.allowBareHand, v -> configCopy.allowBareHand = v);
    }
    
    // === 第二页：消耗设置 ===
    private void initPageConsumption(int x, int y, int w, int h, int s) {
        int i = 0;
        addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.consume_durability",
            () -> configCopy.consumeDurability, v -> configCopy.consumeDurability = v);
        addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.stop_low_durability",
            () -> configCopy.stopOnLowDurability, v -> configCopy.stopOnLowDurability = v);
        addIntButton(x, y + s * i++, w, h, "config.onekeyminer.option.preserve_durability",
            () -> configCopy.preserveDurability, v -> configCopy.preserveDurability = v, 0, 100, 1);
        addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.consume_hunger",
            () -> configCopy.consumeHunger, v -> configCopy.consumeHunger = v);
        addIntButton(x, y + s * i++, w, h, "config.onekeyminer.option.min_hunger",
            () -> configCopy.minHungerLevel, v -> configCopy.minHungerLevel = v, 0, 20, 1);
    }
    
    // === 第三页：高级设置 ===
    private void initPageAdvanced(int x, int y, int w, int h, int s) {
        int i = 0;
        addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.enable_interaction",
            () -> configCopy.enableInteraction, v -> configCopy.enableInteraction = v);
        addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.enable_planting",
            () -> configCopy.enablePlanting, v -> configCopy.enablePlanting = v);
        addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.enable_harvesting",
            () -> configCopy.enableHarvesting, v -> configCopy.enableHarvesting = v);
        addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.harvest_replant",
            () -> configCopy.harvestReplant, v -> configCopy.harvestReplant = v);
        addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.teleport_drops",
            () -> configCopy.teleportDrops, v -> configCopy.teleportDrops = v);
        addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.teleport_exp",
            () -> configCopy.teleportExp, v -> configCopy.teleportExp = v);
        addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.play_sound",
            () -> configCopy.playSound, v -> configCopy.playSound = v);
        addBoolButton(x, y + s * i++, w, h, "config.onekeyminer.option.strict_match",
            () -> configCopy.requireExactMatch, v -> configCopy.requireExactMatch = v);
    }
    
    // === 辅助方法 ===
    
    private void addBoolButton(int x, int y, int w, int h, String key, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        Component label = Component.translatable(key);
        addRenderableWidget(Button.builder(
            Component.literal(label.getString() + ": " + (getter.get() ? "§a开" : "§c关")),
            b -> {
                setter.accept(!getter.get());
                b.setMessage(Component.literal(label.getString() + ": " + (getter.get() ? "§a开" : "§c关")));
            }
        ).bounds(x, y, w, h).build());
    }
    
    private void addIntButton(int x, int y, int w, int h, String key, 
                              Supplier<Integer> getter, Consumer<Integer> setter,
                              int min, int max, int step) {
        Component label = Component.translatable(key);
        addRenderableWidget(Button.builder(
            Component.literal(label.getString() + ": " + getter.get()),
            b -> {
                int next = getter.get() + step;
                if (next > max) next = min;
                setter.accept(next);
                b.setMessage(Component.literal(label.getString() + ": " + getter.get()));
            }
        ).bounds(x, y, w, h).build());
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
    }
    
    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
