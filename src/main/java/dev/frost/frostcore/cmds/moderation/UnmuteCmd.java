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

public class UnmuteCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        ModerationManager mod = ModerationManager.getInstance();
        if (args.length < 1) { mm.sendRaw(sender, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#8FA3BF>Usage: <white>/unmute <player|ID>"); return true; }

        // Try by ID
        try {
            int id = Integer.parseInt(args[0]);
            if (mod.removePunishment(id, sender, "Unmuted", false)) {
                mm.sendRaw(sender, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#7ECFA0>Punishment #" + id + " has been removed.");
            } else {
                mm.sendRaw(sender, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#D4727A>Punishment #" + id + " not found.");
            }
            return true;
        } catch (NumberFormatException ignored) {}

        // Try random ID
        Punishment byRandom = mod.getDatabase().getPunishmentByRandomId(args[0].toUpperCase());
        if (byRandom != null) { mod.removePunishment(byRandom.id(), sender, "Unmuted", false); mm.sendRaw(sender, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#7ECFA0>Unmuted <white>" + byRandom.getTargetDisplayName() + "."); return true; }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (mod.removePunishmentByPlayer(target.getUniqueId(), "MUTE", sender, "Unmuted")) {
            mm.sendRaw(sender, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#7ECFA0>Unmuted <white>" + target.getName() + ".");
        } else {
            mm.sendRaw(sender, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#D4727A>No active mute found for <white>" + args[0] + ".");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList()) : Collections.emptyList();
    }
}
