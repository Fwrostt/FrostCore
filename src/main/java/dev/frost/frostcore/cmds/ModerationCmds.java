package dev.frost.frostcore.cmds;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.PunishmentManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModerationCmds implements CommandExecutor, TabCompleter {

    private final PunishmentManager pm = PunishmentManager.getInstance();
    private final MessageManager mm = Main.getMessageManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String cmd = label.toLowerCase();

        switch (cmd) {
            case "mute" -> handleMute(sender, args);
            case "unmute" -> handleUnmute(sender, args);
            case "lockchat" -> handleLockChat(sender, true);
            case "unlockchat" -> handleLockChat(sender, false);
            case "freeze" -> handleFreeze(sender, args);
        }

        return true;
    }

    private void handleMute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostcore.moderation.mute")) {
            mm.send(sender, "general.no-permission");
            return;
        }

        if (args.length < 1) {
            mm.sendRaw(sender, "<#FF5555>Usage: /mute <player> [time]");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            mm.send(sender, "admin.player-not-found");
            return;
        }

        long duration = -1;
        String timeStr = "Permanent";
        if (args.length > 1) {
            duration = pm.parseTime(args[1]);
            if (duration != -1) timeStr = args[1];
        }

        pm.mute(target.getUniqueId(), duration);
        mm.send(sender, "moderation.mute-success", Map.of("player", target.getName(), "time", timeStr));
        mm.send(target, "moderation.muted-alert", Map.of("time", timeStr));
    }

    private void handleUnmute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostcore.moderation.mute")) {
            mm.send(sender, "general.no-permission");
            return;
        }

        if (args.length < 1) {
            mm.sendRaw(sender, "<#FF5555>Usage: /unmute <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            mm.send(sender, "admin.player-not-found");
            return;
        }

        pm.unmute(target.getUniqueId());
        mm.send(sender, "moderation.unmute-success", Map.of("player", target.getName()));
        mm.send(target, "moderation.unmuted-alert");
    }

    private void handleLockChat(CommandSender sender, boolean locked) {
        if (!sender.hasPermission("frostcore.moderation.lockchat")) {
            mm.send(sender, "general.no-permission");
            return;
        }

        pm.setChatLocked(locked);
        if (locked) {
            mm.broadcast("moderation.chat-locked-broadcast");
        } else {
            mm.broadcast("moderation.chat-unlocked-broadcast");
        }
    }

    private void handleFreeze(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostcore.moderation.freeze")) {
            mm.send(sender, "general.no-permission");
            return;
        }

        if (args.length < 1) {
            mm.sendRaw(sender, "<#FF5555>Usage: /freeze <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            mm.send(sender, "admin.player-not-found");
            return;
        }

        pm.toggleFreeze(target.getUniqueId());
        boolean frozen = pm.isFrozen(target.getUniqueId());

        if (frozen) {
            mm.send(sender, "moderation.freeze-success", Map.of("player", target.getName()));
            mm.send(target, "moderation.frozen-alert-long");
        } else {
            mm.send(sender, "moderation.unfreeze-success", Map.of("player", target.getName()));
            mm.send(target, "moderation.unfrozen-alert");
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String cmd = label.toLowerCase();
        if (args.length == 1 && (cmd.equals("mute") || cmd.equals("unmute") || cmd.equals("freeze"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && cmd.equals("mute")) {
            return List.of("10m", "1h", "1d", "7d");
        }
        return Collections.emptyList();
    }
}
