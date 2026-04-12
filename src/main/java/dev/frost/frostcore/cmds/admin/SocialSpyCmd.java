package dev.frost.frostcore.cmds.admin;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class SocialSpyCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("frostcore.admin.socialspy")) {
            mm.send(player, "general.no-permission");
            return true;
        }

        boolean enabled = Main.getPrivateMessageManager().toggleSocialSpy(player.getUniqueId());
        if (enabled) {
            mm.sendRaw(player, "<gradient:#FF5555:#FF55FF>ADMIN <#AAAAAA>» <#B0C4FF>SocialSpy <#55FF55><bold>ENABLED</bold></#55FF55>. You can now see all private messages.");
        } else {
            mm.sendRaw(player, "<gradient:#FF5555:#FF55FF>ADMIN <#AAAAAA>» <#B0C4FF>SocialSpy <#FF5555><bold>DISABLED</bold></#FF5555>.");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
