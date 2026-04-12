package dev.frost.frostcore.invites;

import dev.frost.frostcore.Main;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;


public class InviteManager {

    private final Map<InviteType, InviteHandler> handlers = new EnumMap<>(InviteType.class);

    private final Map<UUID, Map<InviteType, List<Invite>>> invites = new ConcurrentHashMap<>();

    private BukkitTask cleanupTask;

    public InviteManager(Main plugin) {

        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupExpired, 20L, 20L);
    }

    
    public void registerHandler(InviteType type, InviteHandler handler) {
        handlers.put(type, handler);
    }

    
    public Invite sendInvite(InviteType type, UUID sender, UUID target,
                             Map<String, String> metadata, int expirySeconds) {
        Invite invite = new Invite(type, sender, target, metadata, expirySeconds);

        invites.computeIfAbsent(target, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(type, k -> Collections.synchronizedList(new ArrayList<>()));

        List<Invite> list = invites.get(target).get(type);

        list.removeIf(existing -> existing.getSender().equals(sender));

        list.add(invite);
        return invite;
    }

    
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

    
    public List<Invite> getInvites(UUID target, InviteType type) {
        Map<InviteType, List<Invite>> byType = invites.get(target);
        if (byType == null) return Collections.emptyList();

        List<Invite> list = byType.get(type);
        if (list == null) return Collections.emptyList();

        synchronized (list) {
            return list.stream()
                    .filter(inv -> !inv.isExpired())
                    .toList();
        }
    }

    
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

    
    public boolean hasInvite(UUID target, InviteType type) {
        return !getInvites(target, type).isEmpty();
    }

    
    public boolean hasInviteFrom(UUID target, InviteType type, UUID sender) {
        return findInvite(target, type, sender) != null;
    }

    
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

    
    public void cancelAllFor(UUID target) {
        invites.remove(target);
    }

    
    public void cancelAllSentBy(UUID sender) {
        for (Map<InviteType, List<Invite>> byType : invites.values()) {
            for (List<Invite> list : byType.values()) {
                synchronized (list) {
                    list.removeIf(inv -> inv.getSender().equals(sender));
                }
            }
        }
    }

    
    public void shutdown() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
        }
        invites.clear();
    }

    
    private Invite findInvite(UUID target, InviteType type, UUID fromSender) {
        Map<InviteType, List<Invite>> byType = invites.get(target);
        if (byType == null) return null;

        List<Invite> list = byType.get(type);
        if (list == null) return null;

        synchronized (list) {

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

