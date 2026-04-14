package dev.frost.frostcore.cmds.player;

import dev.frost.frostcore.glow.*;
import dev.frost.frostcore.manager.GlowManager;
import dev.frost.frostcore.Main;
import dev.frost.frostcore.gui.*;
import dev.frost.frostcore.gui.impls.GlowGui;
import dev.frost.frostcore.manager.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GlowCmd implements CommandExecutor, TabCompleter {

    private static final String PREFIX = "<#6B8DAE>GLOW <dark_gray>» ";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        MessageManager mm = Main.getMessageManager();
        GlowManager mgr = Main.getGlowManager();

        if (!(sender instanceof Player player)) {
            if (args.length == 2) {
                // Let console process args later
            } else {
                sender.sendMessage("Usage: /glow <player> <color>");
                return true;
            }
        }

        if (sender instanceof Player p && !p.hasPermission("frostcore.glow.use") && !p.hasPermission("frostcore.glow.admin")) {
            mm.sendRaw(sender, PREFIX + "<#D4727A>You don't have permission to use glow.");
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player p) {
                GuiManager.schedule(() -> GlowGui.open(p));
            }
            return true;
        }

        if (args.length == 1) {
            Player player = (Player) sender;
            String input = args[0];

            if (input.equalsIgnoreCase("off") || input.equalsIgnoreCase("remove") || input.equalsIgnoreCase("none")) {
                mgr.removeGlow(player);
                mm.sendRaw(player, PREFIX + "<#8FA3BF>Glow removed.");
                return true;
            }

            GlowColor color = GlowColor.fromName(input);
            if (color == null) {
                mm.sendRaw(player, PREFIX + "<#D4727A>Unknown color: <white>" + input);
                mm.sendRaw(player, PREFIX + "<dark_gray>Available: <#8FA3BF>" + getColorList(player));
                return true;
            }

            if (!mgr.hasPermission(player, color)) {
                mm.sendRaw(player, PREFIX + "<#D4727A>You don't have access to " + color.getColoredName() + "<#D4727A>.");
                return true;
            }

            mgr.setGlow(player, color);
            mm.sendRaw(player, PREFIX + color.getColoredName() + " <#8FA3BF>glow applied.");
            return true;
        }

        if (args.length == 2) {
            if (!sender.hasPermission("frostcore.glow.admin")) {
                mm.sendRaw(sender, PREFIX + "<#D4727A>You don't have permission to set glow on others.");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                mm.sendRaw(sender, PREFIX + "<#D4727A>Player not found.");
                return true;
            }

            String colorInput = args[1];
            if (colorInput.equalsIgnoreCase("off") || colorInput.equalsIgnoreCase("remove") || colorInput.equalsIgnoreCase("none")) {
                mgr.removeGlow(target);
                mm.sendRaw(sender, PREFIX + "<#7ECFA0>Removed glow from <white>" + target.getName());
                mm.sendRaw(target, PREFIX + "<#8FA3BF>Your glow was removed by an admin.");
                return true;
            }

            GlowColor color = GlowColor.fromName(colorInput);
            if (color == null) {
                mm.sendRaw(sender, PREFIX + "<#D4727A>Unknown color: <white>" + colorInput);
                return true;
            }

            mgr.setGlow(target, color);
            mm.sendRaw(sender, PREFIX + "<#7ECFA0>Set " + color.getColoredName() + " <#7ECFA0>glow on <white>" + target.getName());
            mm.sendRaw(target, PREFIX + "An admin set your glow to " + color.getColoredName());
            return true;
        }

        mm.sendRaw(sender, PREFIX + "<#D4727A>Usage: /glow [color] or /glow <player> <color>");
        return true;
    }

    private String getColorList(Player player) {
        GlowManager mgr = Main.getGlowManager();
        return Arrays.stream(GlowColor.values())
                .filter(c -> mgr.hasPermission(player, c))
                .map(c -> c.getColoredName())
                .collect(Collectors.joining("<dark_gray>, "));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String alias, @NotNull String[] args) {

        if (args.length == 1) {
            List<String> options = new java.util.ArrayList<>();
            options.add("off");

            if (sender.hasPermission("frostcore.glow.admin")) {
                Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
            }

            if (sender instanceof Player player) {
                GlowManager mgr = Main.getGlowManager();
                for (GlowColor c : GlowColor.values()) {
                    if (mgr.hasPermission(player, c)) {
                        options.add(c.name().toLowerCase());
                    }
                }
            }

            return filter(options, args[0]);
        }

        if (args.length == 2 && sender.hasPermission("frostcore.glow.admin")) {
            List<String> options = new java.util.ArrayList<>();
            options.add("off");
            for (GlowColor c : GlowColor.values()) {
                options.add(c.name().toLowerCase());
            }
            return filter(options, args[1]);
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}
