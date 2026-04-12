package dev.frost.frostcore.cmds.moderation;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.moderation.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class ReportCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Only players can report."); return true; }
        if (args.length < 2) { mm.sendRaw(sender, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#8FA3BF>Usage: <white>/report <player> <reason>"); return true; }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { mm.sendRaw(sender, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#D4727A>Player not found."); return true; }
        if (target.equals(player)) { mm.sendRaw(sender, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#D4727A>You cannot report yourself."); return true; }

        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Report report = new Report(0, player.getUniqueId(), player.getName(), target.getUniqueId(), target.getName(), reason, System.currentTimeMillis(), false, null, null, null);

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            ModerationManager mod = ModerationManager.getInstance();
            int id = mod.getDatabase().insertReport(report);
            Report saved = new Report(id, report.reporterUuid(), report.reporterName(), report.targetUuid(), report.targetName(), report.reason(), report.createdAt(), false, null, null, null);

            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                mm.sendRaw(player, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#7ECFA0>Report submitted. <dark_gray>ID: #" + id);

                // Notify staff
                String msg = "<gradient:#D4727A:#A35560>REPORT</gradient> <dark_gray>» <#8FA3BF>" + player.getName() + " reported <white>" + target.getName() + " <dark_gray>| <#8FA3BF>" + reason + " <dark_gray>[#" + id + "]";
                for (Player staff : Bukkit.getOnlinePlayers()) {
                    if (staff.hasPermission("frostcore.moderation.notify")) {
                        mm.sendRaw(staff, msg);
                    }
                }
            });

            // Webhook
            if (mod.getWebhookManager() != null) {
                mod.getWebhookManager().sendReportWebhookAsync(saved);
            }
        });
        return true;
    }
    @Override public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
