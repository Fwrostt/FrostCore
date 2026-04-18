package dev.frost.frostcore.bounty;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.bounty.model.Bounty;
import dev.frost.frostcore.bounty.model.BountyContributor;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.utils.EconomyUtil;
import dev.frost.frostcore.utils.FrostLogger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Business-logic layer for the bounty system.
 *
 * <p>All methods that write to the database are designed to be called from a
 * Bukkit async task. Economy operations (withdraw/deposit) are safe to call
 * async via Vault. The kill-claim path runs on the main thread (PlayerDeathEvent)
 * but immediately hands off DB work to an async task.</p>
 */
public class BountyService {

    private final BountyManager manager;
    private final BountyRepository repository;
    private final MessageManager mm;

    /**
     * Player-level cooldown tracking for placing bounties.
     * Key = placer UUID, Value = expiry timestamp.
     */
    private final java.util.concurrent.ConcurrentHashMap<UUID, Long> placeCooldowns =
            new java.util.concurrent.ConcurrentHashMap<>();

    public BountyService(BountyManager manager) {
        this.manager    = manager;
        this.repository = manager.getRepository();
        this.mm         = Main.getMessageManager();

        // Prevent memory leak for offline players
        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), () -> {
            long now = System.currentTimeMillis();
            placeCooldowns.entrySet().removeIf(entry -> entry.getValue() < now);
        }, 1200L, 1200L);
    }

    // ── Place bounty ───────────────────────────────────────────────────────────

    /**
     * Places (or stacks) a bounty from {@code placer} onto {@code target}.
     *
     * <p>Must be called from an <em>async</em> context. Economy withdraw happens
     * first; if the DB insert then fails, the money is refunded immediately.</p>
     *
     * @param placer the player placing the bounty (must be online)
     * @param target offline-player target
     * @param amount parsed, validated amount
     * @return {@code true} on success; messages are already sent to the placer
     */
    public boolean placeBounty(Player placer, OfflinePlayer target, double amount) {
        FileConfiguration cfg = Main.getConfigManager().getConfig();
        UUID placerUuid = placer.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        // ── 1. Guard: economy active ───────────────────────────────────────────
        if (!EconomyUtil.isEnabled()) {
            mm.send(placer, "bounty.no-economy");
            return false;
        }

        // ── 2. Guard: feature enabled ──────────────────────────────────────────
        if (!manager.isEnabled()) {
            mm.send(placer, "bounty.disabled");
            return false;
        }

        // ── 3. Guard: self-bounty ──────────────────────────────────────────────
        if (cfg.getBoolean("bounty.prevent-self-bounty", true) && placerUuid.equals(targetUuid)) {
            mm.send(placer, "bounty.cannot-target-self");
            return false;
        }

        // ── 4. Guard: amount range ─────────────────────────────────────────────
        double min = cfg.getDouble("bounty.min-amount", 100.0);
        double max = cfg.getDouble("bounty.max-amount", 1_000_000.0);
        if (amount < min) {
            mm.send(placer, "bounty.amount-too-low",
                    Map.of("min", EconomyUtil.formatCompact(min)));
            return false;
        }
        if (amount > max) {
            mm.send(placer, "bounty.amount-too-high",
                    Map.of("max", EconomyUtil.formatCompact(max)));
            return false;
        }

        // ── 5. Guard: place cooldown ───────────────────────────────────────────
        long cooldownMs = cfg.getLong("bounty.place-cooldown-ms", 30_000L);
        if (cooldownMs > 0) {
            Long expires = placeCooldowns.get(placerUuid);
            long now     = System.currentTimeMillis();
            if (expires != null && now < expires) {
                long remaining = (expires - now) / 1000;
                mm.send(placer, "bounty.place-cooldown",
                        Map.of("seconds", String.valueOf(remaining)));
                return false;
            }
        }

        // ── 6. Guard: max contributors per target ──────────────────────────────
        int maxContribs = cfg.getInt("bounty.max-contributors", 0); // 0 = unlimited
        Bounty existing = manager.getBounty(targetUuid);
        if (maxContribs > 0 && existing != null && existing.getContributorCount() >= maxContribs) {
            mm.send(placer, "bounty.max-contributors-reached",
                    Map.of("max", String.valueOf(maxContribs)));
            return false;
        }

        // ── 7. Withdraw money first (fail-safe) ────────────────────────────────
        if (!EconomyUtil.has(placer, amount)) {
            mm.send(placer, "bounty.insufficient-funds",
                    Map.of("amount", EconomyUtil.formatCompact(amount)));
            return false;
        }
        if (!EconomyUtil.withdraw(placer, amount)) {
            mm.send(placer, "bounty.insufficient-funds",
                    Map.of("amount", EconomyUtil.formatCompact(amount)));
            return false;
        }

        // ── 8. Commit to DB (refund on failure) ────────────────────────────────
        long now = System.currentTimeMillis();
        String targetName = target.getName() != null ? target.getName() : targetUuid.toString().substring(0, 8);

        if (existing == null) {
            // New bounty
            long expiresAt = buildExpiresAt(now, cfg);
            Bounty newBounty = new Bounty(targetUuid, targetName, amount,
                    BountyStatus.ACTIVE, now, now, expiresAt);

            if (!repository.insertBounty(newBounty)) {
                EconomyUtil.deposit(placer, amount); // rollback
                mm.send(placer, "bounty.error-db");
                return false;
            }

            BountyContributor c = new BountyContributor(-1, targetUuid, placerUuid,
                    placer.getName(), amount, now);
            long cId = repository.insertContributor(c);
            if (cId < 0) {
                // Non-critical: bounty header exists; just log
                FrostLogger.warn("[Bounty] Contributor row missing for " + placer.getName());
            } else {
                BountyContributor stored = new BountyContributor(cId, targetUuid, placerUuid,
                        placer.getName(), amount, now);
                newBounty.addContributor(stored);
            }

            manager.putBounty(newBounty);

        } else {
            // Stack onto existing bounty
            existing.addAmount(amount);
            repository.updateBountyAmount(targetUuid, amount);

            // Check if this player already contributed
            BountyContributor prev = existing.getContributors().stream()
                    .filter(c -> c.getContributorUuid().equals(placerUuid))
                    .findFirst().orElse(null);

            if (prev != null) {
                prev.addAmount(amount);
                repository.updateContributorAmount(prev.getId(), amount);
            } else {
                BountyContributor c = new BountyContributor(-1, targetUuid, placerUuid,
                        placer.getName(), amount, now);
                long cId = repository.insertContributor(c);
                if (cId >= 0) {
                    existing.addContributor(new BountyContributor(
                            cId, targetUuid, placerUuid, placer.getName(), amount, now));
                }
            }

            manager.putBounty(existing); // re-triggers leaderboard rebuild
        }

        // ── 9. Set place cooldown ──────────────────────────────────────────────
        if (cooldownMs > 0) {
            placeCooldowns.put(placerUuid, System.currentTimeMillis() + cooldownMs);
        }

        // ── 10. Notify + optional broadcast ───────────────────────────────────
        mm.send(placer, "bounty.bounty-placed",
                Map.of("target", targetName,
                       "amount", EconomyUtil.formatCompact(amount),
                       "total",  EconomyUtil.formatCompact(manager.getBounty(targetUuid).getTotalAmount())));

        double broadcastThreshold = cfg.getDouble("bounty.announce.min-amount-threshold", 0.0);
        boolean announceOnPlace   = cfg.getBoolean("bounty.announce.on-place", true);
        double totalNow = manager.getBounty(targetUuid).getTotalAmount();

        if (announceOnPlace && totalNow >= broadcastThreshold) {
            boolean broadcast = cfg.getBoolean("bounty.announce.broadcast", true);
            if (broadcast) {
                mm.broadcast("bounty.announce-place",
                        Map.of("placer",  placer.getName(),
                               "target",  targetName,
                               "amount",  EconomyUtil.formatCompact(amount),
                               "total",   EconomyUtil.formatCompact(totalNow)));
            }
        }

        return true;
    }

    // ── Remove bounty ──────────────────────────────────────────────────────────

    /**
     * Removes a bounty placed by a staff member or the contributor themselves.
     * Refunds based on config. Must be called async.
     */
    public boolean removeBounty(org.bukkit.command.CommandSender remover, UUID targetUuid) {
        Bounty bounty = manager.getBounty(targetUuid);
        if (bounty == null) {
            mm.send(remover, "bounty.no-bounty-on-target");
            return false;
        }

        // Move to removed state in DB, then delete rows
        repository.markExpired(targetUuid);
        repository.deleteBounty(targetUuid);
        manager.removeBounty(targetUuid);

        boolean refund = Main.getConfigManager().getBoolean("bounty.refund-on-remove", false);
        if (refund) {
            manager.refundContributors(bounty);
        }

        mm.send(remover, "bounty.bounty-removed",
                Map.of("target", bounty.getTargetName()));
        return true;
    }

    // ── Claim bounty (kill event) ──────────────────────────────────────────────

    /**
     * Attempts to claim the bounty on {@code target}. Must be called from the
     * main thread (PlayerDeathEvent); heavy work is immediately deferred async.
     *
     * @param killer the player who got the kill
     * @param target the player who died
     */
    public void tryClaimBounty(Player killer, Player target) {
        if (!manager.isEnabled() || !EconomyUtil.isEnabled()) return;

        UUID targetUuid = target.getUniqueId();
        Bounty bounty = manager.getBounty(targetUuid);
        if (bounty == null || bounty.getStatus() != BountyStatus.ACTIVE) return;

        // ── Anti-suicide ───────────────────────────────────────────────────────
        if (killer.getUniqueId().equals(targetUuid)) return;

        // ── Anti-abuse (Self-claim) ────────────────────────────────────────────
        if (Main.getConfigManager().getBoolean("bounty.anti-abuse.prevent-self-claim", true)) {
            boolean contributed = bounty.getContributors().stream()
                    .anyMatch(c -> c.getContributorUuid().equals(killer.getUniqueId()));
            if (contributed) {
                mm.send(killer, "bounty.cannot-claim-own"); // Add visual feedback for clarity
                return;
            }
        }

        // ── Atomic claim guard (prevents any race between two simultaneous deaths)
        if (!bounty.tryClaimAtomically()) {
            FrostLogger.warn("[Bounty] Double-claim race blocked for " + target.getName());
            return;
        }

        final double reward = bounty.getTotalAmount();
        final String killerName = killer.getName();
        final String targetName = target.getName();
        final UUID killerUuid   = killer.getUniqueId();

        // Mark in memory immediately so no further claims can fire
        bounty.setStatus(BountyStatus.CLAIMED);
        manager.removeBounty(targetUuid);

        // Defer all DB + economy to async
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            // ── 1. Update DB status FIRST (anti-dupe: reward after DB confirms) ─
            repository.markClaimed(targetUuid);

            // ── 2. Pay reward ──────────────────────────────────────────────────
            OfflinePlayer killerOp = Bukkit.getOfflinePlayer(killerUuid);
            boolean deposited = EconomyUtil.deposit(killerOp, reward);

            if (!deposited) {
                FrostLogger.error("[Bounty] Failed to deposit bounty reward to " + killerName
                        + "! Attempting once more...");
                EconomyUtil.deposit(killerOp, reward);
            }

            // ── 3. Notify killer (main thread) ─────────────────────────────────
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                Player killerOnline = Bukkit.getPlayer(killerUuid);
                if (killerOnline != null) {
                    mm.send(killerOnline, "bounty.bounty-claimed",
                            Map.of("target", targetName,
                                   "amount", EconomyUtil.formatCompact(reward)));
                }
            });

            // ── 4. Broadcast ───────────────────────────────────────────────────
            FileConfiguration cfg = Main.getConfigManager().getConfig();
            if (cfg.getBoolean("bounty.announce.on-claim", true)
                    && cfg.getBoolean("bounty.announce.broadcast", true)) {
                mm.broadcast("bounty.announce-claim",
                        Map.of("killer", killerName,
                               "target", targetName,
                               "amount", EconomyUtil.formatCompact(reward)));
            }
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private long buildExpiresAt(long now, FileConfiguration cfg) {
        long expiryMs = cfg.getLong("bounty.expiry-ms", -1);
        return expiryMs > 0 ? now + expiryMs : -1;
    }
}
