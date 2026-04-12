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
import java.util.Map;
import java.util.stream.Collectors;

public class FlyCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = Main.getMessageManager();

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

        if (!sender.hasPermission("frostcore.utility.fly")) {
            mm.send(sender, "general.no-permission");
            return true;
        }

        boolean flying = !target.getAllowFlight();
        target.setAllowFlight(flying);
        if (flying) target.setFlying(true);

        if (args.length > 1) {
            try {
                float speed = Float.parseFloat(args[1]);
                if (speed < 1) speed = 1;
                if (speed > 10) speed = 10;
                target.setFlySpeed(speed / 10.0f);
            } catch (NumberFormatException ignored) {}
        }

        if (sender.equals(target)) {
            mm.send(target, "utilities.fly-toggle", Map.of("state", flying ? "<#7ECFA0>Enabled" : "<#D4727A>Disabled"));
        } else {
            mm.send(sender, "utilities.fly-toggle-other", Map.of(
                "player", target.getName(),
                "state", flying ? "<#7ECFA0>Enabled" : "<#D4727A>Disabled"
            ));
            mm.send(target, "utilities.fly-toggle", Map.of("state", flying ? "<#7ECFA0>Enabled" : "<#D4727A>Disabled"));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2) return List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
        return Collections.emptyList();
    }
}
