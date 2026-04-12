package dev.frost.frostcore.cmds.player;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GamemodeCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm;

    public GamemodeCmd() {
        this.mm = Main.getMessageManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        GameMode targetMode = null;
        Player targetPlayer = null;

        switch (label.toLowerCase()) {
            case "gm":
                if (args.length == 0) {
                    mm.send(sender, "admin.gamemode-usage");
                    return true;
                }
                targetMode = parseGameMode(args[0]);
                if (args.length > 1) {
                    targetPlayer = org.bukkit.Bukkit.getPlayer(args[1]);
                }
                break;
            case "gms":
                targetMode = GameMode.SURVIVAL;
                if (args.length > 0) targetPlayer = org.bukkit.Bukkit.getPlayer(args[0]);
                break;
            case "gmc":
                targetMode = GameMode.CREATIVE;
                if (args.length > 0) targetPlayer = org.bukkit.Bukkit.getPlayer(args[0]);
                break;
            case "gma":
                targetMode = GameMode.ADVENTURE;
                if (args.length > 0) targetPlayer = org.bukkit.Bukkit.getPlayer(args[0]);
                break;
            case "gmsp":
                targetMode = GameMode.SPECTATOR;
                if (args.length > 0) targetPlayer = org.bukkit.Bukkit.getPlayer(args[0]);
                break;
        }

        if (targetMode == null) {
            mm.send(sender, "admin.gamemode-invalid");
            return true;
        }

        if (targetPlayer != null) {
            if (!sender.hasPermission("frostcore.gamemode.others") && !sender.hasPermission("frostcore.admin")) {
                mm.send(sender, "general.no-permission");
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("You must specify a player when using from console.");
                return true;
            }
            targetPlayer = (Player) sender;
            
            String specificPerm = "frostcore.gamemode." + targetMode.name().toLowerCase();
            if (!sender.hasPermission("frostcore.gamemode") && !sender.hasPermission(specificPerm) && !sender.hasPermission("frostcore.admin")) {
                mm.send(sender, "general.no-permission");
                return true;
            }
        }

        if (targetPlayer == null) {
            mm.send(sender, "admin.player-not-found");
            return true;
        }

        targetPlayer.setGameMode(targetMode);
        
        if (targetPlayer.equals(sender)) {
            mm.send(targetPlayer, "admin.gamemode-changed", Map.of("mode", targetMode.name().toLowerCase()));
        } else {
            mm.send(sender, "admin.gamemode-changed-other", Map.of(
                "mode", targetMode.name().toLowerCase(),
                "player", targetPlayer.getName()
            ));
            mm.send(targetPlayer, "admin.gamemode-changed", Map.of("mode", targetMode.name().toLowerCase()));
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("frostcore.gamemode") && !sender.hasPermission("frostcore.admin")) return List.of();

        if (label.equalsIgnoreCase("gm")) {
            if (args.length == 1) {
                return List.of("0", "1", "2", "3", "survival", "creative", "adventure", "spectator", "s", "c", "a", "sp").stream()
                        .filter(s -> s.startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args.length == 2) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        } else {
            if (args.length == 1) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return List.of();
    }

    private GameMode parseGameMode(String input) {
        return switch (input.toLowerCase()) {
            case "0", "s", "survival" -> GameMode.SURVIVAL;
            case "1", "c", "creative" -> GameMode.CREATIVE;
            case "2", "a", "adventure" -> GameMode.ADVENTURE;
            case "3", "sp", "spectator" -> GameMode.SPECTATOR;
            default -> null;
        };
    }
}
