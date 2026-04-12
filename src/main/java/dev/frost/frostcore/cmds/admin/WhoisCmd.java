package dev.frost.frostcore.cmds.admin;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.moderation.ModerationManager;
import dev.frost.frostcore.manager.VanishManager;
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

public class WhoisCmd implements CommandExecutor, TabCompleter {
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
            mm.sendRaw(sender, "<#FF5555>Usage: /whois <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target != null) {
            sendOnlineWhois(sender, target);
        } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(args[0]);
            if (offline.hasPlayedBefore()) {
                sendOfflineWhois(sender, offline);
            } else {
                mm.send(sender, "admin.player-not-found");
            }
        }
        return true;
    }

    private void sendOnlineWhois(CommandSender sender, Player target) {
        String bar = "<dark_gray><strikethrough>                                                  </strikethrough>";

        sender.sendMessage(mini.deserialize(bar));
        sender.sendMessage(mini.deserialize("  <gradient:#FF5555:#FF55FF><bold>PLAYER INFO</bold></gradient> <dark_gray>» <white>" + target.getName()));
        sender.sendMessage(mini.deserialize(bar));

        sender.sendMessage(mini.deserialize("  <#6BA3E3>⏺ Status:       <#55FF55>Online"));
        if (VanishManager.getInstance().isVanished(target.getUniqueId())) {
            sender.sendMessage(mini.deserialize("  <#6BA3E3>👻 Vanished:     <#FFAA00>Yes"));
        }

        String healthBar = buildBar(target.getHealth(), target.getMaxHealth(), "<#55FF55>", "<#FF5555>");
        sender.sendMessage(mini.deserialize("  <#6BA3E3>❤ Health:       " + healthBar + " <white>" + String.format("%.0f", target.getHealth()) + "/" + String.format("%.0f", target.getMaxHealth())));

        String foodBar = buildBar(target.getFoodLevel(), 20, "<#FFAA00>", "<dark_gray>");
        sender.sendMessage(mini.deserialize("  <#6BA3E3>🍖 Food:         " + foodBar + " <white>" + target.getFoodLevel() + "/20"));

        sender.sendMessage(mini.deserialize("  <#6BA3E3>⚡ XP Level:     <white>" + target.getLevel()));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>🎮 Gamemode:     <white>" + capitalize(target.getGameMode().name())));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>✈ Flying:       <white>" + (target.isFlying() ? "<#55FF55>Yes" : "<#FF5555>No")));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>🛡 God Mode:     <white>" + (target.isInvulnerable() ? "<#55FF55>Yes" : "<#FF5555>No")));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>⭐ Op:           <white>" + (target.isOp() ? "<#55FF55>Yes" : "<#FF5555>No")));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>🌍 World:        <white>" + target.getWorld().getName()));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>📍 Location:     <white>" + target.getLocation().getBlockX() + ", " + target.getLocation().getBlockY() + ", " + target.getLocation().getBlockZ()));

        String ip = target.getAddress() != null ? target.getAddress().getHostString() : "Unknown";
        sender.sendMessage(mini.deserialize("  <#6BA3E3>🌐 IP:           <white>" + ip));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>📶 Ping:         <white>" + target.getPing() + "ms"));

        try {
            long playtimeTicks = target.getStatistic(Statistic.PLAY_ONE_MINUTE);
            sender.sendMessage(mini.deserialize("  <#6BA3E3>⏰ Playtime:     <white>" + formatPlaytime(playtimeTicks / 20)));
        } catch (Exception ignored) {}

        sender.sendMessage(mini.deserialize("  <#6BA3E3>📅 First Join:   <white>" + DATE_FORMAT.format(new Date(target.getFirstPlayed()))));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>🔑 UUID:         <dark_gray>" + target.getUniqueId()));
        sender.sendMessage(mini.deserialize(bar));
    }

    private void sendOfflineWhois(CommandSender sender, OfflinePlayer target) {
        String bar = "<dark_gray><strikethrough>                                                  </strikethrough>";
        String name = target.getName() != null ? target.getName() : "Unknown";

        sender.sendMessage(mini.deserialize(bar));
        sender.sendMessage(mini.deserialize("  <gradient:#FF5555:#FF55FF><bold>PLAYER INFO</bold></gradient> <dark_gray>» <white>" + name));
        sender.sendMessage(mini.deserialize(bar));

        sender.sendMessage(mini.deserialize("  <#6BA3E3>⏺ Status:       <#FF5555>Offline"));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>⭐ Op:           <white>" + (target.isOp() ? "<#55FF55>Yes" : "<#FF5555>No")));

        ModerationManager mod = ModerationManager.getInstance();
        if (mod != null && mod.isBanned(target.getUniqueId())) {
            sender.sendMessage(mini.deserialize("  <#6BA3E3>🔨 Banned:       <#FF5555>Yes"));
        }
        if (mod != null && mod.isMuted(target.getUniqueId())) {
            sender.sendMessage(mini.deserialize("  <#6BA3E3>🔇 Muted:        <#FFAA00>Yes"));
        }

        sender.sendMessage(mini.deserialize("  <#6BA3E3>📅 First Join:   <white>" + DATE_FORMAT.format(new Date(target.getFirstPlayed()))));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>📅 Last Seen:    <white>" + DATE_FORMAT.format(new Date(target.getLastLogin()))));

        long timeSince = System.currentTimeMillis() - target.getLastLogin();
        sender.sendMessage(mini.deserialize("  <#6BA3E3>⏱ Ago:           <white>" + formatPlaytime(timeSince / 1000)));

        try {
            long playtimeTicks = target.getStatistic(Statistic.PLAY_ONE_MINUTE);
            sender.sendMessage(mini.deserialize("  <#6BA3E3>⏰ Playtime:     <white>" + formatPlaytime(playtimeTicks / 20)));
        } catch (Exception ignored) {}

        sender.sendMessage(mini.deserialize("  <#6BA3E3>🔑 UUID:         <dark_gray>" + target.getUniqueId()));
        sender.sendMessage(mini.deserialize(bar));
    }

    private String buildBar(double current, double max, String filledColor, String emptyColor) {
        int barLength = 10;
        int filled = (int) Math.round((current / max) * barLength);
        int empty = barLength - filled;
        return filledColor + "█".repeat(Math.max(0, filled)) + emptyColor + "█".repeat(Math.max(0, empty));
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
