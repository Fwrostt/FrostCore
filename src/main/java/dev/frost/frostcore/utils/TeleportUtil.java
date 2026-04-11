package dev.frost.frostcore.utils;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.CooldownManager;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.manager.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Map;

public class TeleportUtil {

    private final Main plugin;
    private final MessageManager mm;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    // ━━━ Progress bar characters ━━━
    private static final String BAR_FILLED = "●";
    private static final String BAR_EMPTY  = "○";
    private static final int BAR_LENGTH = 10;

    public TeleportUtil(Main plugin) {
        this.plugin = plugin;
        this.mm = MessageManager.get();
    }

    private ConfigManager config() {
        return Main.getConfigManager();
    }

    private boolean actionBarEnabled() {
        return config().getBoolean("teleport.action-bar", true);
    }

    private boolean titleEnabled() {
        return config().getBoolean("teleport.title", true);
    }

    private boolean soundEnabled() {
        return config().getBoolean("teleport.sounds", true);
    }

    private boolean particlesEnabled() {
        return config().getBoolean("teleport.particles", true);
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
        return teleportWithCooldownAndDelay(player, target, cooldownId, cooldownPath,
                cooldownMsgPath, delaySeconds, waitMsgPath, successMsgPath, cancelMsgPath, Map.of());
    }

    public boolean teleportWithCooldownAndDelay(Player player, Location target,
                                                String cooldownId, String cooldownPath,
                                                String cooldownMsgPath,
                                                int delaySeconds,
                                                String waitMsgPath,
                                                String successMsgPath,
                                                String cancelMsgPath,
                                                Map<String, String> extraPlaceholders) {
        boolean bypassCooldown = hasCooldownBypass(player);
        boolean bypassDelay = hasDelayBypass(player);
        if (!bypassCooldown && CooldownManager.isOnCooldown(player, cooldownId)) {
            int remaining = CooldownManager.getRemainingTime(player, cooldownId);
            Map<String, String> ph = Map.of("time", String.valueOf(remaining));
            mm.send(player, cooldownMsgPath, ph);
            return false;
        }

        if (delaySeconds <= 0 || bypassDelay) {
            doTeleport(player, target, successMsgPath, extraPlaceholders);
            if (!bypassCooldown) {
                int cooldownSeconds = config().getInt(cooldownPath, 0);
                if (cooldownSeconds > 0) {
                    CooldownManager.setCooldown(player, cooldownId, cooldownSeconds);
                }
            }
            return true;
        }

        startDelayedTeleport(player, target, delaySeconds, cooldownId, cooldownPath,
                waitMsgPath, successMsgPath, cancelMsgPath, extraPlaceholders, bypassCooldown);

        return true;
    }

