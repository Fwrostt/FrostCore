package dev.frost.frostcore.cmds.admin;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SkullCmd implements CommandExecutor, TabCompleter {
    private final MessageManager mm = Main.getMessageManager();
    private final MiniMessage mini = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission("frostcore.admin.skull")) {
            mm.send(player, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            mm.sendRaw(player, "<#FF5555>Usage: /skull <player>");
            return true;
        }

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(args[0]));
        meta.displayName(mini.deserialize("<white>" + args[0] + "'s Head"));
        skull.setItemMeta(meta);
        player.getInventory().addItem(skull);
        mm.sendRaw(player, "<gradient:#6BA3E3:#4979C7>FROST</gradient> <#AAAAAA>» <#B0C4FF>You received <white>" + args[0] + "</white>'s skull.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
