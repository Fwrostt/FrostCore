package dev.frost.frostcore.cmds.moderation;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.moderation.ModerationManager;
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

public class FreezeCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) { mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>/freeze <player>"); return true; }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>Player not found."); return true; }
        if (!Main.getGroupLimitManager().canPunish(sender, target)) { mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>You cannot punish this player."); return true; }

        ModerationManager mod = ModerationManager.getInstance();
        mod.toggleFreeze(target.getUniqueId());
        boolean frozen = mod.isFrozen(target.getUniqueId());
        String state = frozen ? "<#D4727A>Frozen" : "<#7ECFA0>Unfrozen";
        mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> " + state + " <white>" + target.getName() + ".");
        mm.sendRaw(target, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>You have been " + (frozen ? "<#D4727A>frozen" : "<#7ECFA0>unfrozen") + " <#8FA3BF>by staff.");
        return true;
    }
    @Override public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList()) : Collections.emptyList();
    }
}
