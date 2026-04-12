package dev.frost.frostcore.cmds.admin;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
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
import java.util.List;

public class BottomCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("frostcore.admin.bottom")) {
            mm.send(player, "general.no-permission");
            return true;
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
            mm.send(player, "admin.bottom-success");
        } else {
            mm.send(player, "admin.bottom-failed");
        }
        return true;
    }

    private boolean isSafe(Block block) {
        return block.getType().isSolid() &&
               block.getRelative(0, 1, 0).getType() == Material.AIR &&
               block.getRelative(0, 2, 0).getType() == Material.AIR;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
