package dev.frost.frostcore.cmds.teleport;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RenameHomeCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = MessageManager.get();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("frostcore.renamehome")) {
            mm.sendRaw(player, "<red>You don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            mm.sendRaw(player, "<red>/renamehome <old> <new>");
            return true;
        }

        String oldName = args[0];
        String newName = args[1];

        if (newName.length() > 20) {
            mm.sendRaw(player, "<red>New home name cannot exceed 20 characters.");
            return true;
        }

        if (Main.getHomeManager().getHome(player, oldName) == null) {
            mm.send(player, "homes.not-found", Map.of("home", oldName));
            return true;
        }

        boolean success = Main.getHomeManager().renameHome(player, oldName, newName);
        if (success) {
            mm.send(player, "homes.rename-success", Map.of("old", oldName, "new", newName));
        } else {
            mm.send(player, "homes.rename-failed");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player p && args.length == 1) {
            return new ArrayList<>(Main.getHomeManager().getHomes(p).keySet());
        }
        return Collections.emptyList();
    }
}

