package dev.frost.frostcore.cmds;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.gui.impls.HomesGui;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.manager.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HomesCmd implements CommandExecutor {

    private final MessageManager mm = MessageManager.get();
    private final ConfigManager config = Main.getConfigManager();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("frostcore.homes")) {
            mm.sendRaw(player, "<red>You don't have permission to use this command.");
            return true;
        }

        if (!config.getBoolean("homes.enabled", true)) {
            mm.send(player, "homes.disabled");
            return true;
        }

        HomesGui gui = new HomesGui(player);
        gui.open(player);
        return true;
    }
}

