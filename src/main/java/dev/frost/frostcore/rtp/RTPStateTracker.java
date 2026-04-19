package dev.frost.frostcore.rtp;

import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Thread-safe state tracker for active RTP requests.
 * Manages pending status, warmup tasks, economy tracking, and performance metrics.
 */
public class RTPStateTracker {

    // ── Pending state ────────────────────────────────────────────
    private final Set<UUID> pendingPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BukkitTask> warmupTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Double> pendingCosts = new ConcurrentHashMap<>();

    // ── Metrics ──────────────────────────────────────────────────
    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong successCount = new AtomicLong();
    private final AtomicLong failCount = new AtomicLong();
    private final AtomicLong totalAttempts = new AtomicLong();
    private final LongAdder totalSearchTimeMs = new LongAdder();

    // ── Pending players ──────────────────────────────────────────

    public boolean isPending(UUID uuid) {
        return pendingPlayers.contains(uuid);
    }

    public void markPending(UUID uuid) {
        pendingPlayers.add(uuid);
    }

    public void clearPending(UUID uuid) {
        pendingPlayers.remove(uuid);
    }

    // ── Warmup tasks ─────────────────────────────────────────────

    public void setWarmupTask(UUID uuid, BukkitTask task) {
        warmupTasks.put(uuid, task);
    }

    public void cancelWarmup(UUID uuid) {
        BukkitTask task = warmupTasks.remove(uuid);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    public boolean hasWarmup(UUID uuid) {
        return warmupTasks.containsKey(uuid);
    }

    // ── Economy tracking ─────────────────────────────────────────

    public void setPendingCost(UUID uuid, double cost) {
        pendingCosts.put(uuid, cost);
    }

    public double getPendingCost(UUID uuid) {
        return pendingCosts.getOrDefault(uuid, 0.0);
    }

    public void clearPendingCost(UUID uuid) {
        pendingCosts.remove(uuid);
    }

    // ── Metrics ──────────────────────────────────────────────────

    public void recordRequest() {
        totalRequests.incrementAndGet();
    }

    public void recordSuccess(int attempts, long searchTimeMs) {
        successCount.incrementAndGet();
        totalAttempts.addAndGet(attempts);
        totalSearchTimeMs.add(searchTimeMs);
    }

    public void recordFailure(int attempts, long searchTimeMs) {
        failCount.incrementAndGet();
        totalAttempts.addAndGet(attempts);
        totalSearchTimeMs.add(searchTimeMs);
    }

    public long getTotalRequests()   { return totalRequests.get(); }
    public long getSuccessCount()    { return successCount.get(); }
    public long getFailCount()       { return failCount.get(); }

    public double getAverageAttempts() {
        long total = successCount.get() + failCount.get();
        return total == 0 ? 0 : (double) totalAttempts.get() / total;
    }

    public double getSuccessRate() {
        long total = totalRequests.get();
        return total == 0 ? 0 : (double) successCount.get() / total * 100.0;
    }

    public long getAverageSearchTimeMs() {
        long total = successCount.get() + failCount.get();
        return total == 0 ? 0 : totalSearchTimeMs.sum() / total;
    }

    // ── Cleanup ──────────────────────────────────────────────────

    public void cleanup() {
        for (BukkitTask task : warmupTasks.values()) {
            if (task != null && !task.isCancelled()) task.cancel();
        }
        warmupTasks.clear();
        pendingPlayers.clear();
        pendingCosts.clear();
    }
}
