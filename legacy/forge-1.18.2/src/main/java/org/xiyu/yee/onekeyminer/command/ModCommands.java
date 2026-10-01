package org.xiyu.yee.onekeyminer.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.xiyu.yee.onekeyminer.Onekeyminer;
import org.xiyu.yee.onekeyminer.capability.ChainModeCapability;
import org.xiyu.yee.onekeyminer.config.ClientConfig;
import org.xiyu.yee.onekeyminer.config.CommonConfig;
import org.xiyu.yee.onekeyminer.config.ServerConfig;
import org.xiyu.yee.onekeyminer.config.ConfigUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

@EventBusSubscriber(modid = Onekeyminer.MODID)
public class ModCommands {
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(
            Commands.literal("excavation")
                .then(Commands.literal("status")
                    .executes(ModCommands::showStatus))
                .then(Commands.literal("help")
                    .executes(ModCommands::showHelp))
                .then(Commands.literal("common")
                    .requires(source -> source.hasPermission(2))
                    .then(registerCommonConfigCommands()))
                .then(Commands.literal("client")
                    .then(registerClientConfigCommands()))
                .then(Commands.literal("server")
                    .requires(source -> source.hasPermission(4))
                    .then(registerServerConfigCommands()))
                .executes(ModCommands::showHelp)
        );
    }
    
    // 显示帮助信息
    private static int showHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
    source.sendSuccess(new TranslatableComponent("command.onekeyminer.help.title"), false);
    source.sendSuccess(new TranslatableComponent("command.onekeyminer.help.status"), false);
    source.sendSuccess(new TranslatableComponent("command.onekeyminer.help.server"), false); // 新增
    source.sendSuccess(new TranslatableComponent("command.onekeyminer.help.client"), false); // 新增
        return 1;
    }
    
    // 查看状态命令
    private static int showStatus(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
            
            // 显示当前玩家连锁模式状态
            boolean chainActive = ChainModeCapability.isChainModeActive(player);
            context.getSource().sendSuccess(new TranslatableComponent("command.onekeyminer.status", 
                    new TranslatableComponent(chainActive ? 
                        "command.onekeyminer.status.on" : 
                        "command.onekeyminer.status.off")),
                false);
            
            // 显示配置信息
            context.getSource().sendSuccess(new TranslatableComponent("command.onekeyminer.status.maxblocks", 
                    CommonConfig.INSTANCE.maxBlocksInChain.get()),
                false);
            
            context.getSource().sendSuccess(new TranslatableComponent("command.onekeyminer.status.maxdepth", 
                    CommonConfig.INSTANCE.maxChainDepth.get()),
                false);
                
            boolean diagonal = CommonConfig.INSTANCE.enableDiagonalChaining.get();
            context.getSource().sendSuccess(new TranslatableComponent("command.onekeyminer.status.diagonal",
                    new TranslatableComponent(diagonal ? 
                        "command.onekeyminer.enabled" : 
                        "command.onekeyminer.disabled")),
                false);
                
            // 添加蹲下需求状态
            boolean requireSneaking = CommonConfig.INSTANCE.requireSneaking.get();
            context.getSource().sendSuccess(new TranslatableComponent("command.onekeyminer.status.sneaking",
                    new TranslatableComponent(requireSneaking ? 
                        "command.onekeyminer.required" : 
                        "command.onekeyminer.not_required")),
                false);
                
            return 1;
        }
        return 0;
    }
    
    /**
     * 注册通用配置命令
     */
    private static LiteralArgumentBuilder<CommandSourceStack> registerCommonConfigCommands() {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("options");
        
        // 最大连锁方块数
        builder.then(Commands.literal("maxChainBlocks")
            .executes(ctx -> showCommonConfigValue(ctx, "maxChainBlocks", CommonConfig.INSTANCE.maxChainBlocks.get()))
            .then(Commands.argument("value", IntegerArgumentType.integer(1, 4096))
                .executes(ctx -> setCommonConfigIntValue(ctx, "maxChainBlocks", 
                    CommonConfig.INSTANCE.maxChainBlocks, 
                    IntegerArgumentType.getInteger(ctx, "value")))));
        
        // 最大连锁深度
        builder.then(Commands.literal("maxChainDepth")
            .executes(ctx -> showCommonConfigValue(ctx, "maxChainDepth", CommonConfig.INSTANCE.maxChainDepth.get()))
            .then(Commands.argument("value", IntegerArgumentType.integer(1, 64))
                .executes(ctx -> setCommonConfigIntValue(ctx, "maxChainDepth", 
                    CommonConfig.INSTANCE.maxChainDepth, 
                    IntegerArgumentType.getInteger(ctx, "value")))));
        
        // 存活模式最大方块数
        builder.then(Commands.literal("maxBlocksInChain")
            .executes(ctx -> showCommonConfigValue(ctx, "maxBlocksInChain", CommonConfig.INSTANCE.maxBlocksInChain.get()))
            .then(Commands.argument("value", IntegerArgumentType.integer(1, 4096))
                .executes(ctx -> setCommonConfigIntValue(ctx, "maxBlocksInChain", 
                    CommonConfig.INSTANCE.maxBlocksInChain, 
                    IntegerArgumentType.getInteger(ctx, "value")))));
        
        // 创造模式最大方块数
        builder.then(Commands.literal("maxBlocksInChainCreative")
            .executes(ctx -> showCommonConfigValue(ctx, "maxBlocksInChainCreative", CommonConfig.INSTANCE.maxBlocksInChainCreative.get()))
            .then(Commands.argument("value", IntegerArgumentType.integer(1, 16384))
                .executes(ctx -> setCommonConfigIntValue(ctx, "maxBlocksInChainCreative", 
                    CommonConfig.INSTANCE.maxBlocksInChainCreative, 
                    IntegerArgumentType.getInteger(ctx, "value")))));
        
        // 黑名单管理
        builder.then(Commands.literal("nonChainableBlocks")
            .then(Commands.literal("list")
                .executes(ModCommands::listNonChainableBlocks))
            .then(Commands.literal("add")
                .then(Commands.argument("blockId", StringArgumentType.string())
                    .executes(ctx -> addToNonChainableBlocks(ctx, StringArgumentType.getString(ctx, "blockId")))))
            .then(Commands.literal("remove")
                .then(Commands.argument("blockId", StringArgumentType.string())
                    .executes(ctx -> removeFromNonChainableBlocks(ctx, StringArgumentType.getString(ctx, "blockId"))))));
        
        // 种子黑名单管理
        builder.then(Commands.literal("seedBlacklist")
            .then(Commands.literal("list")
                .executes(ModCommands::listSeedBlacklist))
            .then(Commands.literal("add")
                .then(Commands.argument("seedId", StringArgumentType.string())
                    .executes(ctx -> addToSeedBlacklist(ctx, StringArgumentType.getString(ctx, "seedId")))))
            .then(Commands.literal("remove")
                .then(Commands.argument("seedId", StringArgumentType.string())
                    .executes(ctx -> removeFromSeedBlacklist(ctx, StringArgumentType.getString(ctx, "seedId"))))));
        
        // 布尔值配置项
        addBooleanConfigOption(builder, "enableDiagonalChaining", CommonConfig.INSTANCE.enableDiagonalChaining);
        addBooleanConfigOption(builder, "teleportDropsToPlayer", CommonConfig.INSTANCE.teleportDropsToPlayer);
        addBooleanConfigOption(builder, "ignoreToolCompatibility", CommonConfig.INSTANCE.ignoreToolCompatibility);
        addBooleanConfigOption(builder, "matchBlockState", CommonConfig.INSTANCE.matchBlockState);
        addBooleanConfigOption(builder,"matchSeedBlockState",CommonConfig.INSTANCE.matchseedBlockState);
        addBooleanConfigOption(builder, "enableInCreative", CommonConfig.INSTANCE.enableInCreative);
        addBooleanConfigOption(builder, "requireSneaking", CommonConfig.INSTANCE.requireSneaking);
        
        return builder;
    }
    
    /**
     * 为布尔值配置项添加命令
     */
    private static void addBooleanConfigOption(LiteralArgumentBuilder<CommandSourceStack> builder,
                                              String optionName,
                                              ForgeConfigSpec.BooleanValue configValue) {
        builder.then(Commands.literal(optionName)
            .executes(ctx -> showCommonConfigValue(ctx, optionName, configValue.get()))
            .then(Commands.argument("value", BoolArgumentType.bool())
                .executes(ctx -> setCommonConfigBoolValue(ctx, optionName, 
                    configValue, 
                    BoolArgumentType.getBool(ctx, "value")))));
    }
    
    /**
     * 显示通用配置值
     */
    private static int showCommonConfigValue(CommandContext<CommandSourceStack> context, 
                                           String name, 
                                           Object value) throws CommandSyntaxException {
    ServerPlayer player = context.getSource().getPlayerOrException();
    player.sendMessage(new TranslatableComponent("command.onekeyminer.config.value", name, value), player.getUUID());
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 设置整数配置值
     */
    private static int setCommonConfigIntValue(CommandContext<CommandSourceStack> context, 
                                             String name, 
                                             ForgeConfigSpec.IntValue configValue, 
                                             int newValue) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        configValue.set(newValue);
        ConfigUtils.saveConfig(CommonConfig.SPEC);
        player.sendMessage(new net.minecraft.network.chat.TranslatableComponent("command.onekeyminer.config.set.int", name, newValue), player.getUUID());
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 设置布尔值配置值
     */
    private static int setCommonConfigBoolValue(CommandContext<CommandSourceStack> context, 
                                              String name, 
                                              ForgeConfigSpec.BooleanValue configValue, 
                                              boolean newValue) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        configValue.set(newValue);
        ConfigUtils.saveConfig(CommonConfig.SPEC);
        String status = newValue ? 
            new net.minecraft.network.chat.TranslatableComponent("command.onekeyminer.enabled").getString() : 
            new net.minecraft.network.chat.TranslatableComponent("command.onekeyminer.disabled").getString();
        player.sendMessage(new net.minecraft.network.chat.TranslatableComponent("command.onekeyminer.config.set.bool", name, status), player.getUUID());
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 列出不可连锁挖掘的方块
     */
    private static int listNonChainableBlocks(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<? extends String> blocks = CommonConfig.INSTANCE.nonChainableBlocks.get();
        
        player.sendMessage(new net.minecraft.network.chat.TranslatableComponent("command.onekeyminer.blacklist.blocks.title"), player.getUUID());
        if (blocks.isEmpty()) {
            player.sendMessage(new net.minecraft.network.chat.TranslatableComponent("command.onekeyminer.list.empty"), player.getUUID());
        } else {
            for (String block : blocks) {
                player.sendMessage(new net.minecraft.network.chat.TranslatableComponent("command.onekeyminer.list.item", block), player.getUUID());
            }
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 添加方块到不可连锁挖掘列表
     */
    private static int addToNonChainableBlocks(CommandContext<CommandSourceStack> context, String blockId) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<String> blocks = new ArrayList<>(CommonConfig.INSTANCE.nonChainableBlocks.get());
        
        // 如果没有冒号，添加默认命名空间
        if (!blockId.contains(":")) {
            blockId = "minecraft:" + blockId;
        }
        
        if (blocks.contains(blockId)) {
            player.sendMessage(new net.minecraft.network.chat.TranslatableComponent("command.onekeyminer.blacklist.block.exists", blockId), player.getUUID());
            return 0;
        }
        
        blocks.add(blockId);
        CommonConfig.INSTANCE.nonChainableBlocks.set(blocks);
        ConfigUtils.saveConfig(CommonConfig.SPEC);
        
        player.sendMessage(new net.minecraft.network.chat.TranslatableComponent("command.onekeyminer.blacklist.block.added", blockId), player.getUUID());
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 从不可连锁挖掘列表中移除方块
     */
    private static int removeFromNonChainableBlocks(CommandContext<CommandSourceStack> context, String blockId) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<String> blocks = new ArrayList<>(CommonConfig.INSTANCE.nonChainableBlocks.get());
        
        // 如果没有冒号，添加默认命名空间
        if (!blockId.contains(":")) {
            blockId = "minecraft:" + blockId;
        }
        
        if (!blocks.contains(blockId)) {
            player.sendMessage(new net.minecraft.network.chat.TranslatableComponent("command.onekeyminer.blacklist.block.not_exists", blockId), player.getUUID());
            return 0;
        }
        
        blocks.remove(blockId);
        CommonConfig.INSTANCE.nonChainableBlocks.set(blocks);
        ConfigUtils.saveConfig(CommonConfig.SPEC);
        
        player.sendMessage(new net.minecraft.network.chat.TranslatableComponent("command.onekeyminer.blacklist.block.removed", blockId), player.getUUID());
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 列出不可连锁种植的种子
     */
    private static int listSeedBlacklist(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<? extends String> seeds = CommonConfig.INSTANCE.seedBlacklist.get();
        
        player.sendMessage(new net.minecraft.network.chat.TextComponent("§e不可连锁种植的种子列表："), player.getUUID());
        if (seeds.isEmpty()) {
            player.sendMessage(new net.minecraft.network.chat.TextComponent("  §7(列表为空)"), player.getUUID());
        } else {
            for (String seed : seeds) {
                player.sendMessage(new net.minecraft.network.chat.TextComponent("  §7- §r" + seed), player.getUUID());
            }
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 添加种子到不可连锁种植列表
     */
    private static int addToSeedBlacklist(CommandContext<CommandSourceStack> context, String seedId) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<String> seeds = new ArrayList<>(CommonConfig.INSTANCE.seedBlacklist.get());
        
        // 如果没有冒号，添加默认命名空间
        if (!seedId.contains(":")) {
            seedId = "minecraft:" + seedId;
        }
        
        if (seeds.contains(seedId)) {
            player.sendMessage(new net.minecraft.network.chat.TextComponent("§c种子 " + seedId + " 已在黑名单中"), player.getUUID());
            return 0;
        }
        
        seeds.add(seedId);
        CommonConfig.INSTANCE.seedBlacklist.set(seeds);
        ConfigUtils.saveConfig(CommonConfig.SPEC);
        
        player.sendMessage(new net.minecraft.network.chat.TextComponent("§a已将 " + seedId + " 添加到不可连锁种植列表"), player.getUUID());
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 从不可连锁种植列表中移除种子
     */
    private static int removeFromSeedBlacklist(CommandContext<CommandSourceStack> context, String seedId) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<String> seeds = new ArrayList<>(CommonConfig.INSTANCE.seedBlacklist.get());
        
        // 如果没有冒号，添加默认命名空间
        if (!seedId.contains(":")) {
            seedId = "minecraft:" + seedId;
        }
        
        if (!seeds.contains(seedId)) {
            player.sendMessage(new net.minecraft.network.chat.TextComponent("§c种子 " + seedId + " 不在黑名单中"), player.getUUID());
            return 0;
        }
        
        seeds.remove(seedId);
        CommonConfig.INSTANCE.seedBlacklist.set(seeds);
        ConfigUtils.saveConfig(CommonConfig.SPEC);
        
        player.sendMessage(new net.minecraft.network.chat.TextComponent("§a已从不可连锁种植列表中移除 " + seedId), player.getUUID());
        return Command.SINGLE_SUCCESS;
    }

    /**
     * 注册客户端配置命令
     */
    private static LiteralArgumentBuilder<CommandSourceStack> registerClientConfigCommands() {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("options");
        
        // 是否显示方块数量
        addBooleanConfigOption(builder, "showBlockCount", ClientConfig.INSTANCE.showBlockCount);
        
        // 消息样式
        builder.then(Commands.literal("messageStyle")
            .executes(ctx -> showConfigValue(ctx, "messageStyle", ClientConfig.INSTANCE.messageStyle.get()))
            .then(Commands.argument("style", StringArgumentType.word())
                .suggests((context, suggestionBuilder) -> {
                    for (String style : Arrays.asList("chat", "actionbar", "both", "none")) {
                        if (style.startsWith(suggestionBuilder.getRemaining().toLowerCase())) {
                            suggestionBuilder.suggest(style);
                        }
                    }
                    return suggestionBuilder.buildFuture();
                })
                .executes(ctx -> {
                    String style = StringArgumentType.getString(ctx, "style");
                    if (Arrays.asList("chat", "actionbar", "both", "none").contains(style)) {
                        ClientConfig.INSTANCE.messageStyle.set(style);
                        ConfigUtils.saveConfig(ClientConfig.SPEC);
                        ServerPlayer _p = ctx.getSource().getPlayerOrException();
                        _p.sendMessage(new net.minecraft.network.chat.TranslatableComponent("command.onekeyminer.config.messagestyle.set", style), _p.getUUID());
                        return Command.SINGLE_SUCCESS;
                    } else {
                        ServerPlayer _p = ctx.getSource().getPlayerOrException();
                        _p.sendMessage(new net.minecraft.network.chat.TranslatableComponent("command.onekeyminer.invalid_option"), _p.getUUID());
                        return 0;
                    }
                })));
        
        // 按键模式
        builder.then(Commands.literal("keyMode")
            .executes(ctx -> showConfigValue(ctx, "keyMode", ClientConfig.INSTANCE.requireKeyHold.get()))
            .then(Commands.argument("mode", StringArgumentType.word())
                .suggests((context, suggestionBuilder) -> {
                    for (String mode : Arrays.asList("toggle", "hold")) {
                        if (mode.startsWith(suggestionBuilder.getRemaining().toLowerCase())) {
                            suggestionBuilder.suggest(mode);
                        }
                    }
                    return suggestionBuilder.buildFuture();
                })
                .executes(ctx -> {
                    String mode = StringArgumentType.getString(ctx, "mode");
                    if (Arrays.asList("toggle", "hold").contains(mode)) {
                        ClientConfig.INSTANCE.requireKeyHold.set(mode.equals("hold"));
                        ConfigUtils.saveConfig(ClientConfig.SPEC);
                        ServerPlayer _p = ctx.getSource().getPlayerOrException();
                        _p.sendMessage(new net.minecraft.network.chat.TranslatableComponent("command.onekeyminer.config.keyhold.set", mode), _p.getUUID());
                        return Command.SINGLE_SUCCESS;
                    } else {
                        ServerPlayer _p = ctx.getSource().getPlayerOrException();
                        _p.sendMessage(new net.minecraft.network.chat.TranslatableComponent("command.onekeyminer.invalid_option"), _p.getUUID());
                        return 0;
                    }
                })));
         builder.then(Commands.literal("Debug")
            .executes(ctx -> showConfigValue(ctx, "Debug", ClientConfig.INSTANCE.Debug.get()))
            .then(Commands.argument("value", BoolArgumentType.bool())
                .executes(ctx -> setCommonConfigBoolValue(ctx, "Debug", 
                    ClientConfig.INSTANCE.Debug, 
                    BoolArgumentType.getBool(ctx, "value")))));
        builder.then(Commands.literal("frozen timer")
            .executes(ctx -> showConfigValue(ctx, "frozen timer", ClientConfig.INSTANCE.frozentimer.get()))
            .then(Commands.argument("value", IntegerArgumentType.integer(1, 32767))
                .executes(ctx -> setCommonConfigIntValue(ctx, "frozen timer", 
                    ClientConfig.INSTANCE.frozentimer, 
                    IntegerArgumentType.getInteger(ctx, "value")))));
        return builder;
    }

    /**
     * 注册服务器配置命令
     */
    private static LiteralArgumentBuilder<CommandSourceStack> registerServerConfigCommands() {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("options");
        
        // 工具耐久阈值
        builder.then(Commands.literal("toolDurabilityThreshold")
            .executes(ctx -> showConfigValue(ctx, "toolDurabilityThreshold", ServerConfig.INSTANCE.toolDurabilityThreshold.get()))
            .then(Commands.argument("value", DoubleArgumentType.doubleArg(0, 1000))
                .executes(ctx -> {
                    double value = DoubleArgumentType.getDouble(ctx, "value");
                    ServerConfig.INSTANCE.toolDurabilityThreshold.set(value);
                    ConfigUtils.saveConfig(ServerConfig.SPEC);
                    ServerPlayer _p = ctx.getSource().getPlayerOrException();
                    _p.sendMessage(new net.minecraft.network.chat.TranslatableComponent("command.onekeyminer.config.tool_durability.set", value), _p.getUUID());
                    return Command.SINGLE_SUCCESS;
                })));
        
        // 饥饿度阈值
        builder.then(Commands.literal("hungerThreshold")
            .executes(ctx -> showConfigValue(ctx, "hungerThreshold", ServerConfig.INSTANCE.hungerThreshold.get()))
            .then(Commands.argument("value", DoubleArgumentType.doubleArg(0, 20))
                .executes(ctx -> {
                    double value = DoubleArgumentType.getDouble(ctx, "value");
                    ServerConfig.INSTANCE.hungerThreshold.set(value);
                    ConfigUtils.saveConfig(ServerConfig.SPEC);
                    ServerPlayer _p = ctx.getSource().getPlayerOrException();
                    _p.sendMessage(new net.minecraft.network.chat.TranslatableComponent("command.onekeyminer.config.hunger.set", value), _p.getUUID());
                    return Command.SINGLE_SUCCESS;
                })));
        
        return builder;
    }
    
    /**
     * 显示配置值（通用方法）
     */
    private static int showConfigValue(CommandContext<CommandSourceStack> context, 
                                     String name, 
                                     Object value) throws CommandSyntaxException {
    ServerPlayer player = context.getSource().getPlayerOrException();
    player.sendMessage(new net.minecraft.network.chat.TranslatableComponent("command.onekeyminer.config.value", name, value), player.getUUID());
        return Command.SINGLE_SUCCESS;
    }
} 