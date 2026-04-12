package dev.frost.frostcore.cmds.item;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RepairCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = Main.getMessageManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("frostcore.utility.repair")) {
            mm.send(player, "general.no-permission");
            return true;
        }

        boolean all = args.length > 0 && args[0].equalsIgnoreCase("all");
        Player target = player;
        
        if (all && args.length > 1) {
            target = Bukkit.getPlayer(args[1]);
        } else if (!all && args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
        }

        if (target == null) {
            mm.send(player, "admin.player-not-found");
            return true;
        }

        if (all) {
            for (ItemStack item : target.getInventory().getContents()) {
                repairItem(item);
            }
            mm.send(player, "utilities.repair-all-success", Map.of("player", target.getName()));
        } else {
            ItemStack item = target.getInventory().getItemInMainHand();
            if (item.getType() == Material.AIR) {
                mm.send(player, "utilities.no-item-held");
                return true;
            }
            if (repairItem(item)) {
                mm.send(player, "utilities.repair-success", Map.of("player", target.getName()));
            } else {
                mm.send(player, "utilities.repair-not-damageable");
            }
        }
        return true;
    }

    private boolean repairItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable d) {
            if (d.hasDamage()) {
                d.setDamage(0);
                item.setItemMeta(meta);
                return true;
            }
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>(getPlayerNames(args[0]));
            if ("all".startsWith(args[0].toLowerCase())) list.add("all");
            return list;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("all")) {
            return getPlayerNames(args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> getPlayerNames(String start) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(start.toLowerCase()))
                .collect(Collectors.toList());
    }
}
