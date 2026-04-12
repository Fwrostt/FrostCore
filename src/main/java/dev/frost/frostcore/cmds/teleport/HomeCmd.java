package dev.frost.frostcore.cmds.teleport;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.manager.MessageManager;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HomeCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = MessageManager.get();
    private final TeleportUtil teleportUtil = Main.getTeleportUtil();
    private final ConfigManager config = Main.getConfigManager();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("frostcore.home")) {
            mm.sendRaw(player, "<red>You don't have permission to use this command.");
            return true;
        }

        if (!config.getBoolean("homes.enabled", true)) {
            mm.send(player, "homes.disabled");
            return true;
        }

        Map<String, Location> homes = Main.getHomeManager().getHomes(player);

        if (args.length < 1) {

            if (homes.size() == 1) {
                String singleHome = homes.keySet().iterator().next();
                teleportToHome(player, singleHome, homes.get(singleHome));
                return true;
            }
            showHomeList(player, homes.keySet());
            return true;
        }

        String homeName = args[0].toLowerCase();
        Location loc = homes.get(homeName);

        if (loc == null) {
            mm.send(player, "homes.not-found", Map.of("home", homeName));
            return true;
        }

        teleportToHome(player, homeName, loc);
        return true;
    }

    private void teleportToHome(Player player, String homeName, Location loc) {
        teleportUtil.teleportWithCooldownAndDelay(
                player, loc,
                "home",
                "homes.cooldown",
                "homes.cooldown",
                config.getInt("homes.delay", 3),
                "homes.wait",
                "homes.teleport",
                "homes.teleport-cancelled",
                Map.of("home", homeName, "time", String.valueOf(config.getInt("homes.delay", 3)))
        );
    }

    private void showHomeList(Player player, Set<String> homeNames) {
        if (homeNames.isEmpty()) {
            mm.send(player, "homes.no-homes");
            return;
        }

        String div = "<gradient:#FF99AA:#CC77FF><strikethrough>                                                          </strikethrough></gradient>";

        mm.sendRaw(player, "");
        mm.sendRaw(player, div);
        mm.sendRaw(player, " <gradient:#FF99AA:#CC77FF><bold>Personal Homes</bold></gradient>");
        mm.sendRaw(player, "");

        Component homeLine = Component.empty().append(miniMessage.deserialize(" "));
        int count = 0;
        for (String name : homeNames) {
            Component home = miniMessage.deserialize("<#B0C4FF>• <white>" + name + "</white>")
                    .clickEvent(ClickEvent.runCommand("/home " + name))
                    .hoverEvent(HoverEvent.showText(miniMessage.deserialize("<#A3FFA3>Click to teleport to <white>" + name + "</white>")));

            if (count > 0 && count % 3 == 0) {
                player.sendMessage(homeLine);
                homeLine = Component.empty().append(miniMessage.deserialize(" "));
            } else if (count > 0) {
                homeLine = homeLine.append(miniMessage.deserialize("  <dark_gray>│</dark_gray>  "));
            }
            homeLine = homeLine.append(home);
            count++;
        }

        if (count > 0) {
            player.sendMessage(homeLine);
        }

        mm.sendRaw(player, "");
        mm.sendRaw(player, div);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player p && args.length == 1) {
            return new ArrayList<>(Main.getHomeManager().getHomes(p).keySet());
        }
        return Collections.emptyList();
    }
}

