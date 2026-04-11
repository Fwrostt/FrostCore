package dev.frost.frostcore.cmds;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.utils.TeleportUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TpCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = MessageManager.get();
    private final TeleportUtil teleportUtil = Main.getTeleportUtil();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("tp")) {
            if (args.length == 1) {
                // /tp <player>
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    mm.send(player, "teleport.player-not-found");
                    return true;
                }
                teleportUtil.teleportInstant(player, target.getLocation());
                mm.send(player, "teleport.tp-player", Map.of("player", target.getName()));
                return true;

            } else if (args.length == 3) {
                // /tp <x> <y> <z>
                try {
                    double x = parseCoord(args[0], player.getLocation().getX());
                    double y = parseCoord(args[1], player.getLocation().getY());
                    double z = parseCoord(args[2], player.getLocation().getZ());

                    Location loc = new Location(player.getWorld(), x, y, z, player.getLocation().getYaw(), player.getLocation().getPitch());
                    teleportUtil.teleportInstant(player, loc);
                    mm.send(player, "teleport.tp-coords", Map.of(
                            "x", String.format("%.1f", x),
                            "y", String.format("%.1f", y),
                            "z", String.format("%.1f", z)
                    ));
                } catch (NumberFormatException e) {
                    mm.sendRaw(player, "<red>Invalid coordinates.</red>");
                }
                return true;
            } else {
                mm.sendRaw(player, "<#B0C4FF>Usage: <white>/tp <player> <#B0C4FF>or <white>/tp <x> <y> <z>");
                return true;
            }
        } else if (cmdName.equals("tp2p")) {
            // /tp2p <player1> <player2>
            if (args.length < 2) {
                mm.sendRaw(player, "<#B0C4FF>Usage: <white>/tp2p <player1> <player2>");
                return true;
            }
            Player target1 = Bukkit.getPlayerExact(args[0]);
            Player target2 = Bukkit.getPlayerExact(args[1]);

            if (target1 == null || target2 == null) {
                mm.send(player, "teleport.player-not-found");
                return true;
            }
            teleportUtil.teleportInstant(target1, target2.getLocation());
            mm.send(player, "teleport.tp-to-player", Map.of("player", target2.getName()));
            return true;

        } else if (cmdName.equals("tphere")) {
            // /tphere <player>
            if (args.length < 1) {
                mm.sendRaw(player, "<#B0C4FF>Usage: <white>/tphere <player>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                mm.send(player, "teleport.player-not-found");
                return true;
            }

            teleportUtil.teleportInstant(target, player.getLocation());
            mm.send(player, "teleport.tp-here", Map.of("player", target.getName()));
            mm.send(target, "teleport.tp-here-target", Map.of("player", player.getName()));
            return true;
        }

        return true;
    }

    private double parseCoord(String arg, double current) throws NumberFormatException {
        if (arg.startsWith("~")) {
            if (arg.length() == 1) return current;
            return current + Double.parseDouble(arg.substring(1));
        }
        return Double.parseDouble(arg);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("tp")) {
            if (args.length == 1) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            } else if (args.length == 2 || args.length == 3) {
                return List.of("~");
            }
        } else if (cmdName.equals("tp2p") && (args.length == 1 || args.length == 2)) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        } else if (cmdName.equals("tphere") && args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }

        return Collections.emptyList();
    }
}
