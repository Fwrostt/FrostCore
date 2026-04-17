package dev.frost.frostcore.cmds.messaging;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.PrivateMessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * /chattoggle — Silences global chat for the player.
 * Private messages, team chat, announcements, broadcasts, and staff chat
 * are all unaffected — only the public AsyncChatEvent messages are filtered.
 */
public class ChatToggleCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = Main.getMessageManager();
    private final PrivateMessageManager pmm = PrivateMessageManager.getInstance();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("frostcore.chattoggle")) {
            mm.send(player, "general.no-permission");
            return true;
        }

        boolean nowHiding = pmm.toggleChatHide(player.getUniqueId());

        if (nowHiding) {
            mm.send(player, "message.chattoggle-off");
        } else {
            mm.send(player, "message.chattoggle-on");
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
