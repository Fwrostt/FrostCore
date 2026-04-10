package dev.frost.frostcore.utils;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.CooldownManager;
import dev.frost.frostcore.manager.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;

public class TeleportUtil {

    private final Main plugin;
    private final MessageManager mm;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public TeleportUtil(Main plugin) {
        this.plugin = plugin;
        this.mm = MessageManager.get();
    }

    /**
     * Cooldown is applied ONLY when the teleport SUCCESSFULLY completes.
     * If cancelled due to movement → no cooldown is given.
     */
    public boolean teleportWithCooldownAndDelay(Player player, Location target,
                                                String cooldownId, String cooldownPath,
                                                String cooldownMsgPath,
                                                int delaySeconds,
                                                String waitMsgPath,
                                                String successMsgPath,
                                                String cancelMsgPath) {
        boolean bypass = hasBypass(player);
        if (!bypass && CooldownManager.isOnCooldown(player, cooldownId)) {
            int remaining = CooldownManager.getRemainingTime(player, cooldownId);
            Map<String, String> ph = Map.of("time", String.valueOf(remaining));
            mm.send(player, cooldownMsgPath, ph);
            return false;
        }

        if (delaySeconds <= 0 || bypass) {
            player.teleport(target);
            mm.send(player, successMsgPath);
            playTeleportSuccessSound(player);
            return true;
        }

        startDelayedTeleport(player, target, delaySeconds, cooldownId, cooldownPath,
                waitMsgPath, successMsgPath, cancelMsgPath);

        return true;
    }

    private void startDelayedTeleport(Player player, Location target, int delaySeconds,
                                      String cooldownId, String cooldownPath,
                                      String waitMsgPath, String successMsgPath, String cancelMsgPath) {

        Location startLoc = player.getLocation().clone();

        Map<String, String> ph = Map.of("time", String.valueOf(delaySeconds));
        mm.send(player, waitMsgPath, ph);

        new CountdownTask(player, target, startLoc, delaySeconds, cooldownId, cooldownPath,
                successMsgPath, cancelMsgPath)
                .start();
    }

    private void playTeleportSuccessSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.6f, 1.2f), 4L);
    }

    private class CountdownTask {
        private final Player player;
        private final Location target;
        private final Location startLoc;
        private final String cooldownId;
        private final String cooldownPath;
        private final String successMsgPath;
        private final String cancelMsgPath;
        private int timeLeft;
        private BukkitTask task;

        public CountdownTask(Player player, Location target, Location startLoc, int delaySeconds,
                             String cooldownId, String cooldownPath,
                             String successMsgPath, String cancelMsgPath) {
            this.player = player;
            this.target = target;
            this.startLoc = startLoc;
            this.cooldownId = cooldownId;
            this.cooldownPath = cooldownPath;
            this.successMsgPath = successMsgPath;
            this.cancelMsgPath = cancelMsgPath;
            this.timeLeft = delaySeconds;
        }

        public void start() {
            task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (player.getLocation().distanceSquared(startLoc) > 1.0) {
                    mm.send(player, cancelMsgPath);
                    playCancelSound(player);
                    cancel();
                    return;
                }

                sendCountdownActionBar();
                playCountdownSound();

                if (timeLeft <= 0) {
                    applyCooldown();
                    completeTeleport();
                    cancel();
                }

                timeLeft--;
            }, 0L, 20L);
        }

        private void applyCooldown() {
            int cooldownSeconds = Main.getConfigManager().getInt(cooldownPath);
            CooldownManager.setCooldown(player, cooldownId, cooldownSeconds);
        }

        private void sendCountdownActionBar() {
            String actionBarText = "<gradient:#FFD700:#FFA500>Teleporting in <bold>" + timeLeft + "</bold>...</gradient>";
            Component component = miniMessage.deserialize(actionBarText);
            player.sendActionBar(component);
        }

        private void playCountdownSound() {
            float pitch = timeLeft <= 3 ? 1.8f : 1.4f;
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, pitch);
        }

        private void completeTeleport() {
            player.teleport(target);
            mm.send(player, successMsgPath);
            playTeleportSuccessSound(player);
        }

        private void playCancelSound(Player p) {
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }

        private void cancel() {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
    }

    private boolean hasBypass(Player player) {
        return player.isOp() ||
                player.hasPermission("frostcore.bypass.cooldown") ||
                player.hasPermission("frostcore.admin");
    }
}