package dev.frost.frostcore.cmds;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.BackManager;
import dev.frost.frostcore.manager.MessageManager;
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

public class BackCmd implements CommandExecutor, TabCompleter {

    private final BackManager backManager = BackManager.getInstance();
    private final MessageManager mm = Main.getMessageManager();
    private final TeleportUtil teleportUtil = Main.getTeleportUtil();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("frostcore.utility.back")) {
            mm.send(player, "general.no-permission");
            return true;
        }

        Location backLoc = backManager.getLastLocation(player.getUniqueId());
        if (backLoc == null) {
            mm.send(player, "teleport.back-no-location");
            return true;
        }

        teleportUtil.teleportWithCooldownAndDelay(
                player, backLoc,
                "back",
                "back.cooldown",
                "teleport.back-cooldown",
                Main.getConfigManager().getInt("back.delay", 3),
                "teleport.back-wait",
                "teleport.back-teleport",
                "teleport.back-teleport-cancelled"
        );

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
