package dev.frost.frostcore.cmds.moderation;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.moderation.*;
import dev.frost.frostcore.gui.impls.HistoryGui;
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

public class HistoryCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) { mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>/history <player> [type]"); return true; }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        String typeFilter = args.length >= 2 ? args[1].toUpperCase() : null;

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            ModerationDatabase modDb = ModerationManager.getInstance().getDatabase();
            List<Punishment> history = modDb.getPlayerHistory(target.getUniqueId(), typeFilter);

            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                if (sender instanceof Player p) {
                    HistoryGui.open(p, target, history, false);
                } else {
                    mm.sendRaw(sender, "");
                    mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>History for <white>" + target.getName() + " <dark_gray>(" + history.size() + " records)");
                    mm.sendRaw(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                    if (history.isEmpty()) {
                        mm.sendRaw(sender, "  <#707880>No punishment history found.");
                    } else {
                        int shown = Math.min(history.size(), 15);
                        for (int i = 0; i < shown; i++) {
                            Punishment pRecord = history.get(i);
                            String status = pRecord.active() ? (pRecord.isExpired() ? "<#D4A76A>Expired" : "<#D4727A>Active") : "<#7ECFA0>Inactive";
                            String typeColor = switch (pRecord.type().getCategory()) {
                                case "BAN"  -> "<#D4727A>";
                                case "MUTE" -> "<#D4A76A>";
                                case "WARN" -> "<#C8A87C>";
                                case "KICK" -> "<#A35560>";
                                default     -> "<#8FA3BF>";
                            };
                            mm.sendRaw(sender, "  " + typeColor + pRecord.type().getDisplayName() + " <dark_gray>#" + pRecord.randomId()
                                    + " <dark_gray>| " + status + " <dark_gray>| <#8FA3BF>" + pRecord.getStaffDisplayName()
                                    + " <dark_gray>| <white>" + pRecord.reason());
                            mm.sendRaw(sender, "    <#707880>" + pRecord.getFormattedDuration() + " — "
                                    + Punishment.formatDuration(System.currentTimeMillis() - pRecord.createdAt()) + " ago");
                        }
                        if (history.size() > shown) {
                            mm.sendRaw(sender, "  <#707880>... and " + (history.size() - shown) + " more.");
                        }
                    }
                    mm.sendRaw(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                }
            });
        });
        return true;
    }
    @Override public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2) return List.of("ban", "mute", "warn", "kick", "jail");
        return Collections.emptyList();
    }
}
