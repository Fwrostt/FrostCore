package dev.frost.frostcore.cmds.admin;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NearCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("frostcore.admin.near")) {
            mm.send(player, "general.no-permission");
            return true;
        }

        double radius = Main.getInstance().getConfig().getDouble("utility.near-radius", 100.0);
        List<Player> nearby = player.getWorld().getPlayers().stream()
                .filter(p -> !p.equals(player))
                .filter(p -> p.getLocation().distance(player.getLocation()) <= radius)
                .sorted(Comparator.comparingDouble(p -> p.getLocation().distance(player.getLocation())))
                .collect(Collectors.toList());

        if (nearby.isEmpty()) {
            mm.send(player, "admin.near-none");
            return true;
        }

        mm.send(player, "admin.near-title", Map.of("radius", String.valueOf((int)radius)));
        for (Player p : nearby) {
            int dist = (int) p.getLocation().distance(player.getLocation());
            mm.send(player, "admin.near-player", Map.of("player", p.getName(), "distance", String.valueOf(dist)));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
