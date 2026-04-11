package dev.frost.frostcore.cmds;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
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

public class ItemEditCmds implements CommandExecutor, TabCompleter {

    private final MessageManager mm = Main.getMessageManager();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use these items commands.");
            return true;
        }

        String cmd = label.toLowerCase();
        
        switch (cmd) {
            case "itemrename" -> handleRename(player, args);
            case "lore" -> handleLore(player, args);
            case "repair" -> handleRepair(player, args);
        }

        return true;
    }

    private void handleRename(Player player, String[] args) {
        if (!player.hasPermission("frostcore.utility.rename")) {
            mm.send(player, "general.no-permission");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            mm.send(player, "utilities.no-item-held");
            return;
        }

        if (args.length == 0) {
            mm.send(player, "utilities.rename-usage");
            return;
        }

        String name = String.join(" ", args);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(miniMessage.deserialize("<!italic>" + name));
        item.setItemMeta(meta);

        mm.send(player, "utilities.rename-success", Map.of("name", name));
    }

    private void handleLore(Player player, String[] args) {
        if (!player.hasPermission("frostcore.utility.lore")) {
            mm.send(player, "general.no-permission");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            mm.send(player, "utilities.no-item-held");
            return;
        }

        if (args.length < 2) {
            mm.send(player, "utilities.lore-usage");
            return;
        }

        String action = args[0].toLowerCase();
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
        if (lore == null) lore = new ArrayList<>();

        switch (action) {
            case "add" -> {
                String text = joinArgs(args, 1);
                lore.add(miniMessage.deserialize("<!italic>" + text));
                mm.send(player, "utilities.lore-add-success");
            }
            case "set" -> {
                if (args.length < 3) {
                    mm.send(player, "utilities.lore-usage");
                    return;
                }
                try {
                    int line = Integer.parseInt(args[1]) - 1;
                    if (line < 0 || line >= lore.size() + 1) {
                        mm.send(player, "utilities.invalid-line");
                        return;
                    }
                    String text = joinArgs(args, 2);
                    if (line == lore.size()) lore.add(miniMessage.deserialize("<!italic>" + text));
                    else lore.set(line, miniMessage.deserialize("<!italic>" + text));
                    mm.send(player, "utilities.lore-set-success");
                } catch (NumberFormatException e) {
                    mm.send(player, "utilities.invalid-line");
                    return;
                }
            }
            case "remove" -> {
                try {
                    int line = Integer.parseInt(args[1]) - 1;
                    if (line < 0 || line >= lore.size()) {
                        mm.send(player, "utilities.invalid-line");
                        return;
                    }
                    lore.remove(line);
                    mm.send(player, "utilities.lore-remove-success");
                } catch (NumberFormatException e) {
                    mm.send(player, "utilities.invalid-line");
                    return;
                }
            }
            default -> {
                mm.send(player, "utilities.lore-usage");
                return;
            }
        }

        meta.lore(lore);
        item.setItemMeta(meta);
    }

    private void handleRepair(Player player, String[] args) {
        if (!player.hasPermission("frostcore.utility.repair")) {
            mm.send(player, "general.no-permission");
            return;
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
            return;
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
                return;
            }
            if (repairItem(item)) {
                mm.send(player, "utilities.repair-success", Map.of("player", target.getName()));
            } else {
                mm.send(player, "utilities.repair-not-damageable");
            }
        }
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
        String cmd = label.toLowerCase();
        
        switch (cmd) {
            case "repair" -> {
                if (args.length == 1) {
                    List<String> list = new ArrayList<>(getPlayerNames(args[0]));
                    if ("all".startsWith(args[0].toLowerCase())) list.add("all");
                    return list;
                }
                if (args.length == 2 && args[0].equalsIgnoreCase("all")) {
                    return getPlayerNames(args[1]);
                }
            }
            case "lore" -> {
                if (args.length == 1) {
                    return List.of("add", "set", "remove").stream()
                            .filter(s -> s.startsWith(args[0].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove"))) {
                    return List.of("1", "2", "3", "4", "5");
                }
            }
        }
        return Collections.emptyList();
    }

    private List<String> getPlayerNames(String start) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(start.toLowerCase()))
                .collect(Collectors.toList());
    }

    private String joinArgs(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            sb.append(args[i]).append(i == args.length - 1 ? "" : " ");
        }
        return sb.toString();
    }
}
