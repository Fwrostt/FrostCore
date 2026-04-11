package dev.frost.frostcore.cmds;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.PrivateMessageManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
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

public class MessageCmds implements CommandExecutor, TabCompleter {

    private final MessageManager mm = Main.getMessageManager();
    private final PrivateMessageManager pmm = PrivateMessageManager.getInstance();
    private final MiniMessage mini = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String cmd = label.toLowerCase();

        switch (cmd) {
            case "msg", "tell", "w", "whisper", "pm" -> handleMsg(sender, args);
            case "r", "reply" -> handleReply(sender, args);
            case "ignore" -> handleIgnore(sender, args);
        }

        return true;
    }

    private void handleMsg(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return;
        }

        if (args.length < 2) {
            mm.sendRaw(player, "<#FF5555>Usage: /msg <player> <message>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            mm.send(player, "admin.player-not-found");
            return;
        }

        if (target.equals(player)) {
            mm.sendRaw(player, "<#FF5555>You can't message yourself!");
            return;
        }

        // Check if target is ignoring the sender
        if (pmm.isIgnoring(target.getUniqueId(), player.getUniqueId())) {
            mm.send(player, "message.ignored");
            return;
        }

        String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        sendPrivateMessage(player, target, msg);
    }

    private void handleReply(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return;
        }

        if (args.length < 1) {
            mm.sendRaw(player, "<#FF5555>Usage: /r <message>");
            return;
        }

        java.util.UUID replyTo = pmm.getReplyTarget(player.getUniqueId());
        if (replyTo == null) {
            mm.send(player, "message.no-reply");
            return;
        }

        Player target = Bukkit.getPlayer(replyTo);
        if (target == null || !target.isOnline()) {
            mm.send(player, "message.reply-offline");
            return;
        }

        if (pmm.isIgnoring(target.getUniqueId(), player.getUniqueId())) {
            mm.send(player, "message.ignored");
            return;
        }

        String msg = String.join(" ", args);
        sendPrivateMessage(player, target, msg);
    }

    private void handleIgnore(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return;
        }
        if (args.length < 1) {
            mm.sendRaw(player, "<#FF5555>Usage: /ignore <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            mm.send(player, "admin.player-not-found");
            return;
        }
        if (target.equals(player)) {
            mm.sendRaw(player, "<#FF5555>You can't ignore yourself!");
            return;
        }

        boolean nowIgnoring = pmm.toggleIgnore(player.getUniqueId(), target.getUniqueId());
        if (nowIgnoring) {
            mm.send(player, "message.ignore-on", Map.of("player", target.getName()));
        } else {
            mm.send(player, "message.ignore-off", Map.of("player", target.getName()));
        }
    }

    private void sendPrivateMessage(Player sender, Player target, String message) {
        // Set reply targets
        pmm.setReplyTarget(sender.getUniqueId(), target.getUniqueId());
        pmm.setReplyTarget(target.getUniqueId(), sender.getUniqueId());

        // Send formatted messages
        mm.send(sender, "message.sent", Map.of("player", target.getName(), "message", message));
        mm.send(target, "message.received", Map.of("player", sender.getName(), "message", message));

        // Play a subtle receive sound for the target
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.8f);

        // SocialSpy — broadcast to all spies
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
        String cmd = label.toLowerCase();
        if (args.length == 1 && (cmd.equals("msg") || cmd.equals("tell") || cmd.equals("w") || cmd.equals("whisper")
                || cmd.equals("pm") || cmd.equals("ignore"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
