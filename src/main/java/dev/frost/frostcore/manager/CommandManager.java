package dev.frost.frostcore.manager;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.utils.FrostLogger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class CommandManager {

    private final Main plugin;
    private final File file;
    private FileConfiguration config;

    private final Map<String, PluginCommand> originalCommands = new HashMap<>();

    public CommandManager(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "commands.yml");
        
        if (!file.exists()) {
            plugin.saveResource("commands.yml", false);
        }
        
        loadConfig();
    }

    private void loadConfig() {
        config = YamlConfiguration.loadConfiguration(file);
        InputStream defStream = plugin.getResource("commands.yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream));
            config.setDefaults(defConfig);
            config.options().copyDefaults(true);
        }
    }

    public void reload() {
        loadConfig();
        
        CommandMap map = Bukkit.getCommandMap();
        Map<String, Command> known = map.getKnownCommands();

        int enabledCount = 0;
        int disabledCount = 0;

        for (Map.Entry<String, PluginCommand> entry : originalCommands.entrySet()) {
            String name = entry.getKey();
            PluginCommand pcmd = entry.getValue();

            boolean enabled = config.getBoolean("commands." + name, true);

            if (enabled) {
                if (!known.containsKey(name)) {
                    map.register(plugin.getName().toLowerCase(), pcmd);
                } else {
                    known.put(name, pcmd);
                }
                enabledCount++;
            } else {
                if (known.containsKey(name)) {
                    Command active = known.get(name);
                    active.unregister(map);
                    known.remove(name);
                    known.remove(plugin.getName().toLowerCase() + ":" + name);
                }
                disabledCount++;
            }
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.updateCommands();
            }
        });

        FrostLogger.info("Commands reloaded: " + enabledCount + " enabled, " + disabledCount + " disabled.");
    }

    public void registerCommand(String commandName, CommandExecutor executor, TabCompleter tabCompleter) {
        PluginCommand command = plugin.getCommand(commandName);

        if (command == null) {
            FrostLogger.error("Command '/" + commandName + "' not found in plugin.yml! Registration tracking failed.");
            return;
        }

        command.setExecutor(executor);
        if (tabCompleter != null) {
            command.setTabCompleter(tabCompleter);
        }

        originalCommands.put(commandName.toLowerCase(), command);
    }

    public void registerCommand(String commandName, CommandExecutor executor) {
        registerCommand(commandName, executor, null);
    }
}
