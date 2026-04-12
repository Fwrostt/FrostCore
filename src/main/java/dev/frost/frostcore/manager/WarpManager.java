package dev.frost.frostcore.manager;

import dev.frost.frostcore.database.DatabaseManager;
import dev.frost.frostcore.utils.FrostLogger;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


public class WarpManager {

    private final Map<String, Location> warps = new LinkedHashMap<>();
    private Location spawn;
    private final DatabaseManager db;

    private final Map<String, WarpItemConfig> warpConfigs = new LinkedHashMap<>();
    private final File           warpsFile;
    private       FileConfiguration warpsYml;

    public WarpManager(JavaPlugin plugin, DatabaseManager db) {
        this.db        = db;
        this.warpsFile = new File(plugin.getDataFolder(), "warps.yml");

        if (!this.warpsFile.exists()) {
            plugin.saveResource("warps.yml", false);
        }

        loadAll();
    }

    private void loadAll() {

        warps.putAll(db.loadServerWarps());
        spawn = db.loadSpawn();

        if (spawn != null) {
            FrostLogger.info("Server spawn loaded.");
        }

        warpsYml = YamlConfiguration.loadConfiguration(warpsFile);

        ConfigurationSection section = warpsYml.getConfigurationSection("warps");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection warpSection = section.getConfigurationSection(key);
                if (warpSection != null) {
                    warpConfigs.put(key.toLowerCase(), WarpItemConfig.loadFrom(warpSection, key));
                }
            }
        }

        boolean dirty = false;
        for (String warpName : warps.keySet()) {
            if (!warpConfigs.containsKey(warpName)) {
                WarpItemConfig def = WarpItemConfig.defaultFor(warpName);
                warpConfigs.put(warpName, def);
                writeConfigToYml(warpName, def);
                dirty = true;
            }
        }
        if (dirty) saveWarpsYml();

        FrostLogger.info("Loaded " + warps.size() + " warp(s) and their configurations.");
    }

    public Location getWarp(String name) {
        return warps.get(name.toLowerCase());
    }

    
    public void setWarp(String name, Location loc) {
        String key = name.toLowerCase();
        warps.put(key, loc);
        db.saveServerWarpAsync(key, loc);

        if (!warpConfigs.containsKey(key)) {
            WarpItemConfig def = WarpItemConfig.defaultFor(key);
            warpConfigs.put(key, def);
            writeConfigToYml(key, def);
            saveWarpsYml();
        }
    }

    
    public void deleteWarp(String name) {
        String key = name.toLowerCase();
        warps.remove(key);
        warpConfigs.remove(key);
        db.deleteServerWarpAsync(key);

        if (warpsYml.isSet("warps." + key)) {
            warpsYml.set("warps." + key, null);
            saveWarpsYml();
        }
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

    public Location getSpawn() { return spawn; }

    public void setSpawn(Location loc) {
        this.spawn = loc;
        db.saveSpawnAsync(loc);
    }

    
    public WarpItemConfig getWarpConfig(String name) {
        return warpConfigs.getOrDefault(name.toLowerCase(), WarpItemConfig.defaultFor(name));
    }

    
    public void setWarpConfig(String name, WarpItemConfig config) {
        String key = name.toLowerCase();
        warpConfigs.put(key, config);
        writeConfigToYml(key, config);
        saveWarpsYml();
    }

    
    public void reloadWarpsYml() {
        warpsYml = YamlConfiguration.loadConfiguration(warpsFile);
        warpConfigs.clear();

        ConfigurationSection section = warpsYml.getConfigurationSection("warps");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection ws = section.getConfigurationSection(key);
                if (ws != null) {
                    warpConfigs.put(key.toLowerCase(), WarpItemConfig.loadFrom(ws, key));
                }
            }
        }

        FrostLogger.info("warps.yml reloaded — " + warpConfigs.size() + " warp config(s) loaded.");
    }

    private void writeConfigToYml(String key, WarpItemConfig cfg) {
        ConfigurationSection section = warpsYml.createSection("warps." + key);
        cfg.saveTo(section);
    }

    private void saveWarpsYml() {
        try {
            warpsYml.save(warpsFile);
        } catch (IOException e) {
            FrostLogger.error("Failed to save warps.yml!", e);
        }
    }
}

