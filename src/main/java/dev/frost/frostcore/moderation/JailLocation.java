package dev.frost.frostcore.moderation;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Represents a named jail location stored in the database.
 */
public record JailLocation(
        String name,
        String worldName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {

    /** Convert to a Bukkit Location. Returns null if the world is not loaded. */
    public Location toBukkitLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z, yaw, pitch);
    }

    /** Create from a Bukkit Location. */
    public static JailLocation fromBukkitLocation(String name, Location loc) {
        return new JailLocation(
                name,
                loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch()
        );
    }
}
