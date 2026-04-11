package dev.frost.frostcore.cmds;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.WarpManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class SetSpawnCmd implements CommandExecutor, TabCompleter {

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

        warpManager.setSpawn(player.getLocation());
        mm.send(player, "teleport.spawn-set");

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
