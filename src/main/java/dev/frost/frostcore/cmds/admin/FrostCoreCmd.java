package dev.frost.frostcore.cmds.admin;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.utils.FrostLogger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;


public class FrostCoreCmd implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "frostcore.admin";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize("<red>You don't have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize("<#B0C4FF>/frostcore reload"));
            sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize("<#B0C4FF>/frostcore chat <reload|debug>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("chat")) {
            if (args.length > 1) {
                if (args[1].equalsIgnoreCase("reload")) {
                    if (Main.getChatManager() != null && Main.getChatManager().getPipeline() != null) {
                        Main.getChatManager().getPipeline().reload();
                        sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                                .deserialize("<green><bold>✔</bold></green> <white>Chat filter pipeline reloaded.</white>"));
                    }
                } else if (args[1].equalsIgnoreCase("debug")) {
                    dev.frost.frostcore.chat.ChatPipeline.DEBUG = !dev.frost.frostcore.chat.ChatPipeline.DEBUG;
                    sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                            .deserialize("<green><bold>✔</bold></green> <white>Chat filter debug is now: " + dev.frost.frostcore.chat.ChatPipeline.DEBUG + "</white>"));
                } else {
                    sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                            .deserialize("<#B0C4FF>/frostcore chat <reload|debug>"));
                }
            } else {
                sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        .deserialize("<#B0C4FF>/frostcore chat <reload|debug>"));
            }
            return true;
        }

        if (!args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize("<#B0C4FF>/frostcore reload"));
            return true;
        }

        ConfigManager config = Main.getConfigManager();
        if (config != null) {
            config.reloadConfig();
        }

        MessageManager messages = Main.getMessageManager();
        if (messages != null) {
            messages.reload();
        }

        if (Main.getWarpManager() != null) {
            Main.getWarpManager().reloadWarpsYml();
        }

        if (Main.getWebhookManager() != null) {
            Main.getWebhookManager().reload();
        }

        if (Main.getMaceManager() != null) {
            Main.getMaceManager().reload();
        }

        if (Main.getChatManager() != null) {
            Main.getChatManager().reload();
        }

        if (Main.getCommandManager() != null) {
            Main.getCommandManager().reload();
        }

        FrostLogger.info("Configuration reloaded by " + sender.getName() + ".");
        sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize("<green><bold>✔</bold></green> <white>FrostCore configuration reloaded.</white> <gray>(config.yml, messages.yml, chat-format.yml, commands.yml, warps.yml, webhooks, mace limiter)"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) return Collections.emptyList();
        if (args.length == 1) {
            return java.util.stream.Stream.of("reload", "chat")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("chat")) {
            return java.util.stream.Stream.of("reload", "debug")
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}

