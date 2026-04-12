package dev.frost.frostcore.cmds.moderation;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.moderation.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.stream.Collectors;


public class IpHistoryCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) { mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>/iphistory <player>"); return true; }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            ModerationDatabase modDb = ModerationManager.getInstance().getDatabase();
            Set<String> ips = modDb.getIpsByUuid(target.getUniqueId());
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                mm.sendRaw(sender, "");
                mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>IP History for <white>" + target.getName());
                mm.sendRaw(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                if (ips.isEmpty()) mm.sendRaw(sender, "  <#707880>No IP records found.");
                else for (String ip : ips) {
                    Set<UUID> shared = modDb.getUuidsByIp(ip);
                    String ipClickable = "<hover:show_text:'<!italic><gray>Click to copy IP'><click:copy_to_clipboard:'" + ip + "'><#8FA3BF>" + ip + "</click></hover>";
                    mm.sendRaw(sender, "  " + ipClickable + " <dark_gray>(" + shared.size() + " account" + (shared.size() != 1 ? "s" : "") + ")");
                }
                mm.sendRaw(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            });
        });
        return true;
    }
    @Override public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList()) : Collections.emptyList();
    }
}
