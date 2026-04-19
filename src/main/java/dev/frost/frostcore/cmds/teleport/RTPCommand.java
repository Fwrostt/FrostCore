package dev.frost.frostcore.cmds.teleport;

import dev.frost.frostcore.gui.impls.RTPGui;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.rtp.RTPConfig;
import dev.frost.frostcore.rtp.RTPService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Command handler for /rtp.
 * <p>
 * Subcommands:
 * <ul>
 *   <li>{@code /rtp} — Open world-selection GUI (if enabled) or RTP directly</li>
 *   <li>{@code /rtp <world>} — RTP to specific world</li>
 *   <li>{@code /rtp reload} — Reload RTP config (admin)</li>
 *   <li>{@code /rtp cancel} — Cancel pending RTP</li>
 *   <li>{@code /rtp force <player> [world]} — Force-teleport a player (admin)</li>
 *   <li>{@code /rtp forceall [world]} — Force-teleport all online players (admin)</li>
 * </ul>
 */
public class RTPCommand implements CommandExecutor, TabCompleter {

    private final RTPService rtpService;
    private final MessageManager mm = MessageManager.get();

    public RTPCommand(RTPService rtpService) {
        this.rtpService = rtpService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                              @NotNull String label, @NotNull String[] args) {

        // ── Admin subcommands (work from console + players) ─────
        if (args.length >= 1) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "reload" -> {
                    if (!sender.hasPermission("frostcore.rtp.admin")) {
                        if (sender instanceof Player p) mm.send(p, "general.no-permission");
                        return true;
                    }
                    rtpService.reload();
                    if (sender instanceof Player p) mm.send(p, "rtp.reloaded");
                    else sender.sendMessage("[RTP] Configuration reloaded.");
                    return true;
                }
                case "force" -> {
                    handleForce(sender, args);
                    return true;
                }
                case "forceall" -> {
                    handleForceAll(sender, args);
                    return true;
                }
            }
        }

        // ── Player-only commands below ──────────────────────────
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Usage: /rtp <reload|force|forceall>");
            return true;
        }

        if (args.length == 0) {
            handleDefaultRTP(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "cancel" -> {
                if (!rtpService.getStateTracker().isPending(player.getUniqueId())) {
                    mm.sendRaw(player, mm.getRaw("rtp.prefix") + "<#8FA3BF>You don't have an active RTP to cancel.");
                    return true;
                }
                rtpService.cancelRTP(player, true);
            }
            default -> {
                // Treat as world name
                rtpService.requestRTP(player, sub);
            }
        }
        return true;
    }

    /**
     * Handles /rtp with no arguments.
     * Opens the world-selection GUI if gui-enabled is true in config.
     * Otherwise, teleports directly in the current or first configured world.
     */
    private void handleDefaultRTP(Player player) {
        Set<String> worlds = rtpService.getConfig().getEnabledWorlds();

        if (worlds.isEmpty()) {
            mm.send(player, "rtp.disabled");
            return;
        }

        if (rtpService.getConfig().isGuiEnabled()) {
            new RTPGui(player, rtpService).open(player);
        } else {
            // GUI disabled — RTP directly in current world or first configured
            String currentWorld = player.getWorld().getName();
            if (worlds.contains(currentWorld)) {
                rtpService.requestRTP(player, currentWorld);
            } else {
                rtpService.requestRTP(player, worlds.iterator().next());
            }
        }
    }

    // ── Admin: /rtp force <player> [world] ──────────────────────

    private void handleForce(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostcore.rtp.admin")) {
            if (sender instanceof Player p) mm.send(p, "general.no-permission");
            return;
        }

        if (args.length < 2) {
            if (sender instanceof Player p) {
                mm.sendRaw(p, mm.getRaw("rtp.prefix") + "<#8FA3BF>Usage: <white>/rtp force <player> [world]");
            } else {
                sender.sendMessage("[RTP] Usage: /rtp force <player> [world]");
            }
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            if (sender instanceof Player p) mm.send(p, "rtp.player-not-found");
            else sender.sendMessage("[RTP] Player not found.");
            return;
        }

        String worldName = resolveWorld(sender, target, args, 2);
        if (worldName == null) return;

        rtpService.forceRTP(target, worldName, sender);
    }

    // ── Admin: /rtp forceall [world] ────────────────────────────

    private void handleForceAll(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostcore.rtp.admin")) {
            if (sender instanceof Player p) mm.send(p, "general.no-permission");
            return;
        }

        Set<String> enabledWorlds = rtpService.getConfig().getEnabledWorlds();
        if (enabledWorlds.isEmpty()) {
            if (sender instanceof Player p) mm.send(p, "rtp.disabled");
            else sender.sendMessage("[RTP] No RTP worlds configured.");
            return;
        }

        String specifiedWorld = args.length >= 2 ? args[1] : null;
        if (specifiedWorld != null && !enabledWorlds.contains(specifiedWorld)) {
            if (sender instanceof Player p) mm.send(p, "rtp.world-not-found", Map.of("world", specifiedWorld));
            else sender.sendMessage("[RTP] World '" + specifiedWorld + "' is not available for RTP.");
            return;
        }

        int count = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (sender instanceof Player p && target.equals(p)) continue;
            if (rtpService.getStateTracker().isPending(target.getUniqueId())) continue;

            String worldName = specifiedWorld;
            if (worldName == null) {
                String current = target.getWorld().getName();
                worldName = enabledWorlds.contains(current) ? current : enabledWorlds.iterator().next();
            }

            rtpService.forceRTP(target, worldName, null);
            count++;
        }

        if (sender instanceof Player p) {
            mm.send(p, "rtp.force-all", Map.of("count", String.valueOf(count)));
        } else {
            sender.sendMessage("[RTP] Force-teleporting " + count + " players...");
        }
    }

    /**
     * Resolves the target world from args or the player's current world.
     * Returns null if the specified world is not enabled (sends error message).
     */
    private @Nullable String resolveWorld(CommandSender sender, Player target, String[] args, int argIndex) {
        Set<String> enabled = rtpService.getConfig().getEnabledWorlds();

        if (args.length > argIndex) {
            String world = args[argIndex];
            if (!enabled.contains(world)) {
                if (sender instanceof Player p) mm.send(p, "rtp.world-not-found", Map.of("world", world));
                else sender.sendMessage("[RTP] World '" + world + "' is not available for RTP.");
                return null;
            }
            return world;
        }

        // No world specified — use target's current world or first configured
        String current = target.getWorld().getName();
        return enabled.contains(current) ? current : enabled.iterator().next();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                  @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        switch (args.length) {
            case 1 -> {
                String partial = args[0].toLowerCase();
                // World names
                for (String world : rtpService.getConfig().getEnabledWorlds()) {
                    if (world.toLowerCase().startsWith(partial)) completions.add(world);
                }
                // Subcommands
                if ("cancel".startsWith(partial)) completions.add("cancel");
                if (sender.hasPermission("frostcore.rtp.admin")) {
                    if ("reload".startsWith(partial)) completions.add("reload");
                    if ("force".startsWith(partial)) completions.add("force");
                    if ("forceall".startsWith(partial)) completions.add("forceall");
                }
            }
            case 2 -> {
                String partial = args[1].toLowerCase();
                if (args[0].equalsIgnoreCase("force") && sender.hasPermission("frostcore.rtp.admin")) {
                    // Online player names
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (online.getName().toLowerCase().startsWith(partial)) {
                            completions.add(online.getName());
                        }
                    }
                }
                if (args[0].equalsIgnoreCase("forceall") && sender.hasPermission("frostcore.rtp.admin")) {
                    // World names
                    for (String world : rtpService.getConfig().getEnabledWorlds()) {
                        if (world.toLowerCase().startsWith(partial)) completions.add(world);
                    }
                }
            }
            case 3 -> {
                String partial = args[2].toLowerCase();
                if (args[0].equalsIgnoreCase("force") && sender.hasPermission("frostcore.rtp.admin")) {
                    // World names for /rtp force <player> [world]
                    for (String world : rtpService.getConfig().getEnabledWorlds()) {
                        if (world.toLowerCase().startsWith(partial)) completions.add(world);
                    }
                }
            }
        }

        Collections.sort(completions);
        return completions;
    }
}
