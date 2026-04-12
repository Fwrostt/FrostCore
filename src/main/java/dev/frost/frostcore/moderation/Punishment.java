package dev.frost.frostcore.moderation;

import java.security.SecureRandom;
import java.util.UUID;


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

    
    public static String generateRandomId() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(HEX_CHARS.charAt(RANDOM.nextInt(HEX_CHARS.length())));
        }
        return sb.toString();
    }

    
    public boolean isExpired() {
        return expiresAt != -1 && expiresAt < System.currentTimeMillis();
    }

    
    public boolean isInEffect() {
        return active && !isExpired();
    }

    
    public boolean isPermanent() {
        return expiresAt == -1;
    }

    
    public long getRemainingMs() {
        if (isPermanent()) return -1;
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }

    
    public String getFormattedRemaining() {
        if (isPermanent()) return "Permanent";
        long remaining = getRemainingMs();
        if (remaining <= 0) return "Expired";
        return formatDuration(remaining);
    }

    
    public String getFormattedDuration() {
        if (duration == -1) return "Permanent";
        return formatDuration(duration);
    }

    
    public String getStaffDisplayName() {
        return staffName != null ? staffName : "CONSOLE";
    }

    
    public String getTargetDisplayName() {
        return targetName != null ? targetName : (targetUuid != null ? targetUuid.toString().substring(0, 8) : "Unknown");
    }

    
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
                case "M" -> val * 2_592_000_000L;  
                case "y" -> val * 31_536_000_000L;  
                default -> 0;
            };
        }
        return found ? totalMs : -2;
    }
}
