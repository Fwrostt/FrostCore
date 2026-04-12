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

public class WarnCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        ModerationManager mod = ModerationManager.getInstance();
        if (args.length < 1) { mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>/warn <player> [reason] [-s] [-t template]"); return true; }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>Player must be online."); return true; }
        if (!Main.getGroupLimitManager().canPunish(sender, target)) { mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>You cannot punish this player."); return true; }

        ParsedArgs parsed = ParsedArgs.parseReasonOnly(args, 1);

        if (parsed.template != null) {
            TemplateManager tm = Main.getTemplateManager();
            TemplateManager.PunishmentTemplate template = tm.getTemplate(parsed.template);
            if (template == null) { mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>Template not found: <white>" + parsed.template); return true; }
            mod.punish(PunishmentType.WARN, target.getUniqueId(), target.getName(), null, template.reason(), sender, -1, parsed.silent);
            mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#7ECFA0>Warned <white>" + target.getName() + " <#8FA3BF>using template <white>" + parsed.template);
            return true;
        }

        String reason = parsed.reason.isEmpty() ? "Warned by an administrator" : parsed.reason;
        mod.punish(PunishmentType.WARN, target.getUniqueId(), target.getName(), null, reason, sender, -1, parsed.silent);
        mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#7ECFA0>Warned <white>" + target.getName() + ".");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length >= 2 && args[args.length - 2].equals("-t")) return new ArrayList<>(Main.getTemplateManager().getTemplateNames());
        return List.of("-s", "-t");
    }
}
