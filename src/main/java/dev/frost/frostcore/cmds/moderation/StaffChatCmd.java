package dev.frost.frostcore.cmds.moderation;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.utils.FrostLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StaffChatCmd implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Set<UUID> toggledPlayers = ConcurrentHashMap.newKeySet();
    private final MessageManager mm = Main.getMessageManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            // Toggle staff chat mode
            if (!(sender instanceof Player player)) {
                mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>Only players can toggle staff chat. Use <white>/sc <message></white> to send.");
                return true;
            }

            UUID uuid = player.getUniqueId();
            if (toggledPlayers.contains(uuid)) {
                toggledPlayers.remove(uuid);
                mm.sendRaw(player, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>Staff chat <#D4727A><bold>disabled</bold></#D4727A>. You are now in global chat.");
            } else {
                toggledPlayers.add(uuid);
                mm.sendRaw(player, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>Staff chat <#7ECFA0><bold>enabled</bold></#7ECFA0>. Your messages are now staff-only.");
            }
            return true;
        }

        // Direct message: /sc <message>
        String message = String.join(" ", args);
        broadcastStaffMessage(sender, message);
        return true;
    }

    public static void broadcastStaffMessage(CommandSender sender, String message) {
        String senderName = sender instanceof Player ? sender.getName() : "CONSOLE";
        MessageManager mm = Main.getMessageManager();

        Component formatted = mm.getComponent("moderation.staffchat-format", Map.of(
                "player", senderName,
                "message", message
        ));

        // Send to all online staff + console
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("frostcore.staffchat") || online.isOp()) {
                online.sendMessage(formatted);
            }
        }

        Bukkit.getConsoleSender().sendMessage(
                Component.text("[StaffChat] ").append(formatted)
        );

        // Discord webhook
        if (Main.getWebhookManager() != null) {
            Main.getWebhookManager().sendStaffChatWebhookAsync(senderName, message);
        }

        FrostLogger.audit("STAFFCHAT: " + senderName + ": " + message);
    }

    public static boolean isToggled(UUID uuid) {
        return toggledPlayers.contains(uuid);
    }

    public static void removeToggle(UUID uuid) {
        toggledPlayers.remove(uuid);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                                @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
