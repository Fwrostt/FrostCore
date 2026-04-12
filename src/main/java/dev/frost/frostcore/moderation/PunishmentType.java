package dev.frost.frostcore.moderation;

/**
 * All punishment types tracked by the moderation system.
 * Stored as the {@code type} column in the {@code punishments} table.
 */
public enum PunishmentType {

    BAN("Ban", "banned", "frostcore.moderation.ban"),
    TEMPBAN("Temp Ban", "temporarily banned", "frostcore.moderation.ban"),
    MUTE("Mute", "muted", "frostcore.moderation.mute"),
    TEMPMUTE("Temp Mute", "temporarily muted", "frostcore.moderation.mute"),
    WARN("Warning", "warned", "frostcore.moderation.warn"),
    KICK("Kick", "kicked", "frostcore.moderation.kick"),
    IPBAN("IP Ban", "IP banned", "frostcore.moderation.ipban"),
    IPMUTE("IP Mute", "IP muted", "frostcore.moderation.ipmute"),
    JAIL("Jail", "jailed", "frostcore.moderation.jail");

    private final String displayName;
    private final String pastTense;
    private final String permission;

    PunishmentType(String displayName, String pastTense, String permission) {
        this.displayName = displayName;
        this.pastTense = pastTense;
        this.permission = permission;
    }

    public String getDisplayName() { return displayName; }
    public String getPastTense()   { return pastTense; }
    public String getPermission()  { return permission; }

    /** Whether this type has a duration component. */
    public boolean isTemporal() {
        return this == TEMPBAN || this == TEMPMUTE || this == JAIL;
    }

    /** Whether this is an IP-based punishment. */
    public boolean isIpBased() {
        return this == IPBAN || this == IPMUTE;
    }

    /** Whether this type remains active until explicitly removed. */
    public boolean isPersistent() {
        return this != KICK && this != WARN;
    }

    /** Map BAN/TEMPBAN → BAN category, etc. for history filtering. */
    public String getCategory() {
        return switch (this) {
            case BAN, TEMPBAN, IPBAN -> "BAN";
            case MUTE, TEMPMUTE, IPMUTE -> "MUTE";
            case WARN -> "WARN";
            case KICK -> "KICK";
            case JAIL -> "JAIL";
        };
    }
}
