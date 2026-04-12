package dev.frost.frostcore.utils;

import dev.frost.frostcore.Main;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class CmdUtil {

    private final JavaPlugin plugin = Main.getInstance();
    private final Map<String, CommandExecutor> commands = new HashMap<>();
    private int registeredCount = 0;
    private int failedCount = 0;

    
    public void registerCommand(String commandName, CommandExecutor executor, TabCompleter tabCompleter) {
        PluginCommand command = plugin.getCommand(commandName);

        if (command == null) {
            FrostLogger.error("Command '/" + commandName + "' not found in plugin.yml! Registration failed.");
            failedCount++;
            return;
        }

        command.setExecutor(executor);
        if (tabCompleter != null) {
            command.setTabCompleter(tabCompleter);
        }

        commands.put(commandName.toLowerCase(), executor);
        registeredCount++;
    }

    public void registerCommand(String commandName, CommandExecutor executor) {
        registerCommand(commandName, executor, null);
    }

    
    public void printSummary() {
        if (failedCount > 0) {
            FrostLogger.warn("Registered " + registeredCount + " commands (" + failedCount + " failed).");
        } else {
            FrostLogger.info("Registered " + registeredCount + " commands.");
        }
    }

    public void unregisterCommand(String commandName) {
        commands.remove(commandName.toLowerCase());
    }

    public CommandExecutor getExecutor(String commandName) {
        return commands.get(commandName.toLowerCase());
    }
}
