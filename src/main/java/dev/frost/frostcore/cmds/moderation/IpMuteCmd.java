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

public class IpMuteCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        ModerationManager mod = ModerationManager.getInstance();
        if (args.length < 1) { mm.sendRaw(sender, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#8FA3BF>Usage: <white>/ipmute <player> [duration] [reason] [-s]"); return true; }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { mm.sendRaw(sender, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#D4727A>Player must be online for IP mute."); return true; }
        if (!Main.getGroupLimitManager().canPunish(sender, target)) { mm.sendRaw(sender, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#D4727A>You cannot punish this player."); return true; }
        String ip = target.getAddress() != null ? target.getAddress().getAddress().getHostAddress() : null;
        if (ip == null) { mm.sendRaw(sender, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#D4727A>Could not resolve player's IP."); return true; }

        ParsedArgs parsed = ParsedArgs.parse(args, 1);
        if (Main.getGroupLimitManager().exceedsMaxDuration(sender, "IPMUTE", parsed.duration)) { mm.sendRaw(sender, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#D4727A>Duration exceeds your group's maximum."); return true; }
        String reason = parsed.reason.isEmpty() ? "IP Muted by an administrator" : parsed.reason;
        mod.punish(PunishmentType.IPMUTE, target.getUniqueId(), target.getName(), ip, reason, sender, parsed.duration, parsed.silent);

        String durationStr = parsed.duration == -1 ? "permanently" : "for " + Punishment.formatDuration(parsed.duration);
        mm.sendRaw(sender, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#7ECFA0>IP Muted <white>" + target.getName() + " <#8FA3BF>" + durationStr);
        return true;
    }
    @Override public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2) return List.of("1h", "1d", "7d", "30d", "permanent");
        return List.of("-s");
    }
}
