package dev.frost.frostcore.cmds.admin;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
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

public class TpallCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("frostcore.admin.tpall")) {
            mm.send(player, "general.no-permission");
            return true;
        }

        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(player)) {
                p.teleport(player.getLocation());
                mm.sendRaw(p, "<gradient:#6B8DAE:#8BADC4>TELEPORT</gradient> <dark_gray>» <#8FA3BF>You were teleported to <white>" + player.getName() + "</white>.");
                count++;
            }
        }
        mm.send(sender, "admin.tpall-success", Map.of("count", String.valueOf(count)));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
