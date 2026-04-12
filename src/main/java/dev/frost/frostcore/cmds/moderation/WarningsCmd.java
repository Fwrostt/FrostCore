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


public class WarningsCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        UUID targetUuid;
        String targetName;
        if (args.length >= 1) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]); targetUuid = target.getUniqueId(); targetName = target.getName();
        } else if (sender instanceof Player p) {
            targetUuid = p.getUniqueId(); targetName = p.getName();
        } else { sender.sendMessage("/warnings <player>"); return true; }

        final String name = targetName;
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            List<Punishment> warnings = ModerationManager.getInstance().getDatabase().getActiveWarnings(targetUuid);
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                mm.sendRaw(sender, "");
                mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>Warnings for <white>" + name + " <dark_gray>(" + warnings.size() + ")");
                mm.sendRaw(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                if (warnings.isEmpty()) mm.sendRaw(sender, "  <#707880>No active warnings.");
                else for (Punishment w : warnings) {
                    mm.sendRaw(sender, "  <#C8A87C>#" + w.randomId() + " <dark_gray>| <#8FA3BF>" + w.getStaffDisplayName() + " <dark_gray>| <white>" + w.reason()
                            + " <dark_gray>| <#707880>" + Punishment.formatDuration(System.currentTimeMillis() - w.createdAt()) + " ago");
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
