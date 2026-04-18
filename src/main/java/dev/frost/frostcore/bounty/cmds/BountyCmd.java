package dev.frost.frostcore.bounty.cmds;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.bounty.BountyManager;
import dev.frost.frostcore.bounty.BountyService;
import dev.frost.frostcore.bounty.gui.BountyDetailGui;
import dev.frost.frostcore.bounty.gui.BountyListGui;
import dev.frost.frostcore.bounty.model.Bounty;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.utils.EconomyUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles /bounty with sub-commands: place, remove, list, info, top.
 *
 * <p>All write operations are dispatched to async tasks immediately;
 * the main thread only validates, parses, and shows messages.</p>
 */
public class BountyCmd implements CommandExecutor, TabCompleter {

    private final BountyManager manager;
    private final BountyService service;
    private final MessageManager mm;

    public BountyCmd(BountyManager manager, BountyService service) {
        this.manager = manager;
        this.service = service;
        this.mm = Main.getMessageManager();
    }

    // ── Execution ──────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!guardEnabled(sender)) return true;

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        return switch (sub) {
            case "place", "add", "set" -> handlePlace(sender, args);
            case "remove", "cancel"    -> handleRemove(sender, args);
            case "list", "browse"      -> handleList(sender);
            case "info", "check"       -> handleInfo(sender, args);
            case "top", "leaderboard"  -> handleTop(sender);
            default -> {
                showHelp(sender);
                yield true;
            }
        };
    }

    // ── Sub-command handlers ───────────────────────────────────────────────────

    private boolean handlePlace(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        if (!sender.hasPermission("frostcore.bounty.place")) {
            mm.send(sender, "general.no-permission");
            return true;
        }
        if (args.length < 3) {
            mm.send(sender, "bounty.usage-place");
            return true;
        }

        String targetName = args[1];
        String amountStr  = args[2];

        double amount;
        try {
            amount = EconomyUtil.parseCompact(amountStr);
        } catch (NumberFormatException e) {
            mm.send(sender, "bounty.invalid-amount");
            return true;
        }

        if (amount <= 0) {
            mm.send(sender, "bounty.invalid-amount");
            return true;
        }

        // Resolve target offline-player
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target.getName() == null && !target.hasPlayedBefore()) {
            mm.send(sender, "admin.player-not-found");
            return true;
        }

        // Hand off to async
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(),
                () -> service.placeBounty(player, target, amount));
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostcore.bounty.remove")) {
            mm.send(sender, "general.no-permission");
            return true;
        }
        if (args.length < 2) {
            mm.send(sender, "bounty.usage-remove");
            return true;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        UUID targetUuid = target.getUniqueId();

        if (!manager.hasBounty(targetUuid)) {
            mm.send(sender, "bounty.no-bounty-on-target");
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(),
                () -> service.removeBounty(sender, targetUuid));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (!sender.hasPermission("frostcore.bounty.list")) {
            mm.send(sender, "general.no-permission");
            return true;
        }
        if (!(sender instanceof Player player)) {
            // Console: print text list
            List<Bounty> lb = manager.getLeaderboard();
            if (lb.isEmpty()) {
                sender.sendMessage("[Bounty] No active bounties.");
            } else {
                sender.sendMessage("[Bounty] Active bounties:");
                for (int i = 0; i < Math.min(10, lb.size()); i++) {
                    Bounty b = lb.get(i);
                    sender.sendMessage("  #" + (i + 1) + " " + b.getTargetName()
                            + " — " + EconomyUtil.formatCompact(b.getTotalAmount()));
                }
            }
            return true;
        }
        new BountyListGui(manager, player).open();
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostcore.bounty.list")) {
            mm.send(sender, "general.no-permission");
            return true;
        }
        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                mm.send(sender, "bounty.usage-info");
                return true;
            }
            // No arg — show own info
            Player p = (Player) sender;
            showInfo(p, p.getUniqueId(), p.getName());
            return true;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getName() == null && !target.hasPlayedBefore()) {
            mm.send(sender, "admin.player-not-found");
            return true;
        }

        Bounty b = manager.getBounty(target.getUniqueId());
        if (b == null) {
            mm.send(sender, "bounty.no-bounty-on-target");
            return true;
        }

        if (sender instanceof Player player) {
            new BountyDetailGui(manager, player, b).open();
        } else {
            sender.sendMessage("[Bounty] " + b.getTargetName() + ": "
                    + EconomyUtil.formatCompact(b.getTotalAmount())
                    + " (" + b.getContributorCount() + " contributors)");
        }
        return true;
    }

    private boolean handleTop(CommandSender sender) {
        if (!sender.hasPermission("frostcore.bounty.list")) {
            mm.send(sender, "general.no-permission");
            return true;
        }

        List<Bounty> lb = manager.getLeaderboard();
        if (lb.isEmpty()) {
            mm.send(sender, "bounty.no-active-bounties");
            return true;
        }

        mm.send(sender, "bounty.top-header");
        int limit = Math.min(10, lb.size());
        for (int i = 0; i < limit; i++) {
            Bounty b = lb.get(i);
            mm.send(sender, "bounty.top-entry",
                    Map.of("rank",   String.valueOf(i + 1),
                           "target", b.getTargetName(),
                           "amount", EconomyUtil.formatCompact(b.getTotalAmount()),
                           "contributors", String.valueOf(b.getContributorCount())));
        }
        return true;
    }

    private void showInfo(Player player, UUID targetUuid, String targetName) {
        Bounty b = manager.getBounty(targetUuid);
        if (b != null) {
            new BountyDetailGui(manager, player, b).open();
        } else {
            mm.send(player, "bounty.no-bounty-on-target");
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private boolean guardEnabled(CommandSender sender) {
        if (!EconomyUtil.isEnabled()) {
            mm.send(sender, "bounty.no-economy");
            return false;
        }
        if (!manager.isEnabled()) {
            mm.send(sender, "bounty.disabled");
            return false;
        }
        return true;
    }

    private void showHelp(CommandSender sender) {
        mm.send(sender, "bounty.help");
    }

    // ── Tab completion ─────────────────────────────────────────────────────────

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                 @NotNull Command command,
                                                 @NotNull String label,
                                                 @NotNull String[] args) {
        if (args.length == 1) {
            return filter(List.of("place", "remove", "list", "info", "top"), args[0]);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            if (sub.equals("place") || sub.equals("info") || sub.equals("remove")) {
                // Online players for real-time; cache handles offline
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && sub.equals("place")) {
            return filter(List.of("100", "500", "1k", "5k", "10k", "100k", "1m"), args[2]);
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        String lowered = input.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lowered))
                .collect(Collectors.toList());
    }
}
