package dev.frost.frostcore.moderation;

import java.util.UUID;

/**
 * Immutable representation of a player report.
 */
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

    /** Get the reporter display name. */
    public String getReporterDisplayName() {
        return reporterName != null ? reporterName : reporterUuid.toString().substring(0, 8);
    }

    /** Get the target display name. */
    public String getTargetDisplayName() {
        return targetName != null ? targetName : targetUuid.toString().substring(0, 8);
    }

    /** Format the age of this report. */
    public String getAge() {
        return Punishment.formatDuration(System.currentTimeMillis() - createdAt) + " ago";
    }
}