    private void doTeleport(Player player, Location target, String successMsgPath,
                            Map<String, String> extraPlaceholders) {
        player.teleportAsync(target).thenAccept(success -> {
            if (success && player.isOnline()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    mm.send(player, successMsgPath, extraPlaceholders);

                    if (soundEnabled()) {
                        playTeleportSuccessSound(player);
                    }

                    if (particlesEnabled()) {
                        playTeleportParticles(player);
                    }

                    if (titleEnabled()) {
                        player.showTitle(Title.title(
                                Component.empty(),
                                miniMessage.deserialize("<gradient:#FFD700:#FFA500>Teleported!</gradient>"),
                                Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(400))
                        ));
                    }

                    // Clear action bar after tp
                    if (actionBarEnabled()) {
                        player.sendActionBar(Component.empty());
                    }
                });
            }
        });
    }

    private void startDelayedTeleport(Player player, Location target, int delaySeconds,
                                      String cooldownId, String cooldownPath,
                                      String waitMsgPath, String successMsgPath,
                                      String cancelMsgPath,
                                      Map<String, String> extraPlaceholders, boolean bypassCooldown) {

        // Preload chunk asynchronously for lag-less TP
        if (target.getWorld() != null) {
            target.getWorld().getChunkAtAsync(target);
        }

        Map<String, String> ph = Map.of("time", String.valueOf(delaySeconds));
        mm.send(player, waitMsgPath, ph);

        new CountdownTask(player, target, player.getLocation().clone(), delaySeconds,
                cooldownId, cooldownPath, successMsgPath, cancelMsgPath, extraPlaceholders, bypassCooldown)
                .start();
    }

    private void playTeleportSuccessSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
    }

    private void playTeleportParticles(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.PORTAL, loc, 40, 0.3, 0.8, 0.3, 0.5);
        player.getWorld().spawnParticle(Particle.END_ROD, loc, 10, 0.2, 0.5, 0.2, 0.02);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Countdown Task
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private class CountdownTask {
        private final Player player;
        private final Location target;
        private final Location startLoc;
        private final String cooldownId;
        private final String cooldownPath;
        private final String successMsgPath;
        private final String cancelMsgPath;
        private final Map<String, String> extraPlaceholders;
        private final boolean bypassCooldown;
        private final int totalTime;
        private int timeLeft;
        private BukkitTask task;
        private int ticksInCurrentSecond;

        public CountdownTask(Player player, Location target, Location startLoc, int delaySeconds,
                             String cooldownId, String cooldownPath,
                             String successMsgPath, String cancelMsgPath,
                             Map<String, String> extraPlaceholders, boolean bypassCooldown) {
            this.player = player;
            this.target = target;
            this.startLoc = startLoc;
            this.cooldownId = cooldownId;
            this.cooldownPath = cooldownPath;
            this.successMsgPath = successMsgPath;
            this.cancelMsgPath = cancelMsgPath;
            this.extraPlaceholders = extraPlaceholders;
            this.bypassCooldown = bypassCooldown;
            this.totalTime = delaySeconds;
            this.timeLeft = delaySeconds;
            this.ticksInCurrentSecond = 0;
        }

        public void start() {
            // Run every 2 ticks for smooth action bar progress
            task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                // Movement check
                if (hasMoved()) {
                    mm.send(player, cancelMsgPath);
                    if (soundEnabled()) playCancelSound();
                    if (actionBarEnabled()) player.sendActionBar(Component.empty());
                    cancel();
                    return;
                }

                // Update action bar smoothly
                if (actionBarEnabled()) {
                    sendProgressActionBar();
                }

                ticksInCurrentSecond += 2;

                // Every 20 ticks (1 second)
                if (ticksInCurrentSecond >= 20) {
                    ticksInCurrentSecond = 0;

                    if (soundEnabled()) playCountdownSound();

                    timeLeft--;

                    if (timeLeft <= 0) {
                        applyCooldown();
                        doTeleport(player, target, successMsgPath, extraPlaceholders);
                        cancel();
                    }
                }
            }, 0L, 2L); // run every 2 ticks for smooth bar
        }

        private boolean hasMoved() {
            Location current = player.getLocation();
            return current.getBlockX() != startLoc.getBlockX()
                    || current.getBlockY() != startLoc.getBlockY()
                    || current.getBlockZ() != startLoc.getBlockZ();
        }

        private void applyCooldown() {
            if (bypassCooldown) return;
            int cooldownSeconds = config().getInt(cooldownPath, 0);
            if (cooldownSeconds > 0) {
                CooldownManager.setCooldown(player, cooldownId, cooldownSeconds);
            }
        }

        private void sendProgressActionBar() {
            // Calculate smooth progress (accounts for sub-second ticks)
            double elapsed = (totalTime - timeLeft) + (ticksInCurrentSecond / 20.0);
            double progress = Math.min(elapsed / totalTime, 1.0);
            int filled = (int) (progress * BAR_LENGTH);
            int empty = BAR_LENGTH - filled;

            StringBuilder bar = new StringBuilder();
            bar.append("<#00C9FF>");
            bar.append(BAR_FILLED.repeat(filled));
            bar.append("</#00C9FF>");
            bar.append("<dark_gray>");
            bar.append(BAR_EMPTY.repeat(empty));
            bar.append("</dark_gray>");

            String color = timeLeft <= 1 ? "<#55FF55>" : timeLeft <= 2 ? "<#FFFF55>" : "<#00C9FF>";

            String text = "<!italic><bold><dark_gray>« </dark_gray>" + bar + "<dark_gray> »   </dark_gray>"
                    + color + timeLeft + "s";

            player.sendActionBar(miniMessage.deserialize(text));
        }

        private void playCountdownSound() {
            if (timeLeft <= 1) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 2.0f);
            } else if (timeLeft <= 3) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, 1.8f);
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.2f);
            }
        }

        private void playCancelSound() {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }

        private void cancel() {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
    }

    public void teleportInstant(Player player, Location target) {
        player.teleportAsync(target).thenAccept(success -> {
            if (success && player.isOnline()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (soundEnabled()) {
                        playTeleportSuccessSound(player);
                    }
                    if (particlesEnabled()) {
                        playTeleportParticles(player);
                    }
                    if (titleEnabled()) {
                        player.showTitle(Title.title(
                                Component.empty(),
                                miniMessage.deserialize("<gradient:#FFD700:#FFA500>Teleported!</gradient>"),
                                Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(400))
                        ));
                    }
                });
            }
        });
    }

    private boolean hasCooldownBypass(Player player) {
        return player.hasPermission("frostcore.bypass.cooldown") ||
               (config().getBoolean("teleport.admin-bypass", false) && player.hasPermission("frostcore.admin"));
    }

    private boolean hasDelayBypass(Player player) {
        return player.hasPermission("frostcore.bypass.delay") ||
               (config().getBoolean("teleport.admin-bypass", false) && player.hasPermission("frostcore.admin"));
    }
}