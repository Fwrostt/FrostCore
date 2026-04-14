package dev.frost.frostcore.cmds.admin;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.gui.GuiManager;
import dev.frost.frostcore.gui.GuiTemplate;
import dev.frost.frostcore.mace.MaceEntry;
import dev.frost.frostcore.gui.MaceGui;
import dev.frost.frostcore.manager.MaceManager;
import dev.frost.frostcore.manager.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class MaceCmd implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "frostcore.mace.admin";
    private static final String PREFIX = "<#6B8DAE>MACE <dark_gray>» ";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            msg(sender, "<#D4727A>You don't have permission to use this command.");
            return true;
        }

        MaceManager mgr = Main.getMaceManager();
        if (mgr == null) {
            msg(sender, "<#D4727A>Mace limiter is not loaded.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> handleInfo(sender, mgr);
            case "list" -> handleList(sender);
            case "inspect" -> handleInspect(sender, args, mgr);
            case "destroy" -> handleDestroy(sender, args, mgr);
            case "remove" -> handleDestroy(sender, args, mgr);
            case "reset" -> handleReset(sender, mgr);
            case "reload" -> handleReload(sender, mgr);
            case "give" -> handleGive(sender, args, mgr);
            case "settings" -> handleSettings(sender);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void handleInfo(CommandSender sender, MaceManager mgr) {
        msg(sender, "<gradient:#6B8DAE:#8BADC4><bold>Mace Limiter Info");
        msg(sender, "");
        msg(sender, "<dark_gray>  Status: " + (mgr.isEnabled() ? "<#7ECFA0>Enabled" : "<#D4727A>Disabled"));
        msg(sender, "<dark_gray>  Active Maces: <white>" + mgr.getActiveMaceCount() + "<dark_gray>/" + mgr.getMaxMacesOverall());
        msg(sender, "<dark_gray>  Per Player: <white>" + (mgr.getMaxMacesPerPlayer() <= 0 ? "Unlimited" : mgr.getMaxMacesPerPlayer()));
        msg(sender, "<dark_gray>  Recipe: " + (mgr.hasReachedGlobalLimit() ? "<#D4727A>Locked" : "<#7ECFA0>Available"));
        msg(sender, "<dark_gray>  PvP Cooldown: <white>" + (mgr.getPvpCooldownSeconds() <= 0 ? "Disabled" : String.format("%.1fs", mgr.getPvpCooldownSeconds())));
        msg(sender, "<dark_gray>  Damage Cap: <white>" + (mgr.getDamageCap() <= 0 ? "Unlimited" : String.format("%.1f", mgr.getDamageCap())));
        msg(sender, "<dark_gray>  Destroy on Death: " + (mgr.isDisableOnDeath() ? "<#D4727A>Yes" : "<#7ECFA0>No"));
        msg(sender, "");

        Map<String, Integer> limits = mgr.getEnchantmentLimits();
        if (!limits.isEmpty()) {
            msg(sender, "<dark_gray>  Enchantment Limits:");
            for (Map.Entry<String, Integer> e : limits.entrySet()) {
                String name = e.getKey().substring(0, 1) + e.getKey().substring(1).toLowerCase();
                msg(sender, "<dark_gray>    <#8FA3BF>▸ <white>" + name + " <dark_gray>max " + e.getValue());
            }
        }

        List<String> worlds = mgr.getRestrictedWorlds();
        if (!worlds.isEmpty()) {
            msg(sender, "<dark_gray>  Restricted Worlds: <white>" + String.join(", ", worlds));
        }
    }

    private void handleList(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            msg(sender, "<#D4727A>This command requires a player.");
            return;
        }
        GuiManager.schedule(() -> MaceGui.openRegistry(player));
    }

    private void handleInspect(CommandSender sender, String[] args, MaceManager mgr) {
        if (args.length < 2) {
            msg(sender, "<#D4727A>Usage: /mace inspect <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            msg(sender, "<#D4727A>Player not found.");
            return;
        }

        List<MaceEntry> playerMaces = mgr.getMacesByPlayer(target.getUniqueId());
        if (playerMaces.isEmpty()) {
            msg(sender, "<#8FA3BF>" + target.getName() + " <dark_gray>has no tracked maces.");
            return;
        }

        msg(sender, "<gradient:#6B8DAE:#8BADC4><bold>" + target.getName() + "'s Maces");
        for (MaceEntry entry : playerMaces) {
            Map<String, Integer> enchants = MaceManager.parseEnchantments(entry.enchantments());
            String enchStr = enchants.isEmpty() ? "none" : enchants.entrySet().stream()
                    .map(e -> e.getKey().toLowerCase() + " " + e.getValue())
                    .collect(Collectors.joining(", "));

            msg(sender, "<dark_gray>  <#8FA3BF>#" + entry.shortId()
                    + " <dark_gray>| Crafted: <white>" + entry.getFormattedAge() + " ago"
                    + " <dark_gray>| Enchants: <white>" + enchStr);
        }
    }

    private void handleDestroy(CommandSender sender, String[] args, MaceManager mgr) {
        if (args.length < 2) {
            msg(sender, "<#D4727A>Usage: /mace destroy <maceId>");
            return;
        }

        String inputId = args[1].replace("#", "");
        String fullId = null;
        for (MaceEntry entry : mgr.getAllActiveMaces()) {
            if (entry.maceId().startsWith(inputId) || entry.shortId().equalsIgnoreCase(inputId)) {
                fullId = entry.maceId();
                break;
            }
        }

        if (fullId == null) {
            msg(sender, "<#D4727A>Mace not found with ID: " + inputId);
            return;
        }

        boolean physicallyRemoved = mgr.physicallyRemoveMace(fullId);
        MaceEntry entry = mgr.getMaceEntry(fullId);
        String shortId = inputId.length() >= 8 ? inputId.substring(0, 8) : inputId;

        if (physicallyRemoved) {
            msg(sender, "<#7ECFA0>Mace #" + shortId + " destroyed and physically removed from the server.");
        } else {
            msg(sender, "<#D4A76A>Mace #" + shortId + " marked as destroyed. Item could not be found in any online inventory.");
        }
    }

    private void handleReset(CommandSender sender, MaceManager mgr) {
        if (!(sender instanceof Player player)) {
            mgr.resetAll();
            msg(sender, "<#7ECFA0>All mace data has been reset.");
            return;
        }

        GuiManager.schedule(() -> {
            dev.frost.frostcore.gui.GuiTemplate.confirm(
                    "<!italic><#D4727A>Reset ALL Mace Data?",
                    ctx -> {
                        ctx.close();
                        mgr.resetAll();
                        msg(player, "<#7ECFA0>All mace data has been reset.");
                    },
                    ctx -> {
                        ctx.close();
                        msg(player, "<#D4727A>Reset cancelled.");
                    }
            ).open(player);
        });
    }

    private void handleReload(CommandSender sender, MaceManager mgr) {
        mgr.reload();
        msg(sender, "<#7ECFA0>Mace limiter configuration reloaded.");
    }

    private void handleGive(CommandSender sender, String[] args, MaceManager mgr) {
        if (args.length < 2) {
            msg(sender, "<#D4727A>Usage: /mace give <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            msg(sender, "<#D4727A>Player not found.");
            return;
        }

        ItemStack mace = new ItemStack(Material.MACE);
        MaceEntry entry = mgr.registerMace(target, mace);
        target.getInventory().addItem(mace);

        msg(sender, "<#7ECFA0>Gave tracked mace #" + entry.shortId() + " to " + target.getName());
        Main.getMessageManager().sendRaw(target,
                "<#6B8DAE>MACE <dark_gray>» <#8FA3BF>You received a tracked mace <dark_gray>#" + entry.shortId());
    }

    private void handleSettings(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            msg(sender, "<#D4727A>This command requires a player.");
            return;
        }
        GuiManager.schedule(() -> MaceGui.openSettings(player));
    }

    private void sendUsage(CommandSender sender) {
        msg(sender, "<gradient:#6B8DAE:#8BADC4><bold>Mace Limiter Commands");
        msg(sender, "");
        msg(sender, "<dark_gray>  <#8FA3BF>/mace info       <dark_gray>— System status & limits");
        msg(sender, "<dark_gray>  <#8FA3BF>/mace list       <dark_gray>— Open mace registry GUI");
        msg(sender, "<dark_gray>  <#8FA3BF>/mace inspect <player> <dark_gray>— View player's maces");
        msg(sender, "<dark_gray>  <#8FA3BF>/mace destroy <id>    <dark_gray>— Destroy & remove a mace");
        msg(sender, "<dark_gray>  <#8FA3BF>/mace give <player>   <dark_gray>— Give a tracked mace");
        msg(sender, "<dark_gray>  <#8FA3BF>/mace settings   <dark_gray>— Open settings GUI");
        msg(sender, "<dark_gray>  <#8FA3BF>/mace reload     <dark_gray>— Reload config");
        msg(sender, "<dark_gray>  <#8FA3BF>/mace reset      <dark_gray>— Reset all tracking data");
    }

    private void msg(CommandSender sender, String minimsg) {
        Main.getMessageManager().sendRaw(sender, PREFIX + minimsg);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) return Collections.emptyList();

        if (args.length == 1) {
            return filter(List.of("info", "list", "inspect", "destroy", "give", "settings", "reload", "reset"), args[0]);
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "inspect", "give" -> {
                    return filter(Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName).collect(Collectors.toList()), args[1]);
                }
                case "destroy", "remove" -> {
                    MaceManager mgr = Main.getMaceManager();
                    if (mgr != null) {
                        return filter(mgr.getAllActiveMaces().stream()
                                .map(MaceEntry::shortId).collect(Collectors.toList()), args[1]);
                    }
                }
            }
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
