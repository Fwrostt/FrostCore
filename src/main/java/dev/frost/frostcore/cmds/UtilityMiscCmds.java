package dev.frost.frostcore.cmds;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
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

public class UtilityMiscCmds implements CommandExecutor, TabCompleter {

    private final MessageManager mm = Main.getMessageManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use these commands.");
            return true;
        }

        

        String cmd = label.toLowerCase();

        switch (cmd) {
            case "top" -> handleTop(player);
            case "bottom" -> handleBottom(player);
            case "near" -> handleNear(player);
            case "coords" -> handleCoords(player);
        }

        return true;
    }

    private void handleTop(Player player) {
        if (!player.hasPermission("frostcore.utility.top")) {
            mm.send(player, "general.no-permission");
            return;
        }

        Location loc = player.getLocation();
        int topY = player.getWorld().getHighestBlockYAt(loc);
        Location topLoc = new Location(player.getWorld(), loc.getX(), topY + 1, loc.getZ(), loc.getYaw(), loc.getPitch());
        
        player.teleport(topLoc);
        mm.send(player, "utility.top-success");
    }

    private void handleBottom(Player player) {
        if (!player.hasPermission("frostcore.utility.bottom")) {
            mm.send(player, "general.no-permission");
            return;
        }

        Location loc = player.getLocation();
        int minY = player.getWorld().getMinHeight();
        int currentY = loc.getBlockY();
        
        Location target = null;
        
        for (int y = minY; y < currentY; y++) {
            Block block = player.getWorld().getBlockAt(loc.getBlockX(), y, loc.getBlockZ());
            if (isSafe(block)) {
                target = block.getLocation().add(0.5, 1, 0.5);
                target.setYaw(loc.getYaw());
                target.setPitch(loc.getPitch());
                break;
            }
        }

        if (target != null) {
            player.teleport(target);
            mm.send(player, "utility.bottom-success");
        } else {
            mm.send(player, "utility.bottom-failed");
        }
    }

    private boolean isSafe(Block block) {
        return block.getType().isSolid() && 
               block.getRelative(0, 1, 0).getType() == Material.AIR && 
               block.getRelative(0, 2, 0).getType() == Material.AIR;
    }

    private void handleNear(Player player) {
        if (!player.hasPermission("frostcore.utility.near")) {
            mm.send(player, "general.no-permission");
            return;
        }

        double radius = Main.getInstance().getConfig().getDouble("utility.near-radius", 100.0);
        List<Player> nearby = player.getWorld().getPlayers().stream()
                .filter(p -> !p.equals(player))
                .filter(p -> p.getLocation().distance(player.getLocation()) <= radius)
                .sorted(Comparator.comparingDouble(p -> p.getLocation().distance(player.getLocation())))
                .collect(Collectors.toList());

        if (nearby.isEmpty()) {
            mm.send(player, "utility.near-none");
            return;
        }

        mm.send(player, "utility.near-title", Map.of("radius", String.valueOf((int)radius)));
        for (Player p : nearby) {
            int dist = (int) p.getLocation().distance(player.getLocation());
            mm.send(player, "utility.near-player", Map.of("player", p.getName(), "distance", String.valueOf(dist)));
        }
    }

    private void handleCoords(Player player) {
        if (!player.hasPermission("frostcore.utility.coords")) {
            mm.send(player, "general.no-permission");
            return;
        }

        Location loc = player.getLocation();
        mm.send(player, "utility.coords", Map.of(
                "x", String.valueOf(loc.getBlockX()),
                "y", String.valueOf(loc.getBlockY()),
                "z", String.valueOf(loc.getBlockZ()),
                "world", player.getWorld().getName()
        ));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
