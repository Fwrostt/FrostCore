package dev.frost.frostcore.cmds.teleport;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.manager.MessageManager;
import org.bukkit.Location;
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

public class SetHomeCmd implements CommandExecutor, TabCompleter {

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

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player p && args.length == 1) {
            return Main.getHomeManager().getHomes(p).keySet().stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
