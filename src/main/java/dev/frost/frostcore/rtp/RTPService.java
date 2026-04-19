package dev.frost.frostcore.rtp;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.CooldownManager;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.utils.EconomyUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Core RTP orchestration service.
 * <p>
 * Manages the complete teleport flow:
 * validate → cooldown → async search → charge → warmup → teleport → effects.
 * <p>
 * Economy is withdrawn ONLY after a safe location is confirmed.
 * Cooldown is applied immediately on request, removed on failure/cancel.
 */
public class RTPService {

    private static final String COOLDOWN_ID = "rtp";
    private static final String BAR_FILLED = "●";
    private static final String BAR_EMPTY  = "○";
    private static final int BAR_LENGTH = 10;
    private static final int CHUNK_LOAD_RADIUS = 3;
    private static final int CHUNK_TICKET_RELEASE_TICKS = 200; // 10 seconds
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Main plugin;
    private final RTPConfig config;
    private final RTPLocationService locationService;
    private final RTPStateTracker stateTracker;
    private final MessageManager mm;

    public RTPService(Main plugin, RTPConfig config,
                      RTPLocationService locationService, RTPStateTracker stateTracker) {
        this.plugin = plugin;
        this.config = config;
        this.locationService = locationService;
        this.stateTracker = stateTracker;
        this.mm = MessageManager.get();
    }

    // ── Public API ───────────────────────────────────────────────

