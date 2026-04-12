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

public class JailCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        ModerationManager mod = ModerationManager.getInstance();
        if (args.length < 1) { mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>/jail <player> [jail] [duration] [reason]"); return true; }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>Player must be online."); return true; }
        if (!Main.getGroupLimitManager().canPunish(sender, target)) { mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>You cannot punish this player."); return true; }
        if (mod.isJailed(target.getUniqueId())) { mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>This player is already jailed."); return true; }

        // Determine jail name
        String jailName = "default";
        if (args.length >= 2 && mod.getJailLocation(args[1]) != null) {
            jailName = args[1].toLowerCase();
        } else if (mod.getJailLocations().isEmpty()) {
            mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>No jail locations set. Use <white>/setjail <name>");
            return true;
        } else {
            jailName = mod.getJailLocations().keySet().iterator().next();
        }

        JailLocation jail = mod.getJailLocation(jailName);
        if (jail == null) { mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>Jail not found: <white>" + jailName); return true; }

        // Parse duration and reason from remaining args
        long duration = -1;
        StringBuilder reason = new StringBuilder();
        int startIdx = (args.length >= 2 && mod.getJailLocation(args[1]) != null) ? 2 : 1;
        for (int i = startIdx; i < args.length; i++) {
            long parsed = Punishment.parseTime(args[i]);
            if (parsed != -2 && duration == -1) { duration = parsed; continue; }
            if (!reason.isEmpty()) reason.append(" ");
            reason.append(args[i]);
        }

        UUID staffUuid = (sender instanceof Player p) ? p.getUniqueId() : null;
        long expiresAt = duration == -1 ? -1 : System.currentTimeMillis() + duration;
        String reasonStr = reason.isEmpty() ? "Jailed by an administrator" : reason.toString();

        mod.jailPlayer(target.getUniqueId(), jailName, expiresAt, reasonStr, staffUuid);
        mod.punish(PunishmentType.JAIL, target.getUniqueId(), target.getName(), null, reasonStr, sender, duration, false);

        // Teleport to jail
        var loc = jail.toBukkitLocation();
        if (loc != null) target.teleport(loc);

        String durationStr = duration == -1 ? "permanently" : "for " + Punishment.formatDuration(duration);
        mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#7ECFA0>Jailed <white>" + target.getName() + " <#8FA3BF>" + durationStr + " <dark_gray>(Jail: " + jailName + ")");
        return true;
    }
    @Override public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2) return new ArrayList<>(ModerationManager.getInstance().getJailLocations().keySet());
        if (args.length == 3) return List.of("1h", "1d", "7d", "permanent");
        return Collections.emptyList();
    }
}
