package dev.frost.frostcore.cmds.admin;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class SeenCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();
    private final MiniMessage mini = MiniMessage.miniMessage();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("frostcore.admin.whois")) {
            mm.send(sender, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            mm.sendRaw(sender, "<#FF5555>Usage: /seen <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target != null) {
            sendOnlineSeen(sender, target);
        } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(args[0]);
            if (offline.hasPlayedBefore()) {
                sendOfflineSeen(sender, offline);
            } else {
                mm.send(sender, "admin.player-not-found");
            }
        }
        return true;
    }

    private void sendOnlineSeen(CommandSender sender, Player target) {
        String bar = "<dark_gray><strikethrough>                                        </strikethrough>";

        sender.sendMessage(mini.deserialize(bar));
        sender.sendMessage(mini.deserialize("  <gradient:#55FF55:#00C9FF><bold>ONLINE</bold></gradient> <dark_gray>» <white>" + target.getName()));
        sender.sendMessage(mini.deserialize(bar));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>⏺ Status:     <#55FF55>Currently Online"));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>🌍 World:      <white>" + target.getWorld().getName()));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>📍 Location:   <white>" + target.getLocation().getBlockX() + ", " + target.getLocation().getBlockY() + ", " + target.getLocation().getBlockZ()));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>🎮 Gamemode:   <white>" + capitalize(target.getGameMode().name())));

        try {
            long playtimeTicks = target.getStatistic(Statistic.PLAY_ONE_MINUTE);
            sender.sendMessage(mini.deserialize("  <#6BA3E3>⏰ Playtime:   <white>" + formatPlaytime(playtimeTicks / 20)));
        } catch (Exception ignored) {}

        sender.sendMessage(mini.deserialize("  <#6BA3E3>📅 First Join: <white>" + DATE_FORMAT.format(new Date(target.getFirstPlayed()))));
        sender.sendMessage(mini.deserialize(bar));
    }

    private void sendOfflineSeen(CommandSender sender, OfflinePlayer target) {
        String bar = "<dark_gray><strikethrough>                                        </strikethrough>";
        String name = target.getName() != null ? target.getName() : "Unknown";

        long timeSince = System.currentTimeMillis() - target.getLastLogin();

        sender.sendMessage(mini.deserialize(bar));
        sender.sendMessage(mini.deserialize("  <gradient:#FF5555:#FF55FF><bold>OFFLINE</bold></gradient> <dark_gray>» <white>" + name));
        sender.sendMessage(mini.deserialize(bar));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>⏺ Status:     <#FF5555>Offline"));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>📅 Last Seen:  <white>" + DATE_FORMAT.format(new Date(target.getLastLogin()))));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>⏱ Time Ago:   <white>" + formatPlaytime(timeSince / 1000)));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>📅 First Join: <white>" + DATE_FORMAT.format(new Date(target.getFirstPlayed()))));

        try {
            long playtimeTicks = target.getStatistic(Statistic.PLAY_ONE_MINUTE);
            sender.sendMessage(mini.deserialize("  <#6BA3E3>⏰ Playtime:   <white>" + formatPlaytime(playtimeTicks / 20)));
        } catch (Exception ignored) {}

        sender.sendMessage(mini.deserialize(bar));
    }

    private String formatPlaytime(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        if (days > 0) return days + "d " + hours + "h " + minutes + "m";
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    private String capitalize(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
