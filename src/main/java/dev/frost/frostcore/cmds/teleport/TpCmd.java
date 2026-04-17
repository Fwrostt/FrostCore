package dev.frost.frostcore.cmds.teleport;

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

        if (!player.hasPermission("frostcore.admin")) {
            mm.sendRaw(player, "<red>You don't have permission to use this command.");
            return true;
        }

        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("tp")) {

            if (args.length == 1) {
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    mm.send(player, "teleport.player-not-found");
                    return true;
                }

                teleportUtil.teleportInstant(player, target.getLocation());
                mm.send(player, "teleport.tp-player", Map.of("player", target.getName()));
                return true;
            }

            if (args.length == 2) {
                Player target1 = Bukkit.getPlayerExact(args[0]);
                Player target2 = Bukkit.getPlayerExact(args[1]);

                if (target1 == null || target2 == null) {
                    mm.send(player, "teleport.player-not-found");
                    return true;
                }

                teleportUtil.teleportInstant(target1, target2.getLocation());
                mm.send(player, "teleport.tp-to-player", Map.of("player", target2.getName()));
                return true;
            }

            if (args.length == 3) {
                try {
                    double x = parseCoord(args[0], player.getLocation().getX());
                    double y = parseCoord(args[1], player.getLocation().getY());
                    double z = parseCoord(args[2], player.getLocation().getZ());

                    Location loc = new Location(
                            player.getWorld(),
                            x, y, z,
                            player.getLocation().getYaw(),
                            player.getLocation().getPitch()
                    );

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
            }

            if (args.length == 4) {
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    mm.send(player, "teleport.player-not-found");
                    return true;
                }

                try {
                    double x = parseCoord(args[1], target.getLocation().getX());
                    double y = parseCoord(args[2], target.getLocation().getY());
                    double z = parseCoord(args[3], target.getLocation().getZ());

                    Location loc = new Location(
                            target.getWorld(),
                            x, y, z,
                            target.getLocation().getYaw(),
                            target.getLocation().getPitch()
                    );

                    teleportUtil.teleportInstant(target, loc);
                    mm.send(player, "teleport.tp-player-coords", Map.of(
                            "player", target.getName(),
                            "x", String.format("%.1f", x),
                            "y", String.format("%.1f", y),
                            "z", String.format("%.1f", z)
                    ));
                } catch (NumberFormatException e) {
                    mm.sendRaw(player, "<red>Invalid coordinates.</red>");
                }
                return true;
            }

            mm.sendRaw(player, "<#B0C4FF>/tp <player> <#B0C4FF>| <white>/tp <player1> <player2> <#B0C4FF>| <white>/tp <x> <y> <z> <#B0C4FF>| <white>/tp <player> <x> <y> <z>");
            return true;
        } else if (cmdName.equals("tp2p")) {

            if (args.length < 1) {
                mm.sendRaw(player, "<#B0C4FF>/tp2p <player>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                mm.send(player, "teleport.player-not-found");
                return true;
            }

            teleportUtil.teleportInstant(player, target.getLocation());
            mm.send(player, "teleport.tp-player", Map.of("player", target.getName()));
            return true;
        } else if (cmdName.equals("tphere")) {

            if (args.length < 1) {
                mm.sendRaw(player, "<#B0C4FF>/tphere <player>");
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
            if (args.length == 1 || args.length == 2) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            } else if (args.length == 3 || args.length == 4) {
                return List.of("~");
            }
        } else if (cmdName.equals("tp2p") && args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        } else if (cmdName.equals("tphere") && args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }

        return Collections.emptyList();
    }
}

