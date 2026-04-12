package dev.frost.frostcore.manager;

import org.bukkit.Location;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class BackManager {

    private static BackManager instance;
    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();

    public BackManager() {
        instance = this;
    }

    public static BackManager getInstance() {
        return instance;
    }

    public void setLastLocation(UUID uuid, Location location) {
        if (location == null) {
            lastLocations.remove(uuid);
        } else {
            lastLocations.put(uuid, location.clone());
        }
    }

    public Location getLastLocation(UUID uuid) {
        return lastLocations.get(uuid);
    }
    
    public void clear(UUID uuid) {
        lastLocations.remove(uuid);
    }
}
