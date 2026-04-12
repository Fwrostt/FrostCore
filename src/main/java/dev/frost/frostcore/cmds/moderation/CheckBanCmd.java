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

/** /checkban <player|ID> */
public class CheckBanCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) { mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>/checkban <player|ID>"); return true; }
        ModerationManager mod = ModerationManager.getInstance();

        // Try ID
        try { int id = Integer.parseInt(args[0]); showPunishment(sender, mod.getDatabase().getPunishmentById(id)); return true; } catch (NumberFormatException ignored) {}
        Punishment byRandom = mod.getDatabase().getPunishmentByRandomId(args[0].toUpperCase());
        if (byRandom != null) { showPunishment(sender, byRandom); return true; }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        Punishment ban = mod.getActiveBan(target.getUniqueId());
        if (ban == null) {
            Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
                Punishment dbBan = mod.getDatabase().getActivePunishment(target.getUniqueId(), "BAN");
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    if (dbBan == null) mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#7ECFA0>" + target.getName() + " is not banned.");
                    else showPunishment(sender, dbBan);
                });
            });
        } else { showPunishment(sender, ban); }
        return true;
    }

    private void showPunishment(CommandSender sender, Punishment p) {
        if (p == null) { mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>Not found."); return; }
        mm.sendRaw(sender, "");
        mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>" + p.type().getDisplayName() + " Info");
        mm.sendRaw(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        mm.sendRaw(sender, "  <#8FA3BF>Player: <white>" + p.getTargetDisplayName());
        mm.sendRaw(sender, "  <#8FA3BF>Reason: <white>" + p.reason());
        mm.sendRaw(sender, "  <#8FA3BF>Staff: <white>" + p.getStaffDisplayName());
        mm.sendRaw(sender, "  <#8FA3BF>Duration: <white>" + p.getFormattedDuration());
        mm.sendRaw(sender, "  <#8FA3BF>Remaining: <white>" + p.getFormattedRemaining());
        String activeStr = p.active() && !p.isExpired() ? "<#D4727A>Active <dark_gray>| <hover:show_text:'<!italic><gray>Click to pardon'><click:run_command:'/unban " + p.randomId() + "'><#7ECFA0>[Pardon]</click></hover>" : "<#7ECFA0>Inactive";
        mm.sendRaw(sender, "  <#8FA3BF>Status: " + activeStr);
        mm.sendRaw(sender, "  <#8FA3BF>ID: <dark_gray>#" + p.id() + " | " + p.randomId());
        if (p.ip() != null) mm.sendRaw(sender, "  <#8FA3BF>IP: <dark_gray>" + p.ip());
        mm.sendRaw(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Override public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList()) : Collections.emptyList();
    }
}
