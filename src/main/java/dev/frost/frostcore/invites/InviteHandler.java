package dev.frost.frostcore.invites;

/**
 * Callback interface for handling invite lifecycle events.
 * Each {@link InviteType} registers its own handler that defines
 * what happens when an invite is accepted, declined, or expires.
 */
public interface InviteHandler {

    /**
     * Called when the target accepts the invite.
     * Implementations should perform the action (e.g. add to team, create alliance).
     */
    void onAccept(Invite invite);

    /**
     * Called when the target declines the invite.
     * Implementations should notify the sender.
     */
    void onDecline(Invite invite);

    /**
     * Called when the invite expires without being accepted or declined.
     * Implementations should notify both parties.
     */
    void onExpire(Invite invite);
}
