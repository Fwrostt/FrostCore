package dev.frost.frostcore.bounty.model;

import dev.frost.frostcore.bounty.BountyStatus;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents an active or historical bounty placed on a specific target player.
 *
 * <p>Thread-safety model: {@code totalAmount} and {@code contributors} are mutated
 * only from async tasks coordinated by {@link dev.frost.frostcore.bounty.BountyService}.
 * The {@code state} field is an {@link AtomicReference} that acts as a secure multi-state guard
 * to prevent double-reward and reward-vs-expiry races on the kill event path.</p>
 */
public class Bounty {

    private final UUID targetUuid;
    private final String targetName;
    private volatile double totalAmount;
    private final AtomicReference<BountyStatus> state;
    private final long createdAt;
    private volatile long updatedAt;
    private volatile long expiresAt; // -1 = no expiry

    /** Live, thread-safe contributor list (read-heavy, rarely written). */
    private final CopyOnWriteArrayList<BountyContributor> contributors = new CopyOnWriteArrayList<>();

    public Bounty(UUID targetUuid, String targetName, double totalAmount,
                  BountyStatus status, long createdAt, long updatedAt, long expiresAt) {
        this.targetUuid  = targetUuid;
        this.targetName  = targetName;
        this.totalAmount = totalAmount;
        this.state       = new AtomicReference<>(status);
        this.createdAt   = createdAt;
        this.updatedAt   = updatedAt;
        this.expiresAt   = expiresAt;
    }

    // ── Transition guards ────────────────────────────────────────────────────────────

    /**
     * Atomically transitions this bounty from ACTIVE to CLAIMED.
     *
     * @return {@code true} if this call succeeded; {@code false} if
     *         another thread already claimed/expired it.
     */
    public boolean tryClaimAtomically() {
        return state.compareAndSet(BountyStatus.ACTIVE, BountyStatus.CLAIMED);
    }

    /**
     * Atomically transitions this bounty from ACTIVE to EXPIRED.
     *
     * @return {@code true} if this call succeeded; {@code false} if already claimed/expired.
     */
    public boolean tryExpireAtomically() {
        return state.compareAndSet(BountyStatus.ACTIVE, BountyStatus.EXPIRED);
    }

    // ── Contributors ───────────────────────────────────────────────────────────

    public void addContributor(BountyContributor contributor) {
        contributors.add(contributor);
    }

    public void setContributors(List<BountyContributor> list) {
        contributors.clear();
        contributors.addAll(list);
    }

    public List<BountyContributor> getContributors() {
        return contributors;
    }

    public int getContributorCount() {
        return contributors.size();
    }

    /** Returns the contributor who put in the highest individual amount, or {@code null}. */
    public BountyContributor getTopContributor() {
        return contributors.stream()
                .max(java.util.Comparator.comparingDouble(BountyContributor::getAmount))
                .orElse(null);
    }

    // ── Mutation helpers ───────────────────────────────────────────────────────

    public synchronized void addAmount(double amount) {
        totalAmount += amount;
        updatedAt = System.currentTimeMillis();
    }

    // ── Getters / setters ──────────────────────────────────────────────────────

    public UUID getTargetUuid()    { return targetUuid; }
    public String getTargetName()  { return targetName; }
    public double getTotalAmount() { return totalAmount; }
    public BountyStatus getStatus(){ return state.get(); }
    public long getCreatedAt()     { return createdAt; }
    public long getUpdatedAt()     { return updatedAt; }
    public long getExpiresAt()     { return expiresAt; }

    public synchronized void setTotalAmount(double amount)  { this.totalAmount = amount; }
    public void setStatus(BountyStatus status) { this.state.set(status); }
    public void setUpdatedAt(long t)           { this.updatedAt = t; }
    public void setExpiresAt(long t)           { this.expiresAt = t; }

    public boolean isExpired() {
        return expiresAt > 0 && System.currentTimeMillis() > expiresAt;
    }
}
