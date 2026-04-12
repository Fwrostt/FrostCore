package dev.frost.frostcore.cmds.teleport;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.utils.TeleportUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OfflineTpCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = MessageManager.get();
    private final TeleportUtil teleportUtil = Main.getTeleportUtil();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("frostcore.admin")) {
            mm.sendRaw(player, "<red>You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            mm.sendRaw(player, "<#B0C4FF>Usage: <white>/otp <offlineplayer>");
            return true;
        }

        final String targetName = args[0];

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            OfflinePlayer target = Arrays.stream(Bukkit.getOfflinePlayers())
                    .filter(p -> p.getName() != null && p.getName().equalsIgnoreCase(targetName))
                    .findFirst()
                    .orElse(null);

            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                if (!player.isOnline()) return;

                if (target == null) {
                    mm.send(player, "teleport.player-not-found");
                    return;
                }

                Location loc = target.getLocation();
                if (loc == null || loc.getWorld() == null) {
                    String name = target.getName() != null ? target.getName() : targetName;
                    mm.send(player, "teleport.tp-offline-no-data", Map.of("player", name));
                    return;
                }

                String name = target.getName() != null ? target.getName() : targetName;
                teleportUtil.teleportInstant(player, loc);
                mm.send(player, "teleport.tp-offline", Map.of("player", name));
            });
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        return Collections.emptyList();
    }
}