    /**
     * Initiates an RTP request for the given player and world.
     * Performs all validation, applies cooldown, and starts the async search.
     */
    public void requestRTP(Player player, String worldName) {
        // 1. Enabled check
        if (!config.isEnabled()) {
            mm.send(player, "rtp.disabled");
            return;
        }

        // 2. World check
        RTPConfig.WorldSettings settings = config.getWorldSettings(worldName);
        if (settings == null) {
            mm.send(player, "rtp.world-not-found", Map.of("world", worldName));
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            mm.send(player, "rtp.world-not-found", Map.of("world", worldName));
            return;
        }

        // 3. Permission check
        if (!player.hasPermission("frostcore.rtp." + worldName) && !player.hasPermission("frostcore.rtp.*")) {
            mm.send(player, "rtp.no-permission", Map.of("world", worldName));
            return;
        }

        // 4. Already pending check
        if (stateTracker.isPending(player.getUniqueId())) {
            mm.send(player, "rtp.already-pending");
            return;
        }

        // 5. Cooldown check
        boolean bypassCooldown = player.hasPermission("frostcore.rtp.bypass.cooldown");
        if (!bypassCooldown && CooldownManager.isOnCooldown(player, COOLDOWN_ID)) {
            int remaining = CooldownManager.getRemainingTime(player, COOLDOWN_ID);
            mm.send(player, "rtp.cooldown", Map.of("time", String.valueOf(remaining)));
            return;
        }

        // 6. Apply cooldown IMMEDIATELY to prevent spam
        if (!bypassCooldown && config.getCooldown() > 0) {
            CooldownManager.setCooldown(player, COOLDOWN_ID, config.getCooldown());
        }

        // 7. Mark pending
        stateTracker.markPending(player.getUniqueId());
        stateTracker.recordRequest();

        // 8. Send searching message
        mm.send(player, "rtp.searching");

        // 9. Start async location search
        long searchStart = System.currentTimeMillis();

        locationService.pollLocation(world, settings).thenAccept(location -> {
            long searchTime = System.currentTimeMillis() - searchStart;

            // Switch to main thread for all subsequent work
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Guard: player still online and still pending
                if (!player.isOnline() || !stateTracker.isPending(player.getUniqueId())) {
                    stateTracker.clearPending(player.getUniqueId());
                    return;
                }

                if (location == null) {
                    handleFailure(player, bypassCooldown, config.getMaxAttempts(), searchTime);
                    return;
                }

                // 10. Economy check + withdraw (ONLY after location confirmed)
                double cost = settings.getCost();
                boolean bypassCost = player.hasPermission("frostcore.rtp.bypass.cost");

                if (cost > 0 && !bypassCost && EconomyUtil.isEnabled()) {
                    if (!EconomyUtil.has(player, cost)) {
                        mm.send(player, "rtp.insufficient-funds", Map.of(
                            "cost", EconomyUtil.format(cost),
                            "balance", EconomyUtil.format(EconomyUtil.getBalance(player))
                        ));
                        handleFailure(player, bypassCooldown, 0, searchTime);
                        return;
                    }
                    EconomyUtil.withdraw(player, cost);
                    stateTracker.setPendingCost(player.getUniqueId(), cost);
                }

                // 11. Warmup or instant teleport
                int delay = config.getDelay();
                boolean bypassDelay = player.hasPermission("frostcore.bypass.delay");

                if (delay <= 0 || bypassDelay) {
                    executeTeleport(player, location, worldName, searchTime, null);
                } else {
                    startWarmup(player, location, delay, worldName, searchTime);
                }
            });
        });
    }

    /**
     * Cancels an active RTP request. Refunds cost and removes cooldown.
     *
     * @param player      the player
     * @param sendMessage whether to send a cancel message to the player
     */
    public void cancelRTP(Player player, boolean sendMessage) {
        if (!stateTracker.isPending(player.getUniqueId())) return;

        stateTracker.cancelWarmup(player.getUniqueId());
        stateTracker.clearPending(player.getUniqueId());

        // Refund cost if already charged
        double cost = stateTracker.getPendingCost(player.getUniqueId());
        if (cost > 0 && player.isOnline()) {
            EconomyUtil.deposit(player, cost);
            mm.send(player, "rtp.refunded", Map.of("cost", EconomyUtil.format(cost)));
        }
        stateTracker.clearPendingCost(player.getUniqueId());

        // Remove cooldown since RTP was cancelled
        CooldownManager.clearCooldown(player, COOLDOWN_ID);

        if (sendMessage && player.isOnline()) {
            mm.send(player, "rtp.cancelled");
            player.sendActionBar(Component.empty());
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    // ── Warmup countdown ─────────────────────────────────────────

    private void startWarmup(Player player, Location target, int delaySeconds,
                              String worldName, long searchTime) {
        // Start preloading a full chunk grid around destination IMMEDIATELY
        // so by the time the countdown finishes, all chunks are loaded and ready
        preloadChunks(target, CHUNK_LOAD_RADIUS);

        mm.send(player, "rtp.wait", Map.of("time", String.valueOf(delaySeconds)));

        final int[] timeLeft = {delaySeconds};
        final int[] ticksInSecond = {0};

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Guard: player still online and still pending
            if (!player.isOnline() || !stateTracker.isPending(player.getUniqueId())) {
                stateTracker.cancelWarmup(player.getUniqueId());
                return;
            }

            // Progress bar (action bar)
            if (Main.getConfigManager().getBoolean("teleport.action-bar", true)) {
                sendProgressBar(player, delaySeconds, timeLeft[0], ticksInSecond[0]);
            }

            ticksInSecond[0] += 2;

            if (ticksInSecond[0] >= 20) {
                ticksInSecond[0] = 0;

                // Countdown sounds
                if (Main.getConfigManager().getBoolean("teleport.sounds", true)) {
                    playCountdownSound(player, timeLeft[0]);
                }

                timeLeft[0]--;

                if (timeLeft[0] <= 0) {
                    stateTracker.cancelWarmup(player.getUniqueId());
                    executeTeleport(player, target, worldName, searchTime, null);
                }
            }
        }, 0L, 2L);

        stateTracker.setWarmupTask(player.getUniqueId(), task);
    }

    // ── Final teleport ───────────────────────────────────────────

    private void executeTeleport(Player player, Location target, String worldName, long searchTime, @Nullable CommandSender forceAdmin) {
        // Preload a full chunk grid and add temporary chunk tickets,
        // THEN teleport once all chunks are loaded
        preloadChunksWithTickets(target, CHUNK_LOAD_RADIUS).thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline() || !stateTracker.isPending(player.getUniqueId())) {
                    stateTracker.clearPending(player.getUniqueId());
                    releaseChunkTickets(target, CHUNK_LOAD_RADIUS);
                    return;
                }

                player.teleportAsync(target).thenAccept(success -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (success && player.isOnline()) {
                            // Prevent fall damage
                            player.setFallDistance(0);

                            // Success message
                            mm.send(player, "rtp.teleport", Map.of(
                                "x", String.valueOf(target.getBlockX()),
                                "y", String.valueOf(target.getBlockY()),
                                "z", String.valueOf(target.getBlockZ()),
                                "world", worldName
                            ));

                            // Arrival effects (reuses FrostCore's teleport effect settings)
                            playArrivalEffects(player);

                            // Update state
                            stateTracker.recordSuccess(0, searchTime);
                            stateTracker.clearPending(player.getUniqueId());
                            stateTracker.clearPendingCost(player.getUniqueId());

                            // Clear action bar
                            player.sendActionBar(Component.empty());

                            // Admin notification for force-RTP
                            if (forceAdmin != null) {
                                if (forceAdmin instanceof Player adminPlayer && adminPlayer.isOnline()) {
                                    mm.send(adminPlayer, "rtp.force-rtp-success", Map.of(
                                        "player", player.getName(),
                                        "x", String.valueOf(target.getBlockX()),
                                        "y", String.valueOf(target.getBlockY()),
                                        "z", String.valueOf(target.getBlockZ())
                                    ));
                                } else if (!(forceAdmin instanceof Player)) {
                                    forceAdmin.sendMessage("[RTP] Force-teleported " + player.getName()
                                        + " to " + target.getBlockX() + ", " + target.getBlockY()
                                        + ", " + target.getBlockZ());
                                }
                            }
                        }

                        // Release chunk tickets after a delay so client has time to render
                        Bukkit.getScheduler().runTaskLater(plugin,
                            () -> releaseChunkTickets(target, CHUNK_LOAD_RADIUS),
                            CHUNK_TICKET_RELEASE_TICKS);
                    });
                });
            });
        });
    }

    /**
     * Force-teleports a player. Bypasses cooldown, cost, and delay.
     * Used by admin commands.
     *
     * @param target    the player to teleport
     * @param worldName the target world
     * @param admin     the command sender who initiated (nullable for bulk operations)
     */
    public void forceRTP(Player target, String worldName, @Nullable CommandSender admin) {
        RTPConfig.WorldSettings settings = config.getWorldSettings(worldName);
        if (settings == null) {
            if (admin instanceof Player p) mm.send(p, "rtp.world-not-found", Map.of("world", worldName));
            else if (admin != null) admin.sendMessage("[RTP] World '" + worldName + "' is not available.");
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            if (admin instanceof Player p) mm.send(p, "rtp.world-not-found", Map.of("world", worldName));
            else if (admin != null) admin.sendMessage("[RTP] World '" + worldName + "' does not exist.");
            return;
        }

        if (stateTracker.isPending(target.getUniqueId())) {
            if (admin instanceof Player p) {
                mm.sendRaw(p, mm.getRaw("rtp.prefix") + "<#D4727A>" + target.getName() + " already has an RTP in progress.");
            }
            return;
        }

        stateTracker.markPending(target.getUniqueId());
        stateTracker.recordRequest();

        if (admin instanceof Player p) mm.send(p, "rtp.force-rtp", Map.of("player", target.getName(), "world", worldName));
        mm.send(target, "rtp.force-rtp-target");

        long searchStart = System.currentTimeMillis();
        locationService.pollLocation(world, settings).thenAccept(location -> {
            long searchTime = System.currentTimeMillis() - searchStart;

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!target.isOnline() || !stateTracker.isPending(target.getUniqueId())) {
                    stateTracker.clearPending(target.getUniqueId());
                    return;
                }

                if (location == null) {
                    stateTracker.clearPending(target.getUniqueId());
                    stateTracker.recordFailure(config.getMaxAttempts(), searchTime);
                    if (admin instanceof Player p) {
                        mm.send(p, "rtp.force-rtp-failed", Map.of("player", target.getName()));
                    } else if (admin != null) {
                        admin.sendMessage("[RTP] Failed to find a safe location for " + target.getName());
                    }
                    return;
                }

                executeTeleport(target, location, worldName, searchTime, admin);
            });
        });
    }

    // ── Chunk preloading ──────────────────────────────────────────

    /**
     * Preloads a grid of chunks around the target location.
     * Does NOT add chunk tickets — for warmup phase only.
     */
    private void preloadChunks(Location target, int radius) {
        World world = target.getWorld();
        if (world == null) return;

        int centerChunkX = target.getBlockX() >> 4;
        int centerChunkZ = target.getBlockZ() >> 4;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                world.getChunkAtAsync(centerChunkX + dx, centerChunkZ + dz);
            }
        }
    }

    /**
     * Preloads a grid of chunks AND adds plugin chunk tickets to keep them loaded.
     * Returns a future that completes when ALL chunks are loaded.
     */
    @SuppressWarnings("unchecked")
    private CompletableFuture<Void> preloadChunksWithTickets(Location target, int radius) {
        World world = target.getWorld();
        if (world == null) return CompletableFuture.completedFuture(null);

        int centerChunkX = target.getBlockX() >> 4;
        int centerChunkZ = target.getBlockZ() >> 4;

        List<CompletableFuture<Chunk>> futures = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                CompletableFuture<Chunk> f = world.getChunkAtAsync(
                    centerChunkX + dx, centerChunkZ + dz
                ).thenApply(chunk -> {
                    // Add a chunk ticket to keep this chunk loaded through the teleport
                    chunk.addPluginChunkTicket(plugin);
                    return chunk;
                });
                futures.add(f);
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Removes plugin chunk tickets from the grid around a location.
     */
    private void releaseChunkTickets(Location target, int radius) {
        World world = target.getWorld();
        if (world == null) return;

        int centerChunkX = target.getBlockX() >> 4;
        int centerChunkZ = target.getBlockZ() >> 4;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = centerChunkX + dx;
                int cz = centerChunkZ + dz;
                if (world.isChunkLoaded(cx, cz)) {
                    world.getChunkAt(cx, cz).removePluginChunkTicket(plugin);
                }
            }
        }
    }

    // ── Failure handling ─────────────────────────────────────────

    private void handleFailure(Player player, boolean bypassCooldown, int attempts, long searchTime) {
        stateTracker.clearPending(player.getUniqueId());
        stateTracker.recordFailure(attempts, searchTime);

        // Refund cost if already charged
        double cost = stateTracker.getPendingCost(player.getUniqueId());
        if (cost > 0) {
            EconomyUtil.deposit(player, cost);
            mm.send(player, "rtp.refunded", Map.of("cost", EconomyUtil.format(cost)));
        }
        stateTracker.clearPendingCost(player.getUniqueId());

        // Remove cooldown — RTP failed, don't punish the player
        if (!bypassCooldown) {
            CooldownManager.clearCooldown(player, COOLDOWN_ID);
        }

        if (player.isOnline()) {
            mm.send(player, "rtp.failed", Map.of("attempts", String.valueOf(attempts)));
        }
    }

    // ── Effects (reuses FrostCore's global teleport settings) ────

    private void playArrivalEffects(Player player) {
        var cfg = Main.getConfigManager();

        if (cfg.getBoolean("teleport.sounds", true)) {
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
        }

        if (cfg.getBoolean("teleport.particles", true)) {
            Location loc = player.getLocation().add(0, 1, 0);
            player.getWorld().spawnParticle(Particle.PORTAL, loc, 40, 0.3, 0.8, 0.3, 0.5);
            player.getWorld().spawnParticle(Particle.END_ROD, loc, 10, 0.2, 0.5, 0.2, 0.02);
        }

        if (cfg.getBoolean("teleport.title", true)) {
            player.showTitle(Title.title(
                Component.empty(),
                MM.deserialize("<gradient:#6B8DAE:#8BADC4>Teleported!"),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(400))
            ));
        }
    }

    private void sendProgressBar(Player player, int totalTime, int timeLeft, int ticksInSecond) {
        String color = timeLeft <= 1 ? "<#55FF55>" : timeLeft <= 2 ? "<#FFFF55>" : "<#00C9FF>";
        String style = dev.frost.frostcore.Main.getConfigManager().getString("teleport.action-bar-style", "BAR").toUpperCase();

        if (style.equals("TEXT")) {
            String text = "<!italic>" + "<white>" + "Teleporting in " + color + timeLeft + "s...";
            player.sendActionBar(MM.deserialize(text));
            return;
        }

        double elapsed = (totalTime - timeLeft) + (ticksInSecond / 20.0);
        double progress = Math.min(elapsed / totalTime, 1.0);
        int filled = (int) (progress * BAR_LENGTH);
        int empty = BAR_LENGTH - filled;

        String bar = "<#00C9FF>" + BAR_FILLED.repeat(filled) + "</#00C9FF>" +
                     "<dark_gray>" + BAR_EMPTY.repeat(empty) + "</dark_gray>";

        String text = "<!italic><bold><dark_gray>« </dark_gray>" + bar +
                      "<dark_gray> »   </dark_gray>" + color + timeLeft + "s";

        player.sendActionBar(MM.deserialize(text));
    }

    private void playCountdownSound(Player player, int timeLeft) {
        if (timeLeft <= 1) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 2.0f);
        } else if (timeLeft <= 3) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, 1.8f);
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.2f);
        }
    }

    // ── Accessors ────────────────────────────────────────────────

    public RTPConfig getConfig()                { return config; }
    public RTPLocationService getLocationService() { return locationService; }
    public RTPStateTracker getStateTracker()     { return stateTracker; }

    public void reload() {
        config.reload();
        locationService.clearPools();
    }

    public void shutdown() {
        stateTracker.cleanup();
        locationService.shutdown();
    }
}
