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

/** /staffhistory <staff> [type] */
public class StaffHistoryCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) { mm.sendRaw(sender, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#8FA3BF>Usage: <white>/staffhistory <staff> [type]"); return true; }
        OfflinePlayer staff = Bukkit.getOfflinePlayer(args[0]);
        String typeFilter = args.length >= 2 ? args[1].toUpperCase() : null;

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            List<Punishment> history = ModerationManager.getInstance().getDatabase().getStaffHistory(staff.getUniqueId(), typeFilter);
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                if (sender instanceof Player p) {
                    HistoryGui.open(p, staff, history, true);
                } else {
                    mm.sendRaw(sender, "");
                    mm.sendRaw(sender, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#8FA3BF>Staff history for <white>" + staff.getName() + " <dark_gray>(" + history.size() + " punishments)");
                    mm.sendRaw(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    int shown = Math.min(history.size(), 15);
                    for (int i = 0; i < shown; i++) {
                        Punishment pRecord = history.get(i);
                        String typeColor = switch (pRecord.type().getCategory()) { case "BAN" -> "<#D4727A>"; case "MUTE" -> "<#D4A76A>"; case "WARN" -> "<#C8A87C>"; default -> "<#8FA3BF>"; };
                        mm.sendRaw(sender, "  " + typeColor + pRecord.type().getDisplayName() + " <dark_gray>#" + pRecord.randomId()
                                + " <dark_gray>→ <white>" + pRecord.getTargetDisplayName() + " <dark_gray>| <#8FA3BF>" + pRecord.reason());
                    }
                    if (history.size() > shown) mm.sendRaw(sender, "  <#707880>... and " + (history.size() - shown) + " more.");
                    mm.sendRaw(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                }
            });
        });
        return true;
    }
    @Override public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2) return List.of("ban", "mute", "warn", "kick");
        return Collections.emptyList();
    }
}
