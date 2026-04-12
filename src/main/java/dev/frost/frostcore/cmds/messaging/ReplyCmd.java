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

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ReplyCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = Main.getMessageManager();
    private final PrivateMessageManager pmm = PrivateMessageManager.getInstance();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length < 1) {
            mm.sendRaw(player, "<#FF5555>Usage: /r <message>");
            return true;
        }

        java.util.UUID replyTo = pmm.getReplyTarget(player.getUniqueId());
        if (replyTo == null) {
            mm.send(player, "message.no-reply");
            return true;
        }

        Player target = Bukkit.getPlayer(replyTo);
        if (target == null || !target.isOnline()) {
            mm.send(player, "message.reply-offline");
            return true;
        }

        if (pmm.isIgnoring(target.getUniqueId(), player.getUniqueId())) {
            mm.send(player, "message.ignored");
            return true;
        }

        String msg = String.join(" ", args);
        sendPrivateMessage(player, target, msg);
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
        return Collections.emptyList();
    }
}
