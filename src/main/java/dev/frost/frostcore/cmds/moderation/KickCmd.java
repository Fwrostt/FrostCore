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

public class KickCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) { mm.sendRaw(sender, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#8FA3BF>Usage: <white>/kick <player> [reason] [-s]"); return true; }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { mm.sendRaw(sender, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#D4727A>Player not found or offline."); return true; }
        if (!Main.getGroupLimitManager().canPunish(sender, target)) { mm.sendRaw(sender, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#D4727A>You cannot punish this player."); return true; }

        ParsedArgs parsed = ParsedArgs.parse(args, 1);
        String reason = parsed.reason.isEmpty() ? "Kicked by an administrator" : parsed.reason;
        ModerationManager.getInstance().punish(PunishmentType.KICK, target.getUniqueId(), target.getName(), null, reason, sender, 0, parsed.silent);
        mm.sendRaw(sender, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#7ECFA0>Kicked <white>" + target.getName() + ".");
        return true;
    }
    @Override public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        return List.of("-s");
    }
}
