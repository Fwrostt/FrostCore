package dev.frost.frostcore.cmds;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminMiscCmds implements CommandExecutor, TabCompleter {

    private final MessageManager mm = Main.getMessageManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String cmd = label.toLowerCase();

        switch (cmd) {
            case "sudo" -> handleSudo(sender, args);
            case "broadcast" -> handleBroadcast(sender, args);
            case "chat" -> handleChat(sender, args);
            case "day" -> handleTime(sender, "day");
            case "night" -> handleTime(sender, "night");
            case "time" -> handleTimeSet(sender, args);
            case "weather" -> handleWeather(sender, args);
        }

        return true;
    }

    private void handleSudo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostcore.admin.sudo")) {
            mm.send(sender, "general.no-permission");
            return;
        }

        if (args.length < 2) {
            mm.sendRaw(sender, "<#FF5555>Usage: /sudo <player> <message|/command>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            mm.send(sender, "admin.player-not-found");
            return;
        }

        String action = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        if (action.startsWith("/")) {
            target.performCommand(action.substring(1));
        } else {
            target.chat(action);
        }

        mm.send(sender, "admin.sudo-success", Map.of("player", target.getName(), "action", action));
    }

    private void handleBroadcast(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostcore.admin.broadcast")) {
            mm.send(sender, "general.no-permission");
            return;
        }

        if (args.length < 1) {
            mm.sendRaw(sender, "<#FF5555>Usage: /broadcast <message>");
            return;
        }

        String msg = String.join(" ", args);
        MiniMessage mini = MiniMessage.miniMessage();
        Bukkit.broadcast(mm.getComponent("admin.broadcast-prefix").append(mini.deserialize(msg)));
    }

    private void handleChat(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("clear")) {
            if (!sender.hasPermission("frostcore.moderation.chatclear")) {
                mm.send(sender, "general.no-permission");
                return;
            }

            for (int i = 0; i < 100; i++) {
                Bukkit.broadcast(Component.empty());
            }
            mm.broadcast("admin.chat-cleared", Map.of("player", sender.getName()));
        }
    }

    private void handleTime(CommandSender sender, String timeType) {
        if (!sender.hasPermission("frostcore.admin.time")) {
            mm.send(sender, "general.no-permission");
            return;
        }

        World world = (sender instanceof Player p) ? p.getWorld() : Bukkit.getWorlds().get(0);
        if (timeType.equals("day")) {
            world.setTime(1000);
        } else {
            world.setTime(13000);
        }
        mm.send(sender, "admin.time-set", Map.of("time", timeType));
    }

    private void handleTimeSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostcore.admin.time")) {
            mm.send(sender, "general.no-permission");
            return;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("set")) {
            mm.sendRaw(sender, "<#FF5555>Usage: /time set <value|day|night>");
            return;
        }

        World world = (sender instanceof Player p) ? p.getWorld() : Bukkit.getWorlds().get(0);
        String value = args[1].toLowerCase();
        
        try {
            long time;
            switch (value) {
                case "day" -> time = 1000;
                case "night" -> time = 13000;
                case "noon" -> time = 6000;
                case "midnight" -> time = 18000;
                default -> time = Long.parseLong(value);
            }
            world.setTime(time);
            mm.send(sender, "admin.time-set", Map.of("time", value));
        } catch (NumberFormatException e) {
            mm.sendRaw(sender, "<#FF5555>Invalid time value.");
        }
    }

    private void handleWeather(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostcore.admin.weather")) {
            mm.send(sender, "general.no-permission");
            return;
        }

        if (args.length < 1) {
            mm.sendRaw(sender, "<#FF5555>Usage: /weather <clear|rain|thunder>");
            return;
        }

        World world = (sender instanceof Player p) ? p.getWorld() : Bukkit.getWorlds().get(0);
        String type = args[0].toLowerCase();
        
        switch (type) {
            case "clear" -> {
                world.setStorm(false);
                world.setThundering(false);
            }
            case "rain" -> {
                world.setStorm(true);
                world.setThundering(false);
            }
            case "thunder" -> {
                world.setStorm(true);
                world.setThundering(true);
            }
            default -> {
                mm.sendRaw(sender, "<#FF5555>Invalid weather type.");
                return;
            }
        }
        mm.send(sender, "admin.weather-set", Map.of("weather", type));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String cmd = label.toLowerCase();
        
        if (cmd.equals("sudo")) {
            if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        } else if (cmd.equals("time")) {
            if (args.length == 1) return List.of("set");
            if (args.length == 2) return List.of("day", "night", "noon", "midnight", "0", "12000");
        } else if (cmd.equals("weather")) {
            if (args.length == 1) return List.of("clear", "rain", "thunder");
        } else if (cmd.equals("chat")) {
            if (args.length == 1) return List.of("clear");
        }
        
        return Collections.emptyList();
    }
}
