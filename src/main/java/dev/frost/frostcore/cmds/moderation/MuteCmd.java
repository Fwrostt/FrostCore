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

public class MuteCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        ModerationManager mod = ModerationManager.getInstance();
        if (args.length < 1) {
            mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>/mute <player> [duration] [reason] [-s] [-t template]");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>Player not found.");
            return true;
        }
        if (target.isOnline() && !Main.getGroupLimitManager().canPunish(sender, target.getPlayer())) {
            mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>You cannot punish this player.");
            return true;
        }
        if (mod.isMuted(target.getUniqueId())) {
            mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>This player is already muted.");
            return true;
        }

        ParsedArgs parsed = ParsedArgs.parse(args, 1);

        if (parsed.template != null) {
            TemplateManager tm = Main.getTemplateManager();
            TemplateManager.PunishmentTemplate template = tm.getTemplate(parsed.template);
            if (template == null) {
                mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>Template not found: <white>" + parsed.template);
                return true;
            }
            int offenseCount = mod.getDatabase().countPlayerWarnings(target.getUniqueId()) + 1;
            long duration = tm.resolveDuration(template, offenseCount);
            PunishmentType type = duration == -1 ? PunishmentType.MUTE : PunishmentType.TEMPMUTE;
            mod.punish(type, target.getUniqueId(), target.getName(), null, template.reason(), sender, duration, parsed.silent);
            mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#7ECFA0>Muted <white>" + target.getName() + " <#8FA3BF>using template <white>" + parsed.template);
            return true;
        }

        GroupLimitManager glm = Main.getGroupLimitManager();
        if (glm.requiresTemplate(sender)) {
            mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>Your group requires a template. Use <white>-t <template>");
            return true;
        }
        if (glm.exceedsMaxDuration(sender, "MUTE", parsed.duration)) {
            mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>Duration exceeds your group's maximum.");
            return true;
        }

        PunishmentType type = (parsed.duration == -1) ? PunishmentType.MUTE : PunishmentType.TEMPMUTE;
        String reason = parsed.reason.isEmpty() ? "Muted by an administrator" : parsed.reason;
        mod.punish(type, target.getUniqueId(), target.getName(), null, reason, sender, parsed.duration, parsed.silent);

        String durationStr = parsed.duration == -1 ? "permanently" : "for " + Punishment.formatDuration(parsed.duration);
        mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#7ECFA0>Muted <white>" + target.getName() + " <#8FA3BF>" + durationStr + ".");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2) return List.of("1h", "1d", "7d", "30d", "permanent", "-t");
        if (args.length >= 2 && args[args.length - 2].equals("-t")) return new ArrayList<>(Main.getTemplateManager().getTemplateNames());
        return List.of("-s", "-t");
    }
}
