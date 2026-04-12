package dev.frost.frostcore.cmds.admin;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EnderchestCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("frostcore.admin.enderchest")) {
            mm.send(player, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            player.openInventory(player.getEnderChest());
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target != null) {
            player.openInventory(target.getEnderChest());
        } else {
            mm.send(player, "admin.player-not-found");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
