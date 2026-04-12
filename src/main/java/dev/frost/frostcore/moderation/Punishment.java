package dev.frost.frostcore.moderation;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Immutable representation of a single punishment record.
 * Corresponds to one row in the {@code punishments} database table.
 */
public record Punishment(
        int id,
        PunishmentType type,
        UUID targetUuid,
        String targetName,
        String ip,
        String reason,
        UUID staffUuid,
        String staffName,
        long duration,
        long createdAt,
        long expiresAt,
        boolean active,
        UUID removedBy,
        String removedByName,
        Long removedAt,
        String removedReason,
        String server,
        boolean silent,
        String randomId
) {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String HEX_CHARS = "0123456789ABCDEF";

    /**
     * Generate a 6-character randomized hex ID for privacy.
     */
    public static String generateRandomId() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(HEX_CHARS.charAt(RANDOM.nextInt(HEX_CHARS.length())));
        }
        return sb.toString();
    }

    /** Check if this punishment has expired naturally. */
    public boolean isExpired() {
        return expiresAt != -1 && expiresAt < System.currentTimeMillis();
    }

    /** Whether this punishment is currently in effect (active and not expired). */
    public boolean isInEffect() {
        return active && !isExpired();
    }

    /** Whether this is a permanent punishment. */
    public boolean isPermanent() {
        return expiresAt == -1;
    }

    /** Get remaining time in milliseconds, or -1 if permanent. */
    public long getRemainingMs() {
        if (isPermanent()) return -1;
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }

    /** Format the remaining duration as a human-readable string. */
    public String getFormattedRemaining() {
        if (isPermanent()) return "Permanent";
        long remaining = getRemainingMs();
        if (remaining <= 0) return "Expired";
        return formatDuration(remaining);
    }

    /** Format the original duration as a human-readable string. */
    public String getFormattedDuration() {
        if (duration == -1) return "Permanent";
        return formatDuration(duration);
    }

    /** Get the staff display name (CONSOLE if null). */
    public String getStaffDisplayName() {
        return staffName != null ? staffName : "CONSOLE";
    }

    /** Get the target display name. */
    public String getTargetDisplayName() {
        return targetName != null ? targetName : (targetUuid != null ? targetUuid.toString().substring(0, 8) : "Unknown");
    }

    /**
     * Format milliseconds into a human-readable duration string.
     * e.g., "7d 3h 25m" or "2h 10m" or "45s"
     */
    public static String formatDuration(long millis) {
        if (millis <= 0) return "0s";
        long totalSeconds = millis / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 && days == 0 && hours == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    /**
     * Parses a human-readable time string like "1d2h30m" into milliseconds.
     *
     * @return milliseconds if valid, -1 for permanent, -2 for invalid input
     */
    public static long parseTime(String input) {
        if (input == null || input.isEmpty() || input.equalsIgnoreCase("permanent")
                || input.equalsIgnoreCase("perm") || input.equalsIgnoreCase("p")) {
            return -1;
        }

        long totalMs = 0;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(\\d+)([smhdwMy])")
                .matcher(input.toLowerCase());

        boolean found = false;
        while (matcher.find()) {
            found = true;
            long val = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            totalMs += switch (unit) {
                case "s" -> val * 1000L;
                case "m" -> val * 60_000L;
                case "h" -> val * 3_600_000L;
                case "d" -> val * 86_400_000L;
                case "w" -> val * 604_800_000L;
                case "M" -> val * 2_592_000_000L;  // 30 days
                case "y" -> val * 31_536_000_000L;  // 365 days
                default -> 0;
            };
        }
        return found ? totalMs : -2;
    }
}
