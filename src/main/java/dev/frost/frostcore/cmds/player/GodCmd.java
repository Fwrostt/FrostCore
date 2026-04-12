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

public class GodCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = Main.getMessageManager();
    private final UtilityManager um = UtilityManager.getInstance();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player target;
        if (args.length > 0 && sender.hasPermission("frostcore.utility.others")) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                mm.send(sender, "admin.player-not-found");
                return true;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage("You must specify a player when using from console.");
            return true;
        }

        if (!sender.hasPermission("frostcore.utility.god")) {
            mm.send(sender, "general.no-permission");
            return true;
        }

        um.toggleGodMode(target.getUniqueId());
        boolean enabled = um.isGodMode(target.getUniqueId());

        if (sender.equals(target)) {
            mm.send(target, "utilities.god-toggle", Map.of("state", enabled ? "<#7ECFA0>Enabled" : "<#D4727A>Disabled"));
        } else {
            mm.send(sender, "utilities.god-toggle-other", Map.of(
                "player", target.getName(),
                "state", enabled ? "<#7ECFA0>Enabled" : "<#D4727A>Disabled"
            ));
            mm.send(target, "utilities.god-toggle", Map.of("state", enabled ? "<#7ECFA0>Enabled" : "<#D4727A>Disabled"));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
