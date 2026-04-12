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


public class LockdownCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        ModerationManager mod = ModerationManager.getInstance();
        if (mod.isLockdown()) {
            mod.setLockdown(false, null);
            mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#7ECFA0>Server lockdown <white>disabled.");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("frostcore.moderation.notify")) {
                    mm.sendRaw(p, "<#D4727A>MOD <dark_gray>»</dark_gray> <#7ECFA0>Server lockdown has been disabled by <white>" + sender.getName());
                }
            }
            if (mod.getWebhookManager() != null) mod.getWebhookManager().sendStaffActivityAsync("Lockdown Disabled", sender.getName(), "Server is now open.");
        } else {
            String reason = args.length > 0 ? String.join(" ", args) : "Maintenance";
            mod.setLockdown(true, reason);
            mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>Server lockdown <white>enabled. <dark_gray>(" + reason + ")");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("frostcore.moderation.notify")) {
                    mm.sendRaw(p, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>Server lockdown enabled by <white>" + sender.getName() + " <dark_gray>— " + reason);
                }
            }
            if (mod.getWebhookManager() != null) mod.getWebhookManager().sendStaffActivityAsync("Lockdown Enabled", sender.getName(), reason);
        }
        return true;
    }
    @Override public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) { return Collections.emptyList(); }
}
