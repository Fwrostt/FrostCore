package dev.frost.frostcore.cmds.teleport;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.WarpManager;
import dev.frost.frostcore.utils.TeleportUtil;
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

public class SpawnCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = MessageManager.get();
    private final TeleportUtil teleportUtil = Main.getTeleportUtil();
    private final WarpManager warpManager = Main.getWarpManager();
    private final ConfigManager config = Main.getConfigManager();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!config.getBoolean("spawn.enabled", true)) {
            mm.send(player, "teleport.spawn-disabled");
            return true;
        }

        Location spawn = warpManager.getSpawn();
        if (spawn == null) {
            mm.send(player, "teleport.spawn-not-set");
            return true;
        }

        teleportUtil.teleportWithCooldownAndDelay(
                player, spawn,
                "spawn",
                "spawn.cooldown",
                "teleport.spawn-cooldown",
                config.getInt("spawn.delay", 3),
                "teleport.spawn-wait",
                "teleport.spawn-teleport",
                "teleport.spawn-teleport-cancelled"
        );

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
