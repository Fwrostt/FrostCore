package dev.frost.frostcore.cmds;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.gui.impls.InvseeGui;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.PunishmentManager;
import dev.frost.frostcore.manager.VanishManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class AdminExtraCmds implements CommandExecutor, TabCompleter {

    private final MessageManager mm = Main.getMessageManager();
    private final MiniMessage mini = MiniMessage.miniMessage();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String cmd = label.toLowerCase();

        switch (cmd) {
            case "invsee" -> handleInvsee(sender, args);
            case "enderchest", "ec" -> handleEnderchest(sender, args);
            case "hat" -> handleHat(sender);
            case "whois" -> handleWhois(sender, args);
            case "seen" -> handleSeen(sender, args);
            case "smite" -> handleSmite(sender, args);
            case "vanish", "v" -> handleVanish(sender);
            case "kick" -> handleKick(sender, args);
            case "ban" -> handleBan(sender, args);
            case "unban" -> handleUnban(sender, args);
            case "warn" -> handleWarn(sender, args);
            case "tpall" -> handleTpall(sender);
            case "ping" -> handlePing(sender, args);
            case "skull" -> handleSkull(sender, args);
            case "socialspy" -> handleSocialSpy(sender);
            case "ram" -> handleRam(sender);
        }

        return true;
    }

    // ━━━ INVSEE++ ━━━

    private void handleInvsee(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return;
        }
        if (!player.hasPermission("frostcore.admin.invsee")) {
            mm.send(player, "general.no-permission");
            return;
        }
        if (args.length < 1) {
            mm.sendRaw(player, "<#FF5555>Usage: /invsee <player>");
            return;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            mm.send(player, "admin.player-not-found");
            return;
        }
        if (target.equals(player)) {
            mm.send(player, "admin.invsee-self");
            return;
        }
        new InvseeGui(target).open(player);
    }

    // ━━━ ENDERCHEST ━━━

    private void handleEnderchest(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return;
        }
        if (!player.hasPermission("frostcore.admin.enderchest")) {
            mm.send(player, "general.no-permission");
            return;
        }

        if (args.length == 0) {
            player.openInventory(player.getEnderChest());
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target != null) {
            player.openInventory(target.getEnderChest());
        } else {
            mm.send(player, "admin.player-not-found");
        }
    }

    // ━━━ HAT ━━━

    private void handleHat(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return;
        }
        if (!player.hasPermission("frostcore.admin.hat")) {
            mm.send(player, "general.no-permission");
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            mm.send(player, "utilities.no-item-held");
            return;
        }

        ItemStack currentHelmet = player.getInventory().getHelmet();
        player.getInventory().setHelmet(hand.clone());
        player.getInventory().setItemInMainHand(currentHelmet != null ? currentHelmet : new ItemStack(Material.AIR));
        mm.send(player, "admin.hat-success");
    }

    // ━━━ KICK ━━━

    private void handleKick(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostcore.moderation.kick")) {
            mm.send(sender, "general.no-permission");
            return;
        }
        if (args.length < 1) {
            mm.sendRaw(sender, "<#FF5555>Usage: /kick <player> [reason]");
            return;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            mm.send(sender, "admin.player-not-found");
            return;
        }
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Kicked by an administrator";
        target.kick(mini.deserialize("\n<gradient:#FF5555:#FF55FF><bold>KICKED</bold></gradient>\n\n<#B0C4FF>" + reason + "\n"));
        mm.send(sender, "moderation.kick-success", Map.of("player", target.getName(), "reason", reason));
    }

    // ━━━ BAN / UNBAN ━━━

    private void handleBan(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostcore.moderation.ban")) {
            mm.send(sender, "general.no-permission");
            return;
        }
        if (args.length < 1) {
            mm.sendRaw(sender, "<#FF5555>Usage: /ban <player> [time] [reason]");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        long duration = -1;
        String timeStr = "Permanent";
        String reason = "Banned by an administrator";
        int reasonStart = 1;

        if (args.length > 1) {
            long parsed = PunishmentManager.getInstance().parseTime(args[1]);
            if (parsed != -1) {
                duration = parsed;
                timeStr = args[1];
                reasonStart = 2;
            }
        }

        if (args.length > reasonStart) {
            reason = String.join(" ", Arrays.copyOfRange(args, reasonStart, args.length));
        }

        long expireAt = duration == -1 ? -1 : System.currentTimeMillis() + duration;
        PunishmentManager.getInstance().ban(target.getUniqueId(), expireAt, reason);

        // Kick if online
        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null) {
            String finalReason = reason;
            String finalTime = timeStr;
            onlineTarget.kick(mini.deserialize("\n<gradient:#FF5555:#FF55FF><bold>BANNED</bold></gradient>\n\n<#B0C4FF>Reason: <white>" + finalReason + "\n<#B0C4FF>Duration: <white>" + finalTime + "\n"));
        }

        mm.send(sender, "moderation.ban-success", Map.of("player", target.getName() != null ? target.getName() : args[0], "time", timeStr));
    }

    private void handleUnban(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostcore.moderation.ban")) {
            mm.send(sender, "general.no-permission");
            return;
        }
        if (args.length < 1) {
            mm.sendRaw(sender, "<#FF5555>Usage: /unban <player>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        PunishmentManager.getInstance().unban(target.getUniqueId());
        mm.send(sender, "moderation.unban-success", Map.of("player", target.getName() != null ? target.getName() : args[0]));
    }

    // ━━━ WARN ━━━

    private void handleWarn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostcore.moderation.warn")) {
            mm.send(sender, "general.no-permission");
            return;
        }
        if (args.length < 1) {
            mm.sendRaw(sender, "<#FF5555>Usage: /warn <player> [reason]");
            return;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            mm.send(sender, "admin.player-not-found");
            return;
        }

        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "No reason specified";

        // Send warning to target with premium formatting
        target.sendMessage(Component.empty());
        target.sendMessage(mini.deserialize("  <gradient:#FF5555:#FFAA00><bold>⚠ WARNING</bold></gradient>"));
        target.sendMessage(mini.deserialize("  <#B0C4FF>Reason: <white>" + reason));
        target.sendMessage(mini.deserialize("  <dark_gray>Warned by " + sender.getName()));
        target.sendMessage(Component.empty());
        target.playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);

        mm.send(sender, "moderation.warn-success", Map.of("player", target.getName(), "reason", reason));
    }

    // ━━━ PREMIUM WHOIS ━━━

    private void handleWhois(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostcore.admin.whois")) {
            mm.send(sender, "general.no-permission");
            return;
        }
        if (args.length < 1) {
            mm.sendRaw(sender, "<#FF5555>Usage: /whois <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target != null) {
            sendOnlineWhois(sender, target);
        } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(args[0]);
            if (offline.hasPlayedBefore()) {
                sendOfflineWhois(sender, offline);
            } else {
                mm.send(sender, "admin.player-not-found");
            }
        }
    }

    private void sendOnlineWhois(CommandSender sender, Player target) {
        String bar = "<dark_gray><strikethrough>                                                  </strikethrough>";

        sender.sendMessage(mini.deserialize(bar));
        sender.sendMessage(mini.deserialize("  <gradient:#FF5555:#FF55FF><bold>PLAYER INFO</bold></gradient> <dark_gray>» <white>" + target.getName()));
        sender.sendMessage(mini.deserialize(bar));

        sender.sendMessage(mini.deserialize("  <#6BA3E3>⏺ Status:       <#55FF55>Online"));
        if (VanishManager.getInstance().isVanished(target.getUniqueId())) {
            sender.sendMessage(mini.deserialize("  <#6BA3E3>👻 Vanished:     <#FFAA00>Yes"));
        }

        String healthBar = buildBar(target.getHealth(), target.getMaxHealth(), "<#55FF55>", "<#FF5555>");
        sender.sendMessage(mini.deserialize("  <#6BA3E3>❤ Health:       " + healthBar + " <white>" + String.format("%.0f", target.getHealth()) + "/" + String.format("%.0f", target.getMaxHealth())));

        String foodBar = buildBar(target.getFoodLevel(), 20, "<#FFAA00>", "<dark_gray>");
        sender.sendMessage(mini.deserialize("  <#6BA3E3>🍖 Food:         " + foodBar + " <white>" + target.getFoodLevel() + "/20"));

        sender.sendMessage(mini.deserialize("  <#6BA3E3>⚡ XP Level:     <white>" + target.getLevel()));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>🎮 Gamemode:     <white>" + capitalize(target.getGameMode().name())));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>✈ Flying:       <white>" + (target.isFlying() ? "<#55FF55>Yes" : "<#FF5555>No")));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>🛡 God Mode:     <white>" + (target.isInvulnerable() ? "<#55FF55>Yes" : "<#FF5555>No")));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>⭐ Op:           <white>" + (target.isOp() ? "<#55FF55>Yes" : "<#FF5555>No")));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>🌍 World:        <white>" + target.getWorld().getName()));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>📍 Location:     <white>" + target.getLocation().getBlockX() + ", " + target.getLocation().getBlockY() + ", " + target.getLocation().getBlockZ()));

        String ip = target.getAddress() != null ? target.getAddress().getHostString() : "Unknown";
        sender.sendMessage(mini.deserialize("  <#6BA3E3>🌐 IP:           <white>" + ip));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>📶 Ping:         <white>" + target.getPing() + "ms"));

        try {
            long playtimeTicks = target.getStatistic(Statistic.PLAY_ONE_MINUTE);
            sender.sendMessage(mini.deserialize("  <#6BA3E3>⏰ Playtime:     <white>" + formatPlaytime(playtimeTicks / 20)));
        } catch (Exception ignored) {}

        sender.sendMessage(mini.deserialize("  <#6BA3E3>📅 First Join:   <white>" + DATE_FORMAT.format(new Date(target.getFirstPlayed()))));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>🔑 UUID:         <dark_gray>" + target.getUniqueId()));
        sender.sendMessage(mini.deserialize(bar));
    }

    private void sendOfflineWhois(CommandSender sender, OfflinePlayer target) {
        String bar = "<dark_gray><strikethrough>                                                  </strikethrough>";
        String name = target.getName() != null ? target.getName() : "Unknown";

        sender.sendMessage(mini.deserialize(bar));
        sender.sendMessage(mini.deserialize("  <gradient:#FF5555:#FF55FF><bold>PLAYER INFO</bold></gradient> <dark_gray>» <white>" + name));
        sender.sendMessage(mini.deserialize(bar));

        sender.sendMessage(mini.deserialize("  <#6BA3E3>⏺ Status:       <#FF5555>Offline"));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>⭐ Op:           <white>" + (target.isOp() ? "<#55FF55>Yes" : "<#FF5555>No")));

        if (PunishmentManager.getInstance().isBanned(target.getUniqueId())) {
            sender.sendMessage(mini.deserialize("  <#6BA3E3>🔨 Banned:       <#FF5555>Yes"));
        }

        sender.sendMessage(mini.deserialize("  <#6BA3E3>📅 First Join:   <white>" + DATE_FORMAT.format(new Date(target.getFirstPlayed()))));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>📅 Last Seen:    <white>" + DATE_FORMAT.format(new Date(target.getLastLogin()))));

        long timeSince = System.currentTimeMillis() - target.getLastLogin();
        sender.sendMessage(mini.deserialize("  <#6BA3E3>⏱ Ago:           <white>" + formatPlaytime(timeSince / 1000)));

        try {
            long playtimeTicks = target.getStatistic(Statistic.PLAY_ONE_MINUTE);
            sender.sendMessage(mini.deserialize("  <#6BA3E3>⏰ Playtime:     <white>" + formatPlaytime(playtimeTicks / 20)));
        } catch (Exception ignored) {}

        sender.sendMessage(mini.deserialize("  <#6BA3E3>🔑 UUID:         <dark_gray>" + target.getUniqueId()));
        sender.sendMessage(mini.deserialize(bar));
    }

    // ━━━ PREMIUM SEEN ━━━

    private void handleSeen(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostcore.admin.whois")) {
            mm.send(sender, "general.no-permission");
            return;
        }
        if (args.length < 1) {
            mm.sendRaw(sender, "<#FF5555>Usage: /seen <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target != null) {
            sendOnlineSeen(sender, target);
        } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(args[0]);
            if (offline.hasPlayedBefore()) {
                sendOfflineSeen(sender, offline);
            } else {
                mm.send(sender, "admin.player-not-found");
            }
        }
    }

    private void sendOnlineSeen(CommandSender sender, Player target) {
        String bar = "<dark_gray><strikethrough>                                        </strikethrough>";

        sender.sendMessage(mini.deserialize(bar));
        sender.sendMessage(mini.deserialize("  <gradient:#55FF55:#00C9FF><bold>ONLINE</bold></gradient> <dark_gray>» <white>" + target.getName()));
        sender.sendMessage(mini.deserialize(bar));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>⏺ Status:     <#55FF55>Currently Online"));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>🌍 World:      <white>" + target.getWorld().getName()));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>📍 Location:   <white>" + target.getLocation().getBlockX() + ", " + target.getLocation().getBlockY() + ", " + target.getLocation().getBlockZ()));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>🎮 Gamemode:   <white>" + capitalize(target.getGameMode().name())));

        try {
            long playtimeTicks = target.getStatistic(Statistic.PLAY_ONE_MINUTE);
            sender.sendMessage(mini.deserialize("  <#6BA3E3>⏰ Playtime:   <white>" + formatPlaytime(playtimeTicks / 20)));
        } catch (Exception ignored) {}

        sender.sendMessage(mini.deserialize("  <#6BA3E3>📅 First Join: <white>" + DATE_FORMAT.format(new Date(target.getFirstPlayed()))));
        sender.sendMessage(mini.deserialize(bar));
    }

    private void sendOfflineSeen(CommandSender sender, OfflinePlayer target) {
        String bar = "<dark_gray><strikethrough>                                        </strikethrough>";
        String name = target.getName() != null ? target.getName() : "Unknown";

        long timeSince = System.currentTimeMillis() - target.getLastLogin();

        sender.sendMessage(mini.deserialize(bar));
        sender.sendMessage(mini.deserialize("  <gradient:#FF5555:#FF55FF><bold>OFFLINE</bold></gradient> <dark_gray>» <white>" + name));
        sender.sendMessage(mini.deserialize(bar));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>⏺ Status:     <#FF5555>Offline"));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>📅 Last Seen:  <white>" + DATE_FORMAT.format(new Date(target.getLastLogin()))));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>⏱ Time Ago:   <white>" + formatPlaytime(timeSince / 1000)));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>📅 First Join: <white>" + DATE_FORMAT.format(new Date(target.getFirstPlayed()))));

        try {
            long playtimeTicks = target.getStatistic(Statistic.PLAY_ONE_MINUTE);
            sender.sendMessage(mini.deserialize("  <#6BA3E3>⏰ Playtime:   <white>" + formatPlaytime(playtimeTicks / 20)));
        } catch (Exception ignored) {}

        sender.sendMessage(mini.deserialize(bar));
    }

    // ━━━ SMITE ━━━

    private void handleSmite(CommandSender sender, String[] args) {
        if (!sender.hasPermission("frostcore.admin.smite")) {
            mm.send(sender, "general.no-permission");
            return;
        }
        if (args.length < 1) {
            mm.sendRaw(sender, "<#FF5555>Usage: /smite <player>");
            return;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            mm.send(sender, "admin.player-not-found");
            return;
        }
        target.getWorld().strikeLightningEffect(target.getLocation());
        mm.send(sender, "admin.smite-success", Map.of("player", target.getName()));
    }

    // ━━━ VANISH ━━━

    private void handleVanish(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return;
        }
        if (!player.hasPermission("frostcore.admin.vanish")) {
            mm.send(player, "general.no-permission");
            return;
        }

        boolean vanished = VanishManager.getInstance().toggle(player);
        if (vanished) {
            mm.send(player, "admin.vanish-on");
        } else {
            mm.send(player, "admin.vanish-off");
        }
    }

    // ━━━ TPALL ━━━

    private void handleTpall(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return;
        }
        if (!player.hasPermission("frostcore.admin.tpall")) {
            mm.send(player, "general.no-permission");
            return;
        }

        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(player)) {
                p.teleport(player.getLocation());
                mm.sendRaw(p, "<gradient:#55CDFC:#7B68EE>TELEPORT</gradient> <#AAAAAA>» <#B0C4FF>You were teleported to <white>" + player.getName() + "</white>.");
                count++;
            }
        }
        mm.send(sender, "admin.tpall-success", Map.of("count", String.valueOf(count)));
    }

    // ━━━ PING ━━━

    private void handlePing(CommandSender sender, String[] args) {
        if (args.length > 0 && sender.hasPermission("frostcore.admin.ping.others")) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                mm.send(sender, "admin.player-not-found");
                return;
            }
            int ping = target.getPing();
            String color = ping < 50 ? "<#55FF55>" : ping < 100 ? "<#FFAA00>" : "<#FF5555>";
            mm.sendRaw(sender, "<gradient:#6BA3E3:#4979C7>FROST</gradient> <#AAAAAA>» <#B0C4FF>" + target.getName() + "'s ping: " + color + ping + "ms");
        } else if (sender instanceof Player player) {
            int ping = player.getPing();
            String color = ping < 50 ? "<#55FF55>" : ping < 100 ? "<#FFAA00>" : "<#FF5555>";
            mm.sendRaw(player, "<gradient:#6BA3E3:#4979C7>FROST</gradient> <#AAAAAA>» <#B0C4FF>Your ping: " + color + ping + "ms");
        }
    }

    // ━━━ SKULL ━━━

    private void handleSkull(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return;
        }
        if (!player.hasPermission("frostcore.admin.skull")) {
            mm.send(player, "general.no-permission");
            return;
        }
        if (args.length < 1) {
            mm.sendRaw(player, "<#FF5555>Usage: /skull <player>");
            return;
        }

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(args[0]));
        meta.displayName(mini.deserialize("<white>" + args[0] + "'s Head"));
        skull.setItemMeta(meta);
        player.getInventory().addItem(skull);
        mm.sendRaw(player, "<gradient:#6BA3E3:#4979C7>FROST</gradient> <#AAAAAA>» <#B0C4FF>You received <white>" + args[0] + "</white>'s skull.");
    }

    // ━━━ SOCIALSPY ━━━

    private void handleSocialSpy(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return;
        }
        if (!player.hasPermission("frostcore.admin.socialspy")) {
            mm.send(player, "general.no-permission");
            return;
        }

        boolean enabled = Main.getPrivateMessageManager().toggleSocialSpy(player.getUniqueId());
        if (enabled) {
            mm.sendRaw(player, "<gradient:#FF5555:#FF55FF>ADMIN</gradient> <#AAAAAA>» <#B0C4FF>SocialSpy <#55FF55><bold>ENABLED</bold></#55FF55>. You can now see all private messages.");
        } else {
            mm.sendRaw(player, "<gradient:#FF5555:#FF55FF>ADMIN</gradient> <#AAAAAA>» <#B0C4FF>SocialSpy <#FF5555><bold>DISABLED</bold></#FF5555>.");
        }
    }

    // ━━━ RAM / SERVER INFO ━━━

    private void handleRam(CommandSender sender) {
        if (!sender.hasPermission("frostcore.admin.ram")) {
            mm.send(sender, "general.no-permission");
            return;
        }

        Runtime runtime = Runtime.getRuntime();
        long maxMem = runtime.maxMemory() / 1024 / 1024;
        long totalMem = runtime.totalMemory() / 1024 / 1024;
        long freeMem = runtime.freeMemory() / 1024 / 1024;
        long usedMem = totalMem - freeMem;
        double usagePercent = (double) usedMem / maxMem * 100;

        String bar = "<dark_gray><strikethrough>                                        </strikethrough>";
        String memBar = buildBar(usedMem, maxMem, usagePercent < 75 ? "<#55FF55>" : "<#FF5555>", "<dark_gray>");

        sender.sendMessage(mini.deserialize(bar));
        sender.sendMessage(mini.deserialize("  <gradient:#6BA3E3:#4979C7><bold>SERVER INFO</bold></gradient>"));
        sender.sendMessage(mini.deserialize(bar));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>💾 Memory:      " + memBar + " <white>" + usedMem + "/" + maxMem + " MB"));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>👥 Online:      <white>" + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers()));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>🌍 Worlds:      <white>" + Bukkit.getWorlds().size()));

        double[] tps = Bukkit.getTPS();
        String tpsColor = tps[0] >= 19.5 ? "<#55FF55>" : tps[0] >= 15 ? "<#FFAA00>" : "<#FF5555>";
        sender.sendMessage(mini.deserialize("  <#6BA3E3>⚡ TPS:          " + tpsColor + String.format("%.1f", tps[0]) + "<dark_gray>, " + String.format("%.1f", tps[1]) + ", " + String.format("%.1f", tps[2])));

        sender.sendMessage(mini.deserialize("  <#6BA3E3>☕ Java:         <white>" + System.getProperty("java.version")));
        sender.sendMessage(mini.deserialize("  <#6BA3E3>📦 Version:     <white>" + Bukkit.getVersion()));
        sender.sendMessage(mini.deserialize(bar));
    }

    // ━━━ TAB COMPLETE ━━━

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String cmd = label.toLowerCase();
        if (args.length == 1) {
            if (cmd.equals("hat") || cmd.equals("vanish") || cmd.equals("v") || cmd.equals("tpall")
                    || cmd.equals("socialspy") || cmd.equals("ram")) {
                return Collections.emptyList();
            }
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    // ━━━ UTILITIES ━━━

    private String buildBar(double current, double max, String filledColor, String emptyColor) {
        int barLength = 10;
        int filled = (int) Math.round((current / max) * barLength);
        int empty = barLength - filled;
        return filledColor + "█".repeat(Math.max(0, filled)) + emptyColor + "█".repeat(Math.max(0, empty));
    }

    private String formatPlaytime(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        if (days > 0) return days + "d " + hours + "h " + minutes + "m";
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    private String capitalize(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}
