package dev.frost.frostcore.invites;

/**
 * Defines the category of an invite.
 * Add new types here as the plugin grows (e.g. TPA, TRADE).
 */
public enum InviteType {

    /** Invite a player to join your team. */
    TEAM_JOIN,

    /** Request an alliance between two teams. */
    TEAM_ALLY
}
