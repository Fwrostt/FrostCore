package dev.frost.frostcore.mace;

import java.util.UUID;

public record MaceEntry(
        String maceId,
        UUID originalCrafter,
        String crafterName,
        long craftedAt,
        UUID currentHolder,
        String holderName,
        long lastSeenAt,
        String lastWorld,
        double lastX,
        double lastY,
        double lastZ,
        String enchantments,
        boolean destroyed
) {

    public MaceEntry withHolder(UUID holder, String name) {
        return new MaceEntry(maceId, originalCrafter, crafterName, craftedAt,
                holder, name, System.currentTimeMillis(), lastWorld, lastX, lastY, lastZ, enchantments, destroyed);
    }

    public MaceEntry withLocation(String world, double x, double y, double z) {
        return new MaceEntry(maceId, originalCrafter, crafterName, craftedAt,
                currentHolder, holderName, System.currentTimeMillis(), world, x, y, z, enchantments, destroyed);
    }

    public MaceEntry withEnchantments(String enchants) {
        return new MaceEntry(maceId, originalCrafter, crafterName, craftedAt,
                currentHolder, holderName, System.currentTimeMillis(), lastWorld, lastX, lastY, lastZ, enchants, destroyed);
    }

    public MaceEntry withDestroyed(boolean flag) {
        return new MaceEntry(maceId, originalCrafter, crafterName, craftedAt,
                currentHolder, holderName, lastSeenAt, lastWorld, lastX, lastY, lastZ, enchantments, flag);
    }

    public String shortId() {
        return maceId.length() >= 8 ? maceId.substring(0, 8) : maceId;
    }

    public String getFormattedAge() {
        long elapsed = System.currentTimeMillis() - craftedAt;
        if (elapsed < 60_000) return (elapsed / 1000) + "s";
        if (elapsed < 3_600_000) return (elapsed / 60_000) + "m";
        if (elapsed < 86_400_000) return (elapsed / 3_600_000) + "h";
        return (elapsed / 86_400_000) + "d";
    }

    public String getFormattedLastSeen() {
        long elapsed = System.currentTimeMillis() - lastSeenAt;
        if (elapsed < 60_000) return (elapsed / 1000) + "s ago";
        if (elapsed < 3_600_000) return (elapsed / 60_000) + "m ago";
        if (elapsed < 86_400_000) return (elapsed / 3_600_000) + "h ago";
        return (elapsed / 86_400_000) + "d ago";
    }
}
