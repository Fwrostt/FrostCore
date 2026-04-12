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

public class DelHomeCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = MessageManager.get();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("frostcore.delhome")) {
            mm.sendRaw(player, "<red>You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            mm.sendRaw(player, "<red>/delhome <name>");
            return true;
        }

        String homeName = args[0];

        if (Main.getHomeManager().getHome(player, homeName) == null) {
            mm.send(player, "homes.not-found", Map.of("home", homeName));
            return true;
        }

        Main.getHomeManager().deleteHome(player, homeName);
        mm.send(player, "homes.delete-success", Map.of("home", homeName));

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

