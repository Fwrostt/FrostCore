package dev.frost.frostcore.moderation;

import java.util.UUID;


public record Report(
        int id,
        UUID reporterUuid,
        String reporterName,
        UUID targetUuid,
        String targetName,
        String reason,
        long createdAt,
        boolean handled,
        UUID handledBy,
        String handledByName,
        Long handledAt
) {

    
    public String getReporterDisplayName() {
        return reporterName != null ? reporterName : reporterUuid.toString().substring(0, 8);
    }

    
    public String getTargetDisplayName() {
        return targetName != null ? targetName : targetUuid.toString().substring(0, 8);
    }

    
    public String getAge() {
        return Punishment.formatDuration(System.currentTimeMillis() - createdAt) + " ago";
    }
}
