package dev.frost.frostcore.cmds.messaging;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.PrivateMessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MsgCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = Main.getMessageManager();
    private final PrivateMessageManager pmm = PrivateMessageManager.getInstance();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (args.length < 2) {
            if (sender instanceof Player) {
                mm.sendRaw(sender, "<#D4727A>Usage: /msg <player> <message>");
            } else {
                sender.sendMessage("Usage: /msg <player> <message>");
            }
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            if (sender instanceof Player) {
                mm.send(sender, "admin.player-not-found");
            } else {
                sender.sendMessage("Player not found.");
            }
            return true;
        }

        if (sender instanceof Player player && target.equals(player)) {
            mm.sendRaw(player, "<#D4727A>You can't message yourself!");
            return true;
        }

        if (sender instanceof Player player && pmm.isIgnoring(target.getUniqueId(), player.getUniqueId())) {
            mm.send(player, "message.ignored");
            return true;
        }

        String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if (sender instanceof Player player) {
            sendPrivateMessage(player, target, msg);
        } else {
            String consoleName = "Console";
            mm.send(target, "message.received", Map.of("player", consoleName, "message", msg));
            target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.8f);
            sender.sendMessage("[" + consoleName + " -> " + target.getName() + "] " + msg);

            for (java.util.UUID spyUUID : pmm.getSocialSpies()) {
                if (spyUUID.equals(target.getUniqueId())) continue;
                Player spy = Bukkit.getPlayer(spyUUID);
                if (spy != null && spy.isOnline()) {
                    mm.sendRaw(spy, "<dark_gray>[<gradient:#FF5555:#FF55FF>SPY</gradient>] <#6BA3E3>" + consoleName + " <dark_gray>→ <#6BA3E3>" + target.getName() + "<dark_gray>: <#B0C4FF>" + msg);
                }
            }
        }
        return true;
    }

    private void sendPrivateMessage(Player sender, Player target, String message) {
        pmm.setReplyTarget(sender.getUniqueId(), target.getUniqueId());
        pmm.setReplyTarget(target.getUniqueId(), sender.getUniqueId());

        mm.send(sender, "message.sent", Map.of("player", target.getName(), "message", message));
        mm.send(target, "message.received", Map.of("player", sender.getName(), "message", message));

        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.8f);

        for (java.util.UUID spyUUID : pmm.getSocialSpies()) {
            if (spyUUID.equals(sender.getUniqueId()) || spyUUID.equals(target.getUniqueId())) continue;
            Player spy = org.bukkit.Bukkit.getPlayer(spyUUID);
            if (spy != null && spy.isOnline()) {
                mm.sendRaw(spy, "<dark_gray>[<gradient:#FF5555:#FF55FF>SPY</gradient>] <#6BA3E3>" + sender.getName() + " <dark_gray>→ <#6BA3E3>" + target.getName() + "<dark_gray>: <#B0C4FF>" + message);
            }
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
