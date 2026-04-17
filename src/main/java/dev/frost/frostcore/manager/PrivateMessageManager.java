package dev.frost.frostcore.manager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class PrivateMessageManager {

    private static PrivateMessageManager instance;

    /** Stores the last person each player sent/received a message from for /r */
    private final Map<UUID, UUID> replyTargets = new ConcurrentHashMap<>();

    /** Players that a given player is ignoring */
    private final Map<UUID, Set<UUID>> ignoreLists = new ConcurrentHashMap<>();

    /** Players with social-spy enabled */
    private final Set<UUID> socialSpies = ConcurrentHashMap.newKeySet();

    /** Players who have blocked incoming private messages (/msgtoggle) */
    private final Set<UUID> msgBlocked = ConcurrentHashMap.newKeySet();

    /** Players who have hidden global chat for themselves (/chattoggle) */
    private final Set<UUID> chatHidden = ConcurrentHashMap.newKeySet();

    public PrivateMessageManager() {
        instance = this;
    }

    public static PrivateMessageManager getInstance() {
        return instance;
    }

    // ── Reply targets ────────────────────────────────────────────

    public void setReplyTarget(UUID player, UUID target) {
        replyTargets.put(player, target);
    }

    public UUID getReplyTarget(UUID player) {
        return replyTargets.get(player);
    }

    // ── Ignore list ──────────────────────────────────────────────

    public boolean isIgnoring(UUID player, UUID target) {
        Set<UUID> ignored = ignoreLists.get(player);
        return ignored != null && ignored.contains(target);
    }

    /** @return true if now ignoring, false if un-ignored */
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

    // ── Social spy ───────────────────────────────────────────────

    /** @return true if now active, false if deactivated */
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

    // ── Msg toggle (/msgtoggle) ──────────────────────────────────

    /**
     * Toggles whether a player accepts private messages.
     * @return true if messages are now blocked, false if now open
     */
    public boolean toggleMsgBlock(UUID player) {
        if (msgBlocked.contains(player)) {
            msgBlocked.remove(player);
            return false;
        } else {
            msgBlocked.add(player);
            return true;
        }
    }

    /** Returns true if the player has blocked incoming private messages. */
    public boolean isMsgBlocked(UUID player) {
        return msgBlocked.contains(player);
    }

    // ── Chat toggle (/chattoggle) ────────────────────────────────

    /**
     * Toggles whether global chat messages are hidden for the player.
     * @return true if global chat is now hidden, false if now visible
     */
    public boolean toggleChatHide(UUID player) {
        if (chatHidden.contains(player)) {
            chatHidden.remove(player);
            return false;
        } else {
            chatHidden.add(player);
            return true;
        }
    }

    /** Returns true if the player has hidden global chat. */
    public boolean isChatHidden(UUID player) {
        return chatHidden.contains(player);
    }

    // ── Cleanup ──────────────────────────────────────────────────

    public void cleanup(UUID player) {
        replyTargets.remove(player);
        socialSpies.remove(player);
        msgBlocked.remove(player);
        chatHidden.remove(player);
    }
}
