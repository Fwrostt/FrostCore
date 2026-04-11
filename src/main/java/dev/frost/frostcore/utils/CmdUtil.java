package dev.frost.frostcore.utils;

import dev.frost.frostcore.Main;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import dev.frost.frostcore.utils.FrostLogger;

import java.util.HashMap;
import java.util.Map;

public class CmdUtil {

    private final JavaPlugin plugin = Main.getInstance();
    private final Map<String, CommandExecutor> commands = new HashMap<>();

    /**
     * Register a command with its executor and optional tab completer.
     *
     * @param commandName The name of the command (must match plugin.yml)
     * @param executor    The command executor
     * @param tabCompleter The tab completer (can be null)
     */
    public void registerCommand(String commandName, CommandExecutor executor, TabCompleter tabCompleter) {
        PluginCommand command = plugin.getCommand(commandName);

        if (command == null) {
            FrostLogger.error("Command '" + commandName + "' not found in plugin.yml! Registration failed.");
            return;
        }

        command.setExecutor(executor);
        if (tabCompleter != null) {
            command.setTabCompleter(tabCompleter);
        }

        commands.put(commandName.toLowerCase(), executor);
        FrostLogger.info("Successfully registered command: /" + commandName);
    }

    public void registerCommand(String commandName, CommandExecutor executor) {
        registerCommand(commandName, executor, null);
    }

    public void unregisterCommand(String commandName) {
        commands.remove(commandName.toLowerCase());
    }

    public CommandExecutor getExecutor(String commandName) {
        return commands.get(commandName.toLowerCase());
    }
}
