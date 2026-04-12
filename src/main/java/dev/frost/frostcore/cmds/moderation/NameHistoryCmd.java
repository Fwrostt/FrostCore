package dev.frost.frostcore.cmds.moderation;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.moderation.ModerationManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class NameHistoryCmd implements CommandExecutor {
    private final MessageManager mm = Main.getMessageManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("frostcore.moderation.namehistory")) {
            mm.sendRaw(sender, "<!italic><#D4727A>No permission.");
            return true;
        }

        if (args.length < 1) {
            mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>/namehistory <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            Map<String, long[]> names = ModerationManager.getInstance().getDatabase().getNameHistory(target.getUniqueId());

            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                mm.sendRaw(sender, "");
                mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>Name history for " + target.getName());
                mm.sendRaw(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                if (names.isEmpty()) {
                    mm.sendRaw(sender, "  <#707880>No name history found.");
                } else {
                    for (Map.Entry<String, long[]> entry : names.entrySet()) {
                        String name = entry.getKey();
                        long[] dates = entry.getValue();
                        long firstSeen = dates[0];
                        long lastSeen = dates[1];
                        
                        String nameClickable = "<hover:show_text:'<!italic><gray>Click to copy name'><click:copy_to_clipboard:'" + name + "'><white>" + name + "</click></hover>";
                        mm.sendRaw(sender, "  <#D4727A>▪ " + nameClickable);
                        mm.sendRaw(sender, "    <dark_gray>First Seen: <#8FA3BF>" + new java.util.Date(firstSeen).toString());
                        mm.sendRaw(sender, "    <dark_gray>Last Seen: <#8FA3BF>" + new java.util.Date(lastSeen).toString());
                    }
                }
                mm.sendRaw(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            });
        });

        return true;
    }
}
