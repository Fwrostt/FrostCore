package dev.frost.frostcore.invites;

import dev.frost.frostcore.Main;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Central manager for all invite types.
 * <p>
 * Invites are indexed by {@code (target UUID, InviteType)} for fast lookup.
 * A periodic cleanup task removes expired invites and fires their
 * {@link InviteHandler#onExpire(Invite)} callback.
 * <p>
 * Deduplication: only one invite per {@code (sender, target, type)} at a time.
 * Sending a duplicate refreshes the timer.
 * <p>
 * Usage for new invite types (e.g. TPA):
 * <pre>
 *   inviteManager.registerHandler(InviteType.TPA, new TpaHandler());
 *   inviteManager.sendInvite(InviteType.TPA, sender, target, Map.of(), 30);
 * </pre>
 */
public class InviteManager {

    private final Map<InviteType, InviteHandler> handlers = new EnumMap<>(InviteType.class);

    // target UUID -> (type -> list of pending invites)
    private final Map<UUID, Map<InviteType, List<Invite>>> invites = new ConcurrentHashMap<>();

    private BukkitTask cleanupTask;

    public InviteManager(Main plugin) {
        // Run cleanup every second (20 ticks)
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupExpired, 20L, 20L);
    }

    /**
     * Register a handler for an invite type. Must be called before sending invites of that type.
     */
    public void registerHandler(InviteType type, InviteHandler handler) {
        handlers.put(type, handler);
    }

    /**
     * Send an invite from sender to target.
     * If a duplicate (same sender, target, type) already exists, it is replaced (timer refreshed).
     *
     * @return the created Invite
     */
    public Invite sendInvite(InviteType type, UUID sender, UUID target,
                             Map<String, String> metadata, int expirySeconds) {
        Invite invite = new Invite(type, sender, target, metadata, expirySeconds);

        invites.computeIfAbsent(target, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(type, k -> Collections.synchronizedList(new ArrayList<>()));

        List<Invite> list = invites.get(target).get(type);

        // Remove existing duplicate (same sender + target + type)
        list.removeIf(existing -> existing.getSender().equals(sender));

        list.add(invite);
        return invite;
    }

    /**
     * Accept the most recent pending invite of the given type for the target.
     * If {@code fromSender} is provided, only accept from that specific sender.
     *
     * @return true if an invite was found and accepted
     */
    public boolean acceptInvite(UUID target, InviteType type, UUID fromSender) {
        Invite invite = findInvite(target, type, fromSender);
        if (invite == null) return false;

        removeInvite(target, type, invite);

        InviteHandler handler = handlers.get(type);
        if (handler != null) {
            handler.onAccept(invite);
        }
        return true;
    }

    /**
     * Decline the most recent pending invite of the given type for the target.
     * If {@code fromSender} is provided, only decline from that specific sender.
     *
     * @return true if an invite was found and declined
     */
    public boolean declineInvite(UUID target, InviteType type, UUID fromSender) {
        Invite invite = findInvite(target, type, fromSender);
        if (invite == null) return false;

        removeInvite(target, type, invite);

        InviteHandler handler = handlers.get(type);
        if (handler != null) {
            handler.onDecline(invite);
        }
        return true;
    }

    /**
     * Get all pending invites of a given type for a target.
     * Returns an unmodifiable snapshot.
     */
    public List<Invite> getInvites(UUID target, InviteType type) {
        Map<InviteType, List<Invite>> byType = invites.get(target);
        if (byType == null) return Collections.emptyList();

        List<Invite> list = byType.get(type);
        if (list == null) return Collections.emptyList();

        // Return non-expired only
        synchronized (list) {
            return list.stream()
                    .filter(inv -> !inv.isExpired())
                    .toList();
        }
    }

    /**
     * Get all pending invites of any type for a target.
     */
    public List<Invite> getAllInvites(UUID target) {
        Map<InviteType, List<Invite>> byType = invites.get(target);
        if (byType == null) return Collections.emptyList();

        List<Invite> result = new ArrayList<>();
        for (List<Invite> list : byType.values()) {
            synchronized (list) {
                list.stream()
                        .filter(inv -> !inv.isExpired())
                        .forEach(result::add);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Check if the target has any pending invite of the given type.
     */
    public boolean hasInvite(UUID target, InviteType type) {
        return !getInvites(target, type).isEmpty();
    }

    /**
     * Check if a specific invite exists from sender to target of the given type.
     */
    public boolean hasInviteFrom(UUID target, InviteType type, UUID sender) {
        return findInvite(target, type, sender) != null;
    }

    /**
     * Cancel all invites of a given type that match the predicate.
     * Does NOT fire onExpire/onDecline — this is a silent cancel.
     */
    public void cancelInvites(InviteType type, Predicate<Invite> filter) {
        for (Map<InviteType, List<Invite>> byType : invites.values()) {
            List<Invite> list = byType.get(type);
            if (list != null) {
                synchronized (list) {
                    list.removeIf(filter);
                }
            }
        }
    }

    /**
     * Cancel all invites for a specific target (e.g. on player quit).
     * Does NOT fire any callbacks — this is a silent removal.
     */
    public void cancelAllFor(UUID target) {
        invites.remove(target);
    }

    /**
     * Cancel all invites that were SENT by the given UUID, regardless of target.
     * Call this when the sender disconnects so stale invites are removed from
     * all potential recipients' inboxes.
     * Does NOT fire any callbacks — this is a silent removal.
     */
    public void cancelAllSentBy(UUID sender) {
        for (Map<InviteType, List<Invite>> byType : invites.values()) {
            for (List<Invite> list : byType.values()) {
                synchronized (list) {
                    list.removeIf(inv -> inv.getSender().equals(sender));
                }
            }
        }
    }

    /**
     * Shut down the cleanup task. Call from onDisable().
     */
    public void shutdown() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
        }
        invites.clear();
    }

    // ==================== INTERNAL ====================

    /**
     * Find the most recent non-expired invite matching the criteria.
     * If fromSender is null, returns the latest invite of that type.
     */
    private Invite findInvite(UUID target, InviteType type, UUID fromSender) {
        Map<InviteType, List<Invite>> byType = invites.get(target);
        if (byType == null) return null;

        List<Invite> list = byType.get(type);
        if (list == null) return null;

        synchronized (list) {
            // Iterate backwards to find the most recent
            for (int i = list.size() - 1; i >= 0; i--) {
                Invite inv = list.get(i);
                if (inv.isExpired()) continue;
                if (fromSender == null || inv.getSender().equals(fromSender)) {
                    return inv;
                }
            }
        }
        return null;
    }

    private void removeInvite(UUID target, InviteType type, Invite invite) {
        Map<InviteType, List<Invite>> byType = invites.get(target);
        if (byType == null) return;

        List<Invite> list = byType.get(type);
        if (list != null) {
            synchronized (list) {
                list.remove(invite);
            }
        }
    }

    /**
     * Periodic task: remove expired invites and fire onExpire handlers.
     */
    private void cleanupExpired() {
        for (Map.Entry<UUID, Map<InviteType, List<Invite>>> targetEntry : invites.entrySet()) {
            for (Map.Entry<InviteType, List<Invite>> typeEntry : targetEntry.getValue().entrySet()) {
                InviteType type = typeEntry.getKey();
                List<Invite> list = typeEntry.getValue();
                InviteHandler handler = handlers.get(type);

                List<Invite> expired;
                synchronized (list) {
                    expired = list.stream().filter(Invite::isExpired).toList();
                    list.removeAll(expired);
                }

                if (handler != null) {
                    expired.forEach(handler::onExpire);
                }
            }
        }
    }
}
