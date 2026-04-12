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

public class SpeedCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = Main.getMessageManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player target;
        int speedArgIndex = 1;

        if (args.length >= 3 && sender.hasPermission("frostcore.utility.others")) {
            target = Bukkit.getPlayer(args[2]);
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

        if (!sender.hasPermission("frostcore.utility.speed")) {
            mm.send(sender, "general.no-permission");
            return true;
        }

        if (args.length < 2) {
            mm.send(sender, "utilities.speed-usage");
            return true;
        }

        String type = args[0].toLowerCase();
        int speedVal;
        try {
            speedVal = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            mm.send(sender, "utilities.speed-invalid");
            return true;
        }

        if (speedVal < 1) speedVal = 1;
        if (speedVal > 10) speedVal = 10;
        float fSpeed = speedVal / 10.0f;

        if (type.equals("fly")) {
            target.setFlySpeed(fSpeed);
            mm.send(sender, "utilities.speed-changed", Map.of("type", "fly", "val", String.valueOf(speedVal)));
        } else if (type.equals("walk")) {
            target.setWalkSpeed(Math.min(fSpeed, 1.0f));
            mm.send(sender, "utilities.speed-changed", Map.of("type", "walk", "val", String.valueOf(speedVal)));
        } else {
            mm.send(sender, "utilities.speed-usage");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return List.of("fly", "walk").stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2) return List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
        if (args.length == 3) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
