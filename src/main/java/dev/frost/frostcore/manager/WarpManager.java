package dev.frost.frostcore.manager;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.database.DatabaseManager;
import dev.frost.frostcore.utils.FrostLogger;
import org.bukkit.Location;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class WarpManager {

    private final Map<String, Location> warps = new LinkedHashMap<>();
    private Location spawn;
    private final DatabaseManager db;

    public WarpManager(DatabaseManager db) {
        this.db = db;
        loadAll();
    }

    private void loadAll() {
        warps.putAll(db.loadServerWarps());
        spawn = db.loadSpawn();
        if (spawn != null) {
            FrostLogger.info("Server spawn loaded.");
        }
    }

    public Location getWarp(String name) {
        return warps.get(name.toLowerCase());
    }

    public void setWarp(String name, Location loc) {
        warps.put(name.toLowerCase(), loc);
        db.saveServerWarpAsync(name.toLowerCase(), loc);
    }

    public void deleteWarp(String name) {
        warps.remove(name.toLowerCase());
        db.deleteServerWarpAsync(name.toLowerCase());
    }

    public boolean hasWarp(String name) {
        return warps.containsKey(name.toLowerCase());
    }

    public Set<String> getWarpNames() {
        return Collections.unmodifiableSet(warps.keySet());
    }

    public Map<String, Location> getAllWarps() {
        return Collections.unmodifiableMap(warps);
    }

    public Location getSpawn() {
        return spawn;
    }

    public void setSpawn(Location loc) {
        this.spawn = loc;
        db.saveSpawnAsync(loc);
    }
}
