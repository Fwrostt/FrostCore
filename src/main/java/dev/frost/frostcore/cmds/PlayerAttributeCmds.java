package dev.frost.frostcore.cmds;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.UtilityManager;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
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
import java.util.stream.Collectors;

public class PlayerAttributeCmds implements CommandExecutor, TabCompleter {

    private final MessageManager mm = Main.getMessageManager();
    private final UtilityManager um = UtilityManager.getInstance();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String cmd = label.toLowerCase();
        
        Player target;
        if (args.length > 0 && sender.hasPermission("frostcore.utility.others")) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                mm.send(sender, "admin.player-not-found");
                return true;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage("You must specify a player when using from console.");
            return true;
        }

        switch (cmd) {
            case "fly" -> handleFly(sender, target, args);
            case "heal" -> handleHeal(sender, target);
            case "feed" -> handleFeed(sender, target);
            case "god" -> handleGod(sender, target);
            case "clear" -> handleClear(sender, target);
            case "speed" -> handleSpeed(sender, target, args);
        }

        return true;
    }

    private void handleFly(CommandSender sender, Player target, String[] args) {
        if (!sender.hasPermission("frostcore.utility.fly")) {
            mm.send(sender, "general.no-permission");
            return;
        }

        boolean flying = !target.getAllowFlight();
        target.setAllowFlight(flying);
        if (flying) target.setFlying(true);

        if (args.length > 1) {
            try {
                float speed = Float.parseFloat(args[1]);
                if (speed < 1) speed = 1;
                if (speed > 10) speed = 10;
                target.setFlySpeed(speed / 10.0f);
            } catch (NumberFormatException ignored) {}
        }

        if (sender.equals(target)) {
            mm.send(target, "utilities.fly-toggle", Map.of("state", flying ? "<#55FF55>Enabled" : "<#FF5555>Disabled"));
        } else {
            mm.send(sender, "utilities.fly-toggle-other", Map.of(
                "player", target.getName(),
                "state", flying ? "<#55FF55>Enabled" : "<#FF5555>Disabled"
            ));
            mm.send(target, "utilities.fly-toggle", Map.of("state", flying ? "<#55FF55>Enabled" : "<#FF5555>Disabled"));
        }
    }

    private void handleHeal(CommandSender sender, Player target) {
        if (!sender.hasPermission("frostcore.utility.heal")) {
            mm.send(sender, "general.no-permission");
            return;
        }

        double maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        target.setHealth(maxHealth);
        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.setFireTicks(0);
        target.getActivePotionEffects().forEach(effect -> target.removePotionEffect(effect.getType()));

        if (sender.equals(target)) {
            mm.send(target, "utilities.heal-success");
        } else {
            mm.send(sender, "utilities.heal-success-other", Map.of("player", target.getName()));
            mm.send(target, "utilities.heal-success");
        }
    }

    private void handleFeed(CommandSender sender, Player target) {
        if (!sender.hasPermission("frostcore.utility.feed")) {
            mm.send(sender, "general.no-permission");
            return;
        }

        target.setFoodLevel(20);
        target.setSaturation(20f);

        if (sender.equals(target)) {
            mm.send(target, "utilities.feed-success");
        } else {
            mm.send(sender, "utilities.feed-success-other", Map.of("player", target.getName()));
            mm.send(target, "utilities.feed-success");
        }
    }

    private void handleGod(CommandSender sender, Player target) {
        if (!sender.hasPermission("frostcore.utility.god")) {
            mm.send(sender, "general.no-permission");
            return;
        }

        um.toggleGodMode(target.getUniqueId());
        boolean enabled = um.isGodMode(target.getUniqueId());

        if (sender.equals(target)) {
            mm.send(target, "utilities.god-toggle", Map.of("state", enabled ? "<#55FF55>Enabled" : "<#FF5555>Disabled"));
        } else {
            mm.send(sender, "utilities.god-toggle-other", Map.of(
                "player", target.getName(),
                "state", enabled ? "<#55FF55>Enabled" : "<#FF5555>Disabled"
            ));
            mm.send(target, "utilities.god-toggle", Map.of("state", enabled ? "<#55FF55>Enabled" : "<#FF5555>Disabled"));
        }
    }

    private void handleClear(CommandSender sender, Player target) {
        if (!sender.hasPermission("frostcore.utility.clear")) {
            mm.send(sender, "general.no-permission");
            return;
        }

        target.getInventory().clear();

        if (sender.equals(target)) {
            mm.send(target, "utilities.clear-success");
        } else {
            mm.send(sender, "utilities.clear-success-other", Map.of("player", target.getName()));
            mm.send(target, "utilities.clear-success");
        }
    }

    private void handleSpeed(CommandSender sender, Player target, String[] args) {
        if (!sender.hasPermission("frostcore.utility.speed")) {
            mm.send(sender, "general.no-permission");
            return;
        }

        if (args.length < 2) {
            mm.send(sender, "utilities.speed-usage");
            return;
        }

        String type = args[0].toLowerCase();
        int speedVal;
        try {
            speedVal = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            mm.send(sender, "utilities.speed-invalid");
            return;
        }

        if (speedVal < 1) speedVal = 1;
        if (speedVal > 10) speedVal = 10;
        float fSpeed = speedVal / 10.0f;

        if (type.equals("fly")) {
            target.setFlySpeed(fSpeed);
            mm.send(sender, "utilities.speed-changed", Map.of("type", "fly", "val", String.valueOf(speedVal)));
        } else if (type.equals("walk")) {
            target.setWalkSpeed(Math.min(fSpeed, 1.0f));
            mm.send(sender, "utilities.speed-changed", Map.of("type", "walk", "val", String.valueOf(speedVal)));
        } else {
            mm.send(sender, "utilities.speed-usage");
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String cmd = label.toLowerCase();
        
        switch (cmd) {
            case "fly" -> {
                if (args.length == 1) return getPlayerNames(args[0]);
                if (args.length == 2) return List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
            }
            case "speed" -> {
                if (args.length == 1) return List.of("fly", "walk").stream()
                        .filter(s -> s.startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
                if (args.length == 2) return List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
                if (args.length == 3) return getPlayerNames(args[2]);
            }
            case "heal", "feed", "god", "clear" -> {
                if (args.length == 1) return getPlayerNames(args[0]);
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
}
