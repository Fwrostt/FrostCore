package dev.frost.frostcore.cmds.moderation;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.moderation.ModerationManager;
import dev.frost.frostcore.utils.FrostLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ScreenshareCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm = Main.getMessageManager();
    private final MiniMessage mini = MiniMessage.miniMessage();

    private static final Map<UUID, String> activeSessions = new ConcurrentHashMap<>();
    private static final Map<UUID, BukkitTask> titleTasks = new ConcurrentHashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>/screenshare <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>Player not found.");
            return true;
        }

        if (!Main.getGroupLimitManager().canPunish(sender, target)) {
            mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>You cannot punish this player.");
            return true;
        }

        UUID targetUuid = target.getUniqueId();
        String staffName = sender.getName();

        if (isInScreenshare(targetUuid)) {
            endScreenshare(target);

            mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#7ECFA0>Released <white>"
                    + target.getName() + " <#8FA3BF>from screenshare.");
            mm.sendRaw(target, "<#D4727A>MOD <dark_gray>»</dark_gray> <#7ECFA0>You have been released from screenshare.");

            broadcastToStaff("<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>" + staffName
                    + " released <white>" + target.getName() + " <#8FA3BF>from screenshare.");

            FrostLogger.audit("SCREENSHARE END: " + staffName + " released " + target.getName());
            FrostLogger.info("[MOD] " + staffName + " released " + target.getName() + " from screenshare.");
        } else {
            startScreenshare(target, staffName);

            mm.sendRaw(sender, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>Screensharing <white>"
                    + target.getName() + "<#8FA3BF>. They are now frozen, blind, and invulnerable.");
            mm.sendRaw(target, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>You have been put into screenshare mode. <#8FA3BF>Do not disconnect.");

            broadcastToStaff("<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>" + staffName
                    + " put <white>" + target.getName() + " <#8FA3BF>into screenshare mode.");

            FrostLogger.audit("SCREENSHARE START: " + staffName + " → " + target.getName());
            FrostLogger.info("[MOD] " + staffName + " put " + target.getName() + " into screenshare mode.");
        }

        return true;
    }

    // ━━━━━━━━━━━━━━━━━━ Session Management ━━━━━━━━━━━━━━━━━━

    private void startScreenshare(Player target, String staffName) {
        UUID uuid = target.getUniqueId();
        ModerationManager mod = ModerationManager.getInstance();

        activeSessions.put(uuid, staffName);

        mod.setFrozen(uuid, true);

        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, PotionEffect.INFINITE_DURATION, 0, false, false, false));

        target.setInvulnerable(true);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            if (!target.isOnline()) return;

            Component title = mini.deserialize("<bold><#D4727A>SCREENSHARE</bold>");
            Component subtitle = mini.deserialize("<#8FA3BF>Do <#D4727A>NOT <#8FA3BF>disconnect");

            target.showTitle(Title.title(title, subtitle, Title.Times.times(
                    Ticks.duration(0),
                    Ticks.duration(50),
                    Ticks.duration(10)
            )));
        }, 0L, 40L);

        titleTasks.put(uuid, task);
    }

    public static void endScreenshare(Player target) {
        UUID uuid = target.getUniqueId();
        ModerationManager mod = ModerationManager.getInstance();

        activeSessions.remove(uuid);
        mod.setFrozen(uuid, false);
        target.removePotionEffect(PotionEffectType.BLINDNESS);
        target.setInvulnerable(false);
        BukkitTask task = titleTasks.remove(uuid);
        if (task != null) task.cancel();
        target.clearTitle();
    }

    public static boolean isInScreenshare(UUID uuid) {
        return activeSessions.containsKey(uuid);
    }

    public static String getScreenshareStaff(UUID uuid) {
        return activeSessions.get(uuid);
    }

    public static void handleScreenshareDisconnect(Player player) {
        UUID uuid = player.getUniqueId();
        String staffName = activeSessions.remove(uuid);
        if (staffName == null) return;
        BukkitTask task = titleTasks.remove(uuid);
        if (task != null) task.cancel();
        player.setInvulnerable(false);
        ModerationManager mod = ModerationManager.getInstance();
        mod.setFrozen(uuid, false);

        long sevenDays = 7L * 24L * 60L * 60L * 1000L; 
        mod.punish(
                dev.frost.frostcore.moderation.PunishmentType.TEMPBAN,
                uuid,
                player.getName(),
                null,
                "Screenshare Dodge — Disconnected during screenshare",
                Bukkit.getConsoleSender(),
                sevenDays,
                false
        );
        MessageManager mm = Main.getMessageManager();
        String msg = "<#D4727A>MOD <dark_gray>»</dark_gray> <white>" + player.getName()
                + " <#D4727A>disconnected during screenshare <#8FA3BF>(by " + staffName
                + "). <#D4727A>Auto-banned for 7 days.";
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("frostcore.moderation.notify")) {
                mm.sendRaw(online, msg);
            }
        }
        FrostLogger.audit("SCREENSHARE DODGE: " + player.getName() + " disconnected during screenshare by "
                + staffName + " — Auto-banned for 7 days.");
        FrostLogger.info("[MOD] " + player.getName() + " dodged screenshare (by " + staffName
                + ") — Auto-banned for 7 days.");
    }

    // ━━━━━━━━━━━━━━━━━━ Utilities ━━━━━━━━━━━━━━━━━━

    private void broadcastToStaff(String message) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("frostcore.moderation.notify")) {
                mm.sendRaw(online, message);
            }
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                                @NotNull String label, @NotNull String[] args) {
        return args.length == 1
                ? Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList())
                : Collections.emptyList();
    }
}
