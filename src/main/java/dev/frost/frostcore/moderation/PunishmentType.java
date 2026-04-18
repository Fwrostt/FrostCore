package dev.frost.frostcore.moderation;


public enum PunishmentType {

    BAN("Ban", "banned", "frostcore.moderation.ban"),
    TEMPBAN("Temp Ban", "temporarily banned", "frostcore.moderation.ban"),
    MUTE("Mute", "muted", "frostcore.moderation.mute"),
    TEMPMUTE("Temp Mute", "temporarily muted", "frostcore.moderation.mute"),
    WARN("Warning", "warned", "frostcore.moderation.warn"),
    KICK("Kick", "kicked", "frostcore.moderation.kick"),
    IPBAN("IP Ban", "IP banned", "frostcore.moderation.ipban"),
    IPMUTE("IP Mute", "IP muted", "frostcore.moderation.ipmute"),
    JAIL("Jail", "jailed", "frostcore.moderation.jail"),
    SHADOWMUTE("Shadow Mute", "shadow muted", "frostcore.moderation.shadowmute");

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

    
    public boolean isTemporal() {
        return this == TEMPBAN || this == TEMPMUTE || this == JAIL;
    }

    
    public boolean isIpBased() {
        return this == IPBAN || this == IPMUTE;
    }

    
    public boolean isPersistent() {
        return this != KICK && this != WARN;
    }

    
    public String getCategory() {
        return switch (this) {
            case BAN, TEMPBAN, IPBAN -> "BAN";
            case MUTE, TEMPMUTE, IPMUTE, SHADOWMUTE -> "MUTE";
            case WARN -> "WARN";
            case KICK -> "KICK";
            case JAIL -> "JAIL";
        };
    }
}
