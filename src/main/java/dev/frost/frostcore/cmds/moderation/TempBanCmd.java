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


public class TempBanCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = Main.getMessageManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        ModerationManager mod = ModerationManager.getInstance();
        if (args.length < 2) {
            mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>/tempban <player> <duration> [reason] [-s]");
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

        if (mod.isBanned(target.getUniqueId())) {
            mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>This player is already banned.");
            return true;
        }

        ParsedArgs parsed = ParsedArgs.parseRequired(args, 1);
        if (parsed.duration == -2) {
            mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>Invalid duration. Use: 10s, 5m, 1h, 7d, 2w, 1M");
            return true;
        }

        if (Main.getGroupLimitManager().exceedsMaxDuration(sender, "TEMPBAN", parsed.duration)) {
            mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>Duration exceeds your group's maximum.");
            return true;
        }

        String reason = parsed.reason.isEmpty() ? "Temporarily banned" : parsed.reason;
        mod.punish(PunishmentType.TEMPBAN, target.getUniqueId(), target.getName(), null, reason, sender, parsed.duration, parsed.silent);

        mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#7ECFA0>Banned <white>"
                + target.getName() + " <#8FA3BF>for " + Punishment.formatDuration(parsed.duration) + ".");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2) return List.of("1h", "1d", "7d", "30d");
        return List.of("-s");
    }
}
