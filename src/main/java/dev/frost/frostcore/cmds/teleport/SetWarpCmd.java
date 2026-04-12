package dev.frost.frostcore.cmds.teleport;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.WarpManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SetWarpCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = MessageManager.get();
    private final WarpManager warpManager = Main.getWarpManager();

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

        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("setwarp")) {
            if (args.length < 1) {
                mm.sendRaw(player, "<#B0C4FF>/setwarp <name>");
                return true;
            }

            String warpName = args[0].toLowerCase();
            warpManager.setWarp(warpName, player.getLocation());
            mm.send(player, "teleport.warp-set", Map.of("warp", warpName));
            return true;
        } else if (cmdName.equals("delwarp")) {
            if (args.length < 1) {
                mm.sendRaw(player, "<#B0C4FF>/delwarp <name>");
                return true;
            }

            String warpName = args[0].toLowerCase();
            if (!warpManager.hasWarp(warpName)) {
                mm.send(player, "teleport.warp-not-found", Map.of("warp", warpName));
                return true;
            }

            warpManager.deleteWarp(warpName);
            mm.send(player, "teleport.warp-deleted", Map.of("warp", warpName));
            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("delwarp") && args.length == 1) {
            return new ArrayList<>(warpManager.getWarpNames());
        }
        return Collections.emptyList();
    }
}

