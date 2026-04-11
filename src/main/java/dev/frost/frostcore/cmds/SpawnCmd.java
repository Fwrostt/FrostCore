package dev.frost.frostcore.cmds;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.WarpManager;
import dev.frost.frostcore.utils.TeleportUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class SpawnCmd implements CommandExecutor {

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
}
