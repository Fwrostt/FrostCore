package dev.frost.frostcore.cmds.player;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.UtilityManager;
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

public class NickCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = Main.getMessageManager();
    private final UtilityManager um = UtilityManager.getInstance();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("frostcore.utility.nick")) {
            mm.send(sender, "general.no-permission");
            return true;
        }

        Player target;
        String nick;
        boolean removing = label.equalsIgnoreCase("unnick");

        if (args.length == 0) {
            if (removing && sender instanceof Player p) {
                target = p;
                nick = null;
            } else {
                mm.send(sender, "utilities.nick-usage");
                return true;
            }
        } else if (args.length == 1) {
            if (removing) {
                target = Bukkit.getPlayer(args[0]);
                nick = null;
            } else if (sender instanceof Player p) {
                target = p;
                nick = args[0];
            } else {
                mm.send(sender, "utilities.nick-usage");
                return true;
            }
        } else {
            target = Bukkit.getPlayer(args[0]);
            nick = args[1];
        }

        if (target == null) {
            mm.send(sender, "admin.player-not-found");
            return true;
        }

        if (removing) {
            um.removeNickname(target.getUniqueId());
            if (sender.equals(target)) {
                mm.send(target, "utilities.nick-removed");
            } else {
                mm.send(sender, "utilities.nick-removed-other", Map.of("player", target.getName()));
                mm.send(target, "utilities.nick-removed");
            }
        } else {
            um.setNickname(target.getUniqueId(), nick);
            if (sender.equals(target)) {
                mm.send(target, "utilities.nick-success", Map.of("nick", nick));
            } else {
                mm.send(sender, "utilities.nick-success-other", Map.of("player", target.getName(), "nick", nick));
                mm.send(target, "utilities.nick-success", Map.of("nick", nick));
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("frostcore.utility.nick")) return Collections.emptyList();
        
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
