package dev.frost.frostcore.cmds;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.manager.MessageManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class SetHomeCmd implements CommandExecutor {

    private final MessageManager mm = MessageManager.get();
    private final ConfigManager config = Main.getConfigManager();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("frostcore.sethome")) {
            mm.sendRaw(player, "<red>You don't have permission to use this command.");
            return true;
        }

        if (!config.getBoolean("homes.enabled", true)) {
            mm.send(player, "homes.disabled");
            return true;
        }

        String homeName = args.length > 0 ? args[0] : "Home";

        if (homeName.length() > 20) {
            mm.sendRaw(player, "<red>Home name cannot exceed 20 characters.");
            return true;
        }

        Location loc = player.getLocation();

        boolean success = Main.getHomeManager().setHome(player, homeName, loc);

        if (success) {
            mm.send(player, "homes.set-success", Map.of("home", homeName));
        } else {
            int max = Main.getHomeManager().getMaxHomes(player);
            mm.send(player, "homes.max-reached", Map.of("max", String.valueOf(max)));
        }

        return true;
    }
}

