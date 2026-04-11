package dev.frost.frostcore.manager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages private messaging state: reply targets and ignore lists.
 * In-memory only — resets on restart.
 */
public class PrivateMessageManager {

    private static PrivateMessageManager instance;

    /** Last person each player received a message from (for /r). */
    private final Map<UUID, UUID> replyTargets = new ConcurrentHashMap<>();

    /** Per-player ignore lists. */
    private final Map<UUID, Set<UUID>> ignoreLists = new ConcurrentHashMap<>();

    /** Players with SocialSpy enabled. */
    private final Set<UUID> socialSpies = ConcurrentHashMap.newKeySet();

    public PrivateMessageManager() {
        instance = this;
    }

    public static PrivateMessageManager getInstance() {
        return instance;
    }

    public void setReplyTarget(UUID player, UUID target) {
        replyTargets.put(player, target);
    }

    public UUID getReplyTarget(UUID player) {
        return replyTargets.get(player);
    }

    public boolean isIgnoring(UUID player, UUID target) {
        Set<UUID> ignored = ignoreLists.get(player);
        return ignored != null && ignored.contains(target);
    }

    /**
     * Toggle ignore for a target.
     * @return true if now ignoring, false if no longer ignoring
     */
    public boolean toggleIgnore(UUID player, UUID target) {
        Set<UUID> ignored = ignoreLists.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet());
        if (ignored.contains(target)) {
            ignored.remove(target);
            return false;
        } else {
            ignored.add(target);
            return true;
        }
    }

    public void cleanup(UUID player) {
        replyTargets.remove(player);
        socialSpies.remove(player);
    }

    /**
     * Toggle SocialSpy for a player.
     * @return true if now enabled, false if disabled
     */
    public boolean toggleSocialSpy(UUID player) {
        if (socialSpies.contains(player)) {
            socialSpies.remove(player);
            return false;
        } else {
            socialSpies.add(player);
            return true;
        }
    }

    public boolean isSocialSpy(UUID player) {
        return socialSpies.contains(player);
    }

    public Set<UUID> getSocialSpies() {
        return Collections.unmodifiableSet(socialSpies);
    }
}
