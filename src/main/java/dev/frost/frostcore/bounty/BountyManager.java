package dev.frost.frostcore.bounty;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.bounty.model.Bounty;
import dev.frost.frostcore.bounty.model.BountyContributor;
import dev.frost.frostcore.utils.FrostLogger;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central cache and lifecycle manager for the bounty system.
 *
 * <p>The primary cache is a {@link ConcurrentHashMap} keyed by target UUID.
 * A sorted leaderboard snapshot is rebuilt whenever the primary cache changes
 * and is served to the GUI / commands without any DB hits.</p>
 *
 * <p>Thread-safety model: mutations of the primary cache happen on async threads
 * via {@link BountyService}. The sorted leaderboard is a volatile reference to a
 * lightweight immutable snapshot rebuilt after each mutation.</p>
 */
public class BountyManager {

    private static BountyManager instance;

    private final BountyRepository repository;

    /** Primary in-memory store — keyed by target UUID. */
    private final ConcurrentHashMap<UUID, Bounty> cache = new ConcurrentHashMap<>();

    /** Sorted snapshot rebuilt after every cache change. Served from memory. */
    private volatile List<Bounty> leaderboard = List.of();

    private boolean enabled = false;

    public BountyManager(BountyRepository repository) {
        this.repository = repository;
        instance = this;
    }

    public static BountyManager getInstance() {
        return instance;
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    /** Called from Main.setupClasses() — loads all active bounties asynchronously. */
    public void loadAsync() {
        enabled = isConfigEnabled();
        if (!enabled) {
            FrostLogger.info("[Bounty] System disabled via config.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            repository.createTables();
            List<Bounty> active = repository.loadActiveBounties();
            for (Bounty b : active) {
                cache.put(b.getTargetUuid(), b);
            }
            rebuildLeaderboard();
            FrostLogger.info("[Bounty] Loaded " + active.size() + " active bounties.");

            // Schedule expiry + GUI cache refresh every minute
            scheduleExpiryTask();
        });
    }

    private void scheduleExpiryTask() {
        long intervalTicks = 20L * 60; // every minute
        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), () -> {
            if (!enabled) return;
            long now = System.currentTimeMillis();
            for (Bounty b : cache.values()) {
                if (b.isExpired() && b.tryExpireAtomically()) {
                    cache.remove(b.getTargetUuid());
                    repository.markExpired(b.getTargetUuid());
                    rebuildLeaderboard();

                    // Partial refund if configured
                    if (Main.getConfigManager().getBoolean("bounty.refund-on-expire", false)) {
                        refundContributors(b);
                    }
                    FrostLogger.info("[Bounty] Bounty on " + b.getTargetName() + " expired.");
                }
            }
        }, intervalTicks, intervalTicks);
    }

    /** Called from Main.onDisable() to flush any remaining state. */
    public void shutdown() {
        if (!enabled) return;
        // All mutations are written through immediately; nothing extra needed.
        FrostLogger.info("[Bounty] Shutdown complete (" + cache.size() + " active bounties remain).");
    }

    // ── Cache accessors ────────────────────────────────────────────────────────

    public boolean isEnabled() {
        return enabled;
    }

    public Bounty getBounty(UUID targetUuid) {
        return cache.get(targetUuid);
    }

    public boolean hasBounty(UUID targetUuid) {
        return cache.containsKey(targetUuid);
    }

    /** All active bounties, sorted by total amount descending. Served from memory. */
    public List<Bounty> getLeaderboard() {
        return leaderboard;
    }

    public int getActiveBountyCount() {
        return cache.size();
    }

    // ── Mutations (called from BountyService, async thread) ────────────────────

    public void putBounty(Bounty bounty) {
        cache.put(bounty.getTargetUuid(), bounty);
        rebuildLeaderboard();
    }

    public void removeBounty(UUID targetUuid) {
        cache.remove(targetUuid);
        rebuildLeaderboard();
    }

    // ── Internal helpers ───────────────────────────────────────────────────────

    private void rebuildLeaderboard() {
        List<Bounty> sorted = new ArrayList<>(cache.values());
        sorted.sort((a, b) -> Double.compare(b.getTotalAmount(), a.getTotalAmount()));
        leaderboard = List.copyOf(sorted);
    }

    private boolean isConfigEnabled() {
        return Main.getConfigManager().getBoolean("bounty.enabled", true);
    }

    /** Refunds all contributors of an expired/removed bounty, async-safe. */
    public void refundContributors(Bounty bounty) {
        double refundPct = Main.getConfigManager().getDouble("bounty.refund-percentage", 100.0) / 100.0;
        for (BountyContributor c : bounty.getContributors()) {
            double refund = c.getAmount() * refundPct;
            if (refund <= 0) continue;
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(c.getContributorUuid());
            dev.frost.frostcore.utils.EconomyUtil.deposit(op, refund);

            // Notify if online
            org.bukkit.entity.Player online = Bukkit.getPlayer(c.getContributorUuid());
            if (online != null) {
                Main.getMessageManager().send(online, "bounty.bounty-refunded",
                        Map.of("amount", dev.frost.frostcore.utils.EconomyUtil.formatCompact(refund),
                               "target", bounty.getTargetName()));
            }
        }
    }

    public BountyRepository getRepository() {
        return repository;
    }
}
