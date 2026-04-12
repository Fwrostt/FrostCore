package dev.frost.frostcore.cmds.messaging;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.PrivateMessageManager;
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
import java.util.Map;
import java.util.stream.Collectors;

public class IgnoreCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = Main.getMessageManager();
    private final PrivateMessageManager pmm = PrivateMessageManager.getInstance();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length < 1) {
            mm.sendRaw(player, "<#FF5555>Usage: /ignore <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            mm.send(player, "admin.player-not-found");
            return true;
        }
        if (target.equals(player)) {
            mm.sendRaw(player, "<#FF5555>You can't ignore yourself!");
            return true;
        }

        boolean nowIgnoring = pmm.toggleIgnore(player.getUniqueId(), target.getUniqueId());
        if (nowIgnoring) {
            mm.send(player, "message.ignore-on", Map.of("player", target.getName()));
        } else {
            mm.send(player, "message.ignore-off", Map.of("player", target.getName()));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
