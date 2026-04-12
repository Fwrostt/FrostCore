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


public class StaffRollbackCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) { mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>/staffrollback <staff> [duration]"); return true; }
        OfflinePlayer staff = Bukkit.getOfflinePlayer(args[0]);

        Long sinceMs = null;
        if (args.length >= 2) {
            long parsed = Punishment.parseTime(args[1]);
            if (parsed > 0) sinceMs = System.currentTimeMillis() - parsed;
        }
        final Long since = sinceMs;

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            int count = ModerationManager.getInstance().getDatabase().staffRollback(staff.getUniqueId(), since);
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#7ECFA0>Rolled back <white>" + count + " <#7ECFA0>punishments by <white>" + staff.getName() + ".");
                if (ModerationManager.getInstance().getWebhookManager() != null) {
                    ModerationManager.getInstance().getWebhookManager().sendStaffActivityAsync("Staff Rollback", sender.getName(), "Rolled back " + count + " punishments by " + staff.getName());
                }
            });
        });
        return true;
    }
    @Override public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2) return List.of("1h", "1d", "7d");
        return Collections.emptyList();
    }
}
