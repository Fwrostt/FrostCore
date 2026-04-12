package dev.frost.frostcore.cmds.moderation;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.gui.impls.AltsGui;
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

public class AltsCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) { mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>/alts <player>"); return true; }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            ModerationDatabase modDb = ModerationManager.getInstance().getDatabase();
            Set<UUID> alts = modDb.getAlts(target.getUniqueId());

            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                if (sender instanceof Player p) {
                    AltsGui.open(p, target, alts);
                } else {
                    mm.sendRaw(sender, "");
                    mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>Alt accounts for <white>" + target.getName() + " <dark_gray>(" + alts.size() + ")");
                    mm.sendRaw(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    if (alts.isEmpty()) mm.sendRaw(sender, "  <#707880>No known alt accounts.");
                    else for (UUID altUuid : alts) {
                        String altName = modDb.getLastKnownName(altUuid);
                        if (altName == null) altName = altUuid.toString().substring(0, 8);
                        boolean banned = ModerationManager.getInstance().isBanned(altUuid);
                        String status = banned ? "<#D4727A>Banned" : "<#7ECFA0>Clean";
                        OfflinePlayer altPlayer = Bukkit.getOfflinePlayer(altUuid);
                        String online = altPlayer.isOnline() ? " <#7ECFA0>●" : " <#707880>○";
                        mm.sendRaw(sender, "  " + online + " <white>" + altName + " <dark_gray>| " + status);
                    }
                    mm.sendRaw(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                }
            });
        });
        return true;
    }
    @Override public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList()) : Collections.emptyList();
    }
}
