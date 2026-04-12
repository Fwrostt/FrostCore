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

        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
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

        FrostLogger.info("Configuration reloaded by " + sender.getName() + ".");
        sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize("<green><bold>✔</bold></green> <white>FrostCore configuration reloaded.</white> <gray>(config.yml, messages.yml, warps.yml, webhooks)"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) return Collections.emptyList();
        if (args.length == 1) {
            return List.of("reload");
        }
        return Collections.emptyList();
    }
}

