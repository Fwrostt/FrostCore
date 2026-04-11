package dev.frost.frostcore.cmds;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.WarpManager;
import dev.frost.frostcore.utils.TeleportUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class WarpCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = MessageManager.get();
    private final TeleportUtil teleportUtil = Main.getTeleportUtil();
    private final WarpManager warpManager = Main.getWarpManager();
    private final ConfigManager config = Main.getConfigManager();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!config.getBoolean("warps.enabled", true)) {
            mm.send(player, "teleport.warp-disabled");
            return true;
        }

        if (args.length < 1) {
            showWarpList(player);
            return true;
        }

        String warpName = args[0].toLowerCase();
        Location loc = warpManager.getWarp(warpName);

        if (loc == null) {
            mm.send(player, "teleport.warp-not-found", Map.of("warp", warpName));
            return true;
        }

        // Check per-warp permission (configured in warps.yml)
        dev.frost.frostcore.manager.WarpItemConfig warpCfg = warpManager.getWarpConfig(warpName);
        if (warpCfg.requiresPermission() && !player.hasPermission(warpCfg.getPermission())) {
            mm.sendRaw(player, "<red>✘ You don't have permission to use the <white>"
                    + warpName + "</white> warp.");
            return true;
        }

        teleportUtil.teleportWithCooldownAndDelay(
                player, loc,
                "warp",
                "warps.cooldown",
                "teleport.warp-cooldown",
                config.getInt("warps.delay", 3),
                "teleport.warp-wait",
                "teleport.warp-teleport",
                "teleport.warp-teleport-cancelled",
                Map.of("warp", warpName)
        );

        return true;
    }

    private void showWarpList(Player player) {
        Set<String> warpNames = warpManager.getWarpNames();
        if (warpNames.isEmpty()) {
            mm.send(player, "teleport.no-warps");
            return;
        }

        String div = "<gradient:#55CDFC:#7B68EE><strikethrough>                                                          </strikethrough></gradient>";

        mm.sendRaw(player, "");
        mm.sendRaw(player, div);
        mm.sendRaw(player, " <gradient:#55CDFC:#7B68EE><bold>✦ Server Warps</bold></gradient>");
        mm.sendRaw(player, "");

        Component warpLine = Component.empty().append(miniMessage.deserialize(" "));
        int count = 0;
        for (String name : warpNames) {
            Component warp = miniMessage.deserialize("<#B0C4FF>• <white>" + name + "</white>")
                    .clickEvent(ClickEvent.runCommand("/warp " + name))
                    .hoverEvent(HoverEvent.showText(miniMessage.deserialize("<#A3FFA3>Click to warp to <white>" + name + "</white>")));

            if (count > 0 && count % 3 == 0) {
                player.sendMessage(warpLine);
                warpLine = Component.empty().append(miniMessage.deserialize(" "));
            } else if (count > 0) {
                warpLine = warpLine.append(miniMessage.deserialize("  <dark_gray>│</dark_gray>  "));
            }
            warpLine = warpLine.append(warp);
            count++;
        }
        
        if (count > 0) {
            player.sendMessage(warpLine);
        }

        mm.sendRaw(player, "");
        mm.sendRaw(player, div);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(warpManager.getWarpNames());
        }
        return Collections.emptyList();
    }
}
