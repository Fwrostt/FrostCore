package dev.frost.frostcore.cmds.moderation;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.moderation.ModerationManager;
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


public class ModerationCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length < 2) { mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>/moderation allow <add|remove|check> <player>"); return true; }

        if (args[0].equalsIgnoreCase("allow")) {
            if (args.length < 3) { mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>/moderation allow <add|remove|check> <player>"); return true; }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
            ModerationManager mod = ModerationManager.getInstance();
            UUID staffUuid = (sender instanceof Player p) ? p.getUniqueId() : null;

            switch (args[1].toLowerCase()) {
                case "add" -> {
                    mod.addAllowed(target.getUniqueId(), staffUuid);
                    mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#7ECFA0>Added <white>" + target.getName() + " <#7ECFA0>to the allowed list. They will bypass IP bans.");
                }
                case "remove" -> {
                    mod.removeAllowed(target.getUniqueId());
                    mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>Removed <white>" + target.getName() + " <#D4727A>from the allowed list.");
                }
                case "check" -> {
                    boolean allowed = mod.isAllowed(target.getUniqueId());
                    String status = allowed ? "<#7ECFA0>Allowed" : "<#D4727A>Not allowed";
                    mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>" + target.getName() + " " + status);
                }
                default -> mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>Unknown sub-command. Use: add, remove, check");
            }
        } else if (args[0].equalsIgnoreCase("unlink")) {
            if (args.length < 2) { mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>/moderation unlink <player>"); return true; }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
                ModerationManager.getInstance().getDatabase().unlinkIps(target.getUniqueId());
                mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4A76A>Unlinked all IP addresses for <white>" + target.getName() + "<#D4A76A>.");
            });
        } else if (args[0].equalsIgnoreCase("broadcast")) {
            if (args.length < 2) { mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>/moderation broadcast <message>"); return true; }
            String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            String formatted = "<#D4727A>MOD <dark_gray>»</dark_gray> <white>" + msg;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("frostcore.moderation.notify")) {
                    mm.sendRaw(p, formatted);
                }
            }
            mm.sendRaw(Bukkit.getConsoleSender(), formatted);
        } else {
            mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>Unknown sub-command. Use: <white>/moderation <allow|unlink|broadcast>");
        }
        return true;
    }
    @Override public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return List.of("allow", "unlink", "broadcast").stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2 && args[0].equalsIgnoreCase("allow")) return List.of("add", "remove", "check").stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2 && args[0].equalsIgnoreCase("unlink")) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        if (args.length == 3 && args[0].equalsIgnoreCase("allow")) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
