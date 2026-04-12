package dev.frost.frostcore.cmds.teleport;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.gui.WarpsGui;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.manager.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Handles {@code /warps} — opens the interactive warp browser GUI.
 * <p>
 * If {@code warps.gui-enabled} is {@code false} in {@code config.yml}, it falls back
 * to displaying the classic chat warp list instead (delegates to {@link WarpCmd}).
 */
public class WarpsCmd implements CommandExecutor, TabCompleter {

    private final MessageManager mm     = MessageManager.get();
    private final ConfigManager  config = Main.getConfigManager();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("frostcore.warp")) {
            mm.sendRaw(player, "<red>You don't have permission to use this command.");
            return true;
        }

        if (!config.getBoolean("warps.enabled", true)) {
            mm.send(player, "teleport.warp-disabled");
            return true;
        }

        if (!config.getBoolean("warps.gui-enabled", true)) {
            sender.sendMessage("Use /warp <name> to teleport to a warp.");
            return true;
        }

        WarpsGui gui = new WarpsGui(player);
        gui.open(player);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}

