package org.xiyu.yee.onekeyminer.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextComponentString;
import org.xiyu.yee.onekeyminer.config.ClientConfig;
import org.xiyu.yee.onekeyminer.config.CommonConfig;
import org.xiyu.yee.onekeyminer.config.ServerConfig;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Minecraft 1.12.2 命令实现
 * 使用旧的 CommandBase 系统而不是 Brigadier
 */
public class ModCommands extends CommandBase {
    
    @Override
    public String getName() {
        return "excavation";
    }
    
    @Override
    public String getUsage(ICommandSender sender) {
        return "command.onekeyminer.help.title";
    }
    
    @Override
    public int getRequiredPermissionLevel() {
        return 0; // 所有玩家都可以使用基本命令
    }
    
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            showHelp(sender);
            return;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
                showHelp(sender);
                break;
            case "status":
                showStatus(sender);
                break;
            case "common":
            case "general":
                if (!sender.canUseCommand(2, getName())) {
                    throw new CommandException("commands.generic.permission");
                }
                handleCommonConfig(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "client":
                handleClientConfig(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "server":
                if (!sender.canUseCommand(4, getName())) {
                    throw new CommandException("commands.generic.permission");
                }
                handleServerConfig(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            default:
                showHelp(sender);
                break;
        }
    }
    
    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "help", "status", "common", "client", "server");
        }
        
        if (args.length >= 2) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "common":
                case "general":
                    return getCommonConfigTabCompletion(args);
                case "client":
                    return getClientConfigTabCompletion(args);
                case "server":
                    return getServerConfigTabCompletion(args);
            }
        }
        
        return Collections.emptyList();
    }
    
    private void showHelp(ICommandSender sender) {
        sender.sendMessage(new TextComponentTranslation("command.onekeyminer.help.title"));
        sender.sendMessage(new TextComponentTranslation("command.onekeyminer.help.status"));
        sender.sendMessage(new TextComponentTranslation("command.onekeyminer.help.config"));
        sender.sendMessage(new TextComponentTranslation("command.onekeyminer.help.server"));
        sender.sendMessage(new TextComponentTranslation("command.onekeyminer.help.client"));
    }
    
    private void showStatus(ICommandSender sender) throws CommandException {
        EntityPlayer player = getCommandSenderAsPlayer(sender);
        
        // 显示配置信息
        sender.sendMessage(new TextComponentTranslation("command.onekeyminer.status.maxblocks", 
            CommonConfig.INSTANCE.maxBlocksInChain.get()));
        sender.sendMessage(new TextComponentTranslation("command.onekeyminer.status.maxdepth", 
            CommonConfig.INSTANCE.maxChainDepth.get()));
        
        boolean diagonal = CommonConfig.INSTANCE.enableDiagonalChaining.get();
        sender.sendMessage(new TextComponentTranslation("command.onekeyminer.status.diagonal",
            new TextComponentTranslation(diagonal ? "command.onekeyminer.enabled" : "command.onekeyminer.disabled")));
        
        boolean requireSneaking = CommonConfig.INSTANCE.requireSneaking.get();
        sender.sendMessage(new TextComponentTranslation("command.onekeyminer.status.sneaking",
            new TextComponentTranslation(requireSneaking ? "command.onekeyminer.required" : "command.onekeyminer.not_required")));
    }
    
    private void handleCommonConfig(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.sendMessage(new TextComponentString("§e可用选项: maxChainBlocks, maxChainDepth, maxBlocksInChain, maxBlocksInChainCreative, enableDiagonalChaining, teleportDropsToPlayer, ignoreToolCompatibility, matchIBlockState, matchseedIBlockState, enableInCreative, requireSneaking, nonChainableBlocks, seedBlacklist"));
            return;
        }
        
        String option = args[0].toLowerCase();
        
        switch (option) {
            case "maxchainblocks":
                if (args.length == 1) {
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.value", 
                        "maxChainBlocks", CommonConfig.INSTANCE.maxChainBlocks.get()));
                } else {
                    int value = parseInt(args[1], 1, 4096);
                    CommonConfig.INSTANCE.maxChainBlocks.set(value);
                    CommonConfig.save();
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.set.int", 
                        "maxChainBlocks", value));
                }
                break;
            case "maxchaindepth":
                if (args.length == 1) {
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.value", 
                        "maxChainDepth", CommonConfig.INSTANCE.maxChainDepth.get()));
                } else {
                    int value = parseInt(args[1], 1, 64);
                    CommonConfig.INSTANCE.maxChainDepth.set(value);
                    CommonConfig.save();
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.set.int", 
                        "maxChainDepth", value));
                }
                break;
            case "maxblocksinchain":
                if (args.length == 1) {
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.value", 
                        "maxBlocksInChain", CommonConfig.INSTANCE.maxBlocksInChain.get()));
                } else {
                    int value = parseInt(args[1], 1, 4096);
                    CommonConfig.INSTANCE.maxBlocksInChain.set(value);
                    CommonConfig.save();
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.set.int", 
                        "maxBlocksInChain", value));
                }
                break;
            case "maxblocksinchaincreative":
                if (args.length == 1) {
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.value", 
                        "maxBlocksInChainCreative", CommonConfig.INSTANCE.maxBlocksInChainCreative.get()));
                } else {
                    int value = parseInt(args[1], 1, 16384);
                    CommonConfig.INSTANCE.maxBlocksInChainCreative.set(value);
                    CommonConfig.save();
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.set.int", 
                        "maxBlocksInChainCreative", value));
                }
                break;
            case "enablediagonalchaining":
                handleBooleanConfig(sender, args, "enableDiagonalChaining", CommonConfig.INSTANCE.enableDiagonalChaining);
                break;
            case "teleportdropstoplayer":
                handleBooleanConfig(sender, args, "teleportDropsToPlayer", CommonConfig.INSTANCE.teleportDropsToPlayer);
                break;
            case "ignoretoolcompatibility":
                handleBooleanConfig(sender, args, "ignoreToolCompatibility", CommonConfig.INSTANCE.ignoreToolCompatibility);
                break;
            case "matchiblockstate":
            case "matchblockstate":
                handleBooleanConfig(sender, args, "matchIBlockState", CommonConfig.INSTANCE.matchIBlockState);
                break;
            case "matchseediblockstate":
                handleBooleanConfig(sender, args, "matchseedIBlockState", CommonConfig.INSTANCE.matchseedIBlockState);
                break;
            case "enableincreative":
                handleBooleanConfig(sender, args, "enableInCreative", CommonConfig.INSTANCE.enableInCreative);
                break;
            case "requiresneaking":
                handleBooleanConfig(sender, args, "requireSneaking", CommonConfig.INSTANCE.requireSneaking);
                break;
            case "nonchainableblocks":
                handleBlockBlacklist(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "seedblacklist":
                handleSeedBlacklist(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            default:
                sender.sendMessage(new TextComponentTranslation("command.onekeyminer.invalid_option"));
                break;
        }
    }
    
    private void handleBooleanConfig(ICommandSender sender, String[] args, String name, CommonConfig.BooleanValue configValue) throws CommandException {
        if (args.length == 1) {
            sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.value", name, configValue.get()));
        } else {
            boolean value = parseBoolean(args[1]);
            configValue.set(value);
            CommonConfig.save();
            String status = value ? 
                new TextComponentTranslation("command.onekeyminer.enabled").getUnformattedText() : 
                new TextComponentTranslation("command.onekeyminer.disabled").getUnformattedText();
            sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.set.bool", name, status));
        }
    }
    
    private void handleBlockBlacklist(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            List<String> blocks = CommonConfig.INSTANCE.nonChainableBlocks.get();
            sender.sendMessage(new TextComponentTranslation("command.onekeyminer.blacklist.blocks.title"));
            if (blocks.isEmpty()) {
                sender.sendMessage(new TextComponentTranslation("command.onekeyminer.list.empty"));
            } else {
                for (String block : blocks) {
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.list.item", block));
                }
            }
        } else if (args[0].equalsIgnoreCase("add") && args.length > 1) {
            String blockId = args[1];
            if (!blockId.contains(":")) {
                blockId = "minecraft:" + blockId;
            }
            
            List<String> blocks = new ArrayList<>(CommonConfig.INSTANCE.nonChainableBlocks.get());
            if (blocks.contains(blockId)) {
                sender.sendMessage(new TextComponentTranslation("command.onekeyminer.blacklist.block.exists", blockId));
            } else {
                blocks.add(blockId);
                CommonConfig.INSTANCE.nonChainableBlocks.set(blocks);
                CommonConfig.save();
                sender.sendMessage(new TextComponentTranslation("command.onekeyminer.blacklist.block.added", blockId));
            }
        } else if (args[0].equalsIgnoreCase("remove") && args.length > 1) {
            String blockId = args[1];
            if (!blockId.contains(":")) {
                blockId = "minecraft:" + blockId;
            }
            
            List<String> blocks = new ArrayList<>(CommonConfig.INSTANCE.nonChainableBlocks.get());
            if (!blocks.contains(blockId)) {
                sender.sendMessage(new TextComponentTranslation("command.onekeyminer.blacklist.block.not_exists", blockId));
            } else {
                blocks.remove(blockId);
                CommonConfig.INSTANCE.nonChainableBlocks.set(blocks);
                CommonConfig.save();
                sender.sendMessage(new TextComponentTranslation("command.onekeyminer.blacklist.block.removed", blockId));
            }
        }
    }
    
    private void handleSeedBlacklist(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            List<String> seeds = CommonConfig.INSTANCE.seedBlacklist.get();
            sender.sendMessage(new TextComponentTranslation("command.onekeyminer.blacklist.seeds.title"));
            if (seeds.isEmpty()) {
                sender.sendMessage(new TextComponentTranslation("command.onekeyminer.list.empty"));
            } else {
                for (String seed : seeds) {
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.list.item", seed));
                }
            }
        } else if (args[0].equalsIgnoreCase("add") && args.length > 1) {
            String seedId = args[1];
            if (!seedId.contains(":")) {
                seedId = "minecraft:" + seedId;
            }
            
            List<String> seeds = new ArrayList<>(CommonConfig.INSTANCE.seedBlacklist.get());
            if (seeds.contains(seedId)) {
                sender.sendMessage(new TextComponentTranslation("command.onekeyminer.blacklist.seed.exists", seedId));
            } else {
                seeds.add(seedId);
                CommonConfig.INSTANCE.seedBlacklist.set(seeds);
                CommonConfig.save();
                sender.sendMessage(new TextComponentTranslation("command.onekeyminer.blacklist.seed.added", seedId));
            }
        } else if (args[0].equalsIgnoreCase("remove") && args.length > 1) {
            String seedId = args[1];
            if (!seedId.contains(":")) {
                seedId = "minecraft:" + seedId;
            }
            
            List<String> seeds = new ArrayList<>(CommonConfig.INSTANCE.seedBlacklist.get());
            if (!seeds.contains(seedId)) {
                sender.sendMessage(new TextComponentTranslation("command.onekeyminer.blacklist.seed.not_exists", seedId));
            } else {
                seeds.remove(seedId);
                CommonConfig.INSTANCE.seedBlacklist.set(seeds);
                CommonConfig.save();
                sender.sendMessage(new TextComponentTranslation("command.onekeyminer.blacklist.seed.removed", seedId));
            }
        }
    }
    
    private void handleClientConfig(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.sendMessage(new TextComponentString("§e可用选项: showBlockCount, messageStyle, requireKeyHold, Debug, frozentimer"));
            return;
        }
        
        String option = args[0].toLowerCase();
        
        switch (option) {
            case "showblockcount":
                if (args.length == 1) {
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.value", 
                        "showBlockCount", ClientConfig.INSTANCE.showBlockCount.get()));
                } else {
                    boolean value = parseBoolean(args[1]);
                    ClientConfig.INSTANCE.showBlockCount.set(value);
                    ClientConfig.save();
                    String status = value ? 
                        new TextComponentTranslation("command.onekeyminer.enabled").getUnformattedText() : 
                        new TextComponentTranslation("command.onekeyminer.disabled").getUnformattedText();
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.set.bool", "showBlockCount", status));
                }
                break;
            case "messagestyle":
                if (args.length == 1) {
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.value", 
                        "messageStyle", ClientConfig.INSTANCE.messageStyle.get()));
                } else {
                    String style = args[1].toLowerCase();
                    if (Arrays.asList("chat", "actionbar", "both", "none").contains(style)) {
                        ClientConfig.INSTANCE.messageStyle.set(style);
                        ClientConfig.save();
                        sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.messagestyle.set", style));
                    } else {
                        sender.sendMessage(new TextComponentTranslation("command.onekeyminer.invalid_option"));
                    }
                }
                break;
            case "requirekeyhold":
                if (args.length == 1) {
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.value", 
                        "requireKeyHold", ClientConfig.INSTANCE.requireKeyHold.get()));
                } else {
                    boolean value = parseBoolean(args[1]);
                    ClientConfig.INSTANCE.requireKeyHold.set(value);
                    ClientConfig.save();
                    String status = value ? 
                        new TextComponentTranslation("command.onekeyminer.enabled").getUnformattedText() : 
                        new TextComponentTranslation("command.onekeyminer.disabled").getUnformattedText();
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.set.bool", "requireKeyHold", status));
                }
                break;
            case "debug":
                if (args.length == 1) {
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.value", 
                        "Debug", ClientConfig.INSTANCE.Debug.get()));
                } else {
                    boolean value = parseBoolean(args[1]);
                    ClientConfig.INSTANCE.Debug.set(value);
                    ClientConfig.save();
                    String status = value ? 
                        new TextComponentTranslation("command.onekeyminer.enabled").getUnformattedText() : 
                        new TextComponentTranslation("command.onekeyminer.disabled").getUnformattedText();
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.set.bool", "Debug", status));
                }
                break;
            case "frozentimer":
                if (args.length == 1) {
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.value", 
                        "frozentimer", ClientConfig.INSTANCE.frozentimer.get()));
                } else {
                    int value = parseInt(args[1], 1, 32767);
                    ClientConfig.INSTANCE.frozentimer.set(value);
                    ClientConfig.save();
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.set.int", 
                        "frozentimer", value));
                }
                break;
            default:
                sender.sendMessage(new TextComponentTranslation("command.onekeyminer.invalid_option"));
                break;
        }
    }
    
    private void handleServerConfig(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.sendMessage(new TextComponentString("§e可用选项: toolProtectionEnabled, toolDurabilityThreshold, hungerProtectionEnabled, hungerThreshold"));
            return;
        }
        
        String option = args[0].toLowerCase();
        
        switch (option) {
            case "toolprotectionenabled":
                if (args.length == 1) {
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.value", 
                        "toolProtectionEnabled", ServerConfig.INSTANCE.toolProtectionEnabled.get()));
                } else {
                    boolean value = parseBoolean(args[1]);
                    ServerConfig.INSTANCE.toolProtectionEnabled.set(value);
                    ServerConfig.save();
                    String status = value ? 
                        new TextComponentTranslation("command.onekeyminer.enabled").getUnformattedText() : 
                        new TextComponentTranslation("command.onekeyminer.disabled").getUnformattedText();
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.set.bool", "toolProtectionEnabled", status));
                }
                break;
            case "tooldurability":
            case "tooldurабилитythreshold":
                if (args.length == 1) {
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.value", 
                        "toolDurabilityThreshold", ServerConfig.INSTANCE.toolDurabilityThreshold.get()));
                } else {
                    double value = parseDouble(args[1], 0.0, 1000.0);
                    ServerConfig.INSTANCE.toolDurabilityThreshold.set(value);
                    ServerConfig.save();
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.tool_durability.set", value));
                }
                break;
            case "hungerprotectionenabled":
                if (args.length == 1) {
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.value", 
                        "hungerProtectionEnabled", ServerConfig.INSTANCE.hungerProtectionEnabled.get()));
                } else {
                    boolean value = parseBoolean(args[1]);
                    ServerConfig.INSTANCE.hungerProtectionEnabled.set(value);
                    ServerConfig.save();
                    String status = value ? 
                        new TextComponentTranslation("command.onekeyminer.enabled").getUnformattedText() : 
                        new TextComponentTranslation("command.onekeyminer.disabled").getUnformattedText();
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.set.bool", "hungerProtectionEnabled", status));
                }
                break;
            case "hunger":
            case "hungerthreshold":
                if (args.length == 1) {
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.value", 
                        "hungerThreshold", ServerConfig.INSTANCE.hungerThreshold.get()));
                } else {
                    double value = parseDouble(args[1], 0.0, 20.0);
                    ServerConfig.INSTANCE.hungerThreshold.set(value);
                    ServerConfig.save();
                    sender.sendMessage(new TextComponentTranslation("command.onekeyminer.config.hunger.set", value));
                }
                break;
            default:
                sender.sendMessage(new TextComponentTranslation("command.onekeyminer.invalid_option"));
                break;
        }
    }
    
    private List<String> getCommonConfigTabCompletion(String[] args) {
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, "maxChainBlocks", "maxChainDepth", "maxBlocksInChain", 
                "maxBlocksInChainCreative", "enableDiagonalChaining", "teleportDropsToPlayer", 
                "ignoreToolCompatibility", "matchIBlockState", "matchseedIBlockState", 
                "enableInCreative", "requireSneaking", "nonChainableBlocks", "seedBlacklist");
        } else if (args.length == 3) {
            String option = args[1].toLowerCase();
            if (option.equals("nonchainableblocks") || option.equals("seedblacklist")) {
                return getListOfStringsMatchingLastWord(args, "list", "add", "remove");
            } else if (option.startsWith("enable") || option.startsWith("ignore") || 
                       option.startsWith("match") || option.startsWith("require") || 
                       option.equals("teleportdropstoplayer")) {
                return getListOfStringsMatchingLastWord(args, "true", "false");
            }
        }
        return Collections.emptyList();
    }
    
    private List<String> getClientConfigTabCompletion(String[] args) {
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, "showBlockCount", "messageStyle", "requireKeyHold", "Debug", "frozentimer");
        } else if (args.length == 3) {
            String option = args[1].toLowerCase();
            if (option.equals("messagestyle")) {
                return getListOfStringsMatchingLastWord(args, "chat", "actionbar", "both", "none");
            } else if (option.equals("showblockcount") || option.equals("requirekeyhold") || option.equals("debug")) {
                return getListOfStringsMatchingLastWord(args, "true", "false");
            }
        }
        return Collections.emptyList();
    }
    
    private List<String> getServerConfigTabCompletion(String[] args) {
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, "toolProtectionEnabled", "toolDurabilityThreshold", 
                "hungerProtectionEnabled", "hungerThreshold");
        } else if (args.length == 3) {
            String option = args[1].toLowerCase();
            if (option.equals("toolprotectionenabled") || option.equals("hungerprotectionenabled")) {
                return getListOfStringsMatchingLastWord(args, "true", "false");
            }
        }
        return Collections.emptyList();
    }
}
