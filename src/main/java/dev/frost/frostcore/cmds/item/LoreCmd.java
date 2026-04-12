package dev.frost.frostcore.cmds.item;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LoreCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = Main.getMessageManager();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("frostcore.utility.lore")) {
            mm.send(player, "general.no-permission");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            mm.send(player, "utilities.no-item-held");
            return true;
        }

        if (args.length < 2) {
            mm.send(player, "utilities.lore-usage");
            return true;
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
                    return true;
                }
                try {
                    int line = Integer.parseInt(args[1]) - 1;
                    if (line < 0 || line >= lore.size() + 1) {
                        mm.send(player, "utilities.invalid-line");
                        return true;
                    }
                    String text = joinArgs(args, 2);
                    if (line == lore.size()) lore.add(miniMessage.deserialize("<!italic>" + text));
                    else lore.set(line, miniMessage.deserialize("<!italic>" + text));
                    mm.send(player, "utilities.lore-set-success");
                } catch (NumberFormatException e) {
                    mm.send(player, "utilities.invalid-line");
                    return true;
                }
            }
            case "remove" -> {
                try {
                    int line = Integer.parseInt(args[1]) - 1;
                    if (line < 0 || line >= lore.size()) {
                        mm.send(player, "utilities.invalid-line");
                        return true;
                    }
                    lore.remove(line);
                    mm.send(player, "utilities.lore-remove-success");
                } catch (NumberFormatException e) {
                    mm.send(player, "utilities.invalid-line");
                    return true;
                }
            }
            default -> {
                mm.send(player, "utilities.lore-usage");
                return true;
            }
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return true;
    }

    private String joinArgs(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            sb.append(args[i]).append(i == args.length - 1 ? "" : " ");
        }
        return sb.toString();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("add", "set", "remove").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove"))) {
            return List.of("1", "2", "3", "4", "5");
        }
        return Collections.emptyList();
    }
}
