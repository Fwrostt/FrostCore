package dev.frost.frostcore.moderation;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;


public record JailLocation(
        String name,
        String worldName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {

    
    public Location toBukkitLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z, yaw, pitch);
    }

    
    public static JailLocation fromBukkitLocation(String name, Location loc) {
        return new JailLocation(
                name,
                loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch()
        );
    }
}
