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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SudoCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = Main.getMessageManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("frostcore.admin.sudo")) {
            mm.send(sender, "general.no-permission");
            return true;
        }

        if (args.length < 2) {
            mm.sendRaw(sender, "<#FF5555>/sudo <player> <message|/command>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            mm.send(sender, "admin.player-not-found");
            return true;
        }

        String action = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        if (action.startsWith("/")) {
            target.performCommand(action.substring(1));
        } else {
            target.chat(action);
        }

        mm.send(sender, "admin.sudo-success", Map.of("player", target.getName(), "action", action));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
