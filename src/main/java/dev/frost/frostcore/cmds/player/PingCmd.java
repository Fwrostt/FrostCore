package dev.frost.frostcore.cmds.player;

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

public class PingCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && sender.hasPermission("frostcore.admin.ping.others")) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                mm.send(sender, "admin.player-not-found");
                return true;
            }
            int ping = target.getPing();
            String color = ping < 50 ? "<#7ECFA0>" : ping < 100 ? "<#D4A76A>" : "<#D4727A>";
            mm.sendRaw(sender, "<gradient:#6B8DAE:#8BADC4>FROST <dark_gray>» <#8FA3BF>" + target.getName() + "'s ping: " + color + ping + "ms");
        } else if (sender instanceof Player player) {
            int ping = player.getPing();
            String color = ping < 50 ? "<#7ECFA0>" : ping < 100 ? "<#D4A76A>" : "<#D4727A>";
            mm.sendRaw(player, "<gradient:#6B8DAE:#8BADC4>FROST <dark_gray>» <#8FA3BF>Your ping: " + color + ping + "ms");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("frostcore.admin.ping.others")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
