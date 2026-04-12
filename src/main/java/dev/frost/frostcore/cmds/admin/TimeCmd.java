package dev.frost.frostcore.cmds.admin;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
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

public class TimeCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = Main.getMessageManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("frostcore.admin.time")) {
            mm.send(sender, "general.no-permission");
            return true;
        }

        World world = (sender instanceof Player p) ? p.getWorld() : Bukkit.getWorlds().get(0);
        String cmd = label.toLowerCase();
        
        if (cmd.equals("day")) {
            world.setTime(1000);
            mm.send(sender, "admin.time-set", Map.of("time", "day"));
            return true;
        } else if (cmd.equals("night")) {
            world.setTime(13000);
            mm.send(sender, "admin.time-set", Map.of("time", "night"));
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("set")) {
            mm.sendRaw(sender, "<#FF5555>/time set <value|day|night>");
            return true;
        }

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
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String cmd = label.toLowerCase();
        if (cmd.equals("time")) {
            if (args.length == 1) return List.of("set");
            if (args.length == 2) return List.of("day", "night", "noon", "midnight", "0", "12000");
        }
        return Collections.emptyList();
    }
}
