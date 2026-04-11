package dev.frost.frostcore.manager;

import dev.frost.frostcore.utils.FrostLogger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Level;

public class ConfigManager {

    private static ConfigManager instance;
    private final JavaPlugin plugin;
    private final File configFile;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            plugin.getDataFolder().mkdirs();
            plugin.saveResource("config.yml", false);
        }

        reloadConfig();
    }

    public static ConfigManager getInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new ConfigManager(plugin);
        }
        return instance;
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);

        InputStream defStream = plugin.getResource("config.yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream));
            config.setDefaults(defConfig);
            config.options().copyDefaults(true);
        }

        saveConfig();
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            FrostLogger.error("Could not save config to " + configFile, e);
        }
    }

    public String getString(String path, String def) { return config.getString(path, def); }
    public String getString(String path) { return config.getString(path); }

    public int getInt(String path, int def) { return config.getInt(path, def); }
    public int getInt(String path) { return config.getInt(path); }

    public double getDouble(String path, double def) { return config.getDouble(path, def); }
    public double getDouble(String path) { return config.getDouble(path); }

    public boolean getBoolean(String path, boolean def) { return config.getBoolean(path, def); }
    public boolean getBoolean(String path) { return config.getBoolean(path); }

    public List<String> getStringList(String path) { return config.getStringList(path); }

    /**
     * Update a config value in memory only. Call {@link #saveConfig()} separately
     * if you need the value persisted to disk.
     */
    public void set(String path, Object value) {
        config.set(path, value);
    }

    /**
     * Update a config value and immediately persist to disk.
     * Prefer {@link #set(String, Object)} for bulk updates to avoid redundant I/O.
     */
    public void setAndSave(String path, Object value) {
        config.set(path, value);
        saveConfig();
    }

    public boolean contains(String path) { return config.contains(path); }
    public FileConfiguration getConfig() { return config; }
}

