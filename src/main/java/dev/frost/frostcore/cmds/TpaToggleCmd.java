package dev.frost.frostcore.cmds;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * Handles /tpatoggle, /tpaon, and /tpaoff.
 * <ul>
 *   <li>/tpatoggle — flips current state</li>
 *   <li>/tpaon     — force-enables TPA requests</li>
 *   <li>/tpaoff    — force-disables TPA requests</li>
 * </ul>
 */
public class TpaToggleCmd implements CommandExecutor {

    public static final NamespacedKey TPA_DISABLED_KEY = new NamespacedKey(Main.getInstance(), "tpa_disabled");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        MessageManager mm = MessageManager.get();
        String cmdName = label.toLowerCase();

        boolean currentlyDisabled = player.getPersistentDataContainer()
                .has(TPA_DISABLED_KEY, PersistentDataType.BYTE);

        if (cmdName.equals("tpaon")) {
            // Force-enable regardless of current state
            if (!currentlyDisabled) {
                mm.sendRaw(player, "<yellow>Teleport requests are already <bold>ENABLED</bold>.");
                return true;
            }
            player.getPersistentDataContainer().remove(TPA_DISABLED_KEY);
            mm.sendRaw(player, "<green>Teleport requests are now <bold>ENABLED</bold>.");

        } else if (cmdName.equals("tpaoff")) {
            // Force-disable regardless of current state
            if (currentlyDisabled) {
                mm.sendRaw(player, "<yellow>Teleport requests are already <bold>DISABLED</bold>.");
                return true;
            }
            player.getPersistentDataContainer().set(TPA_DISABLED_KEY, PersistentDataType.BYTE, (byte) 1);
            mm.sendRaw(player, "<red>Teleport requests are now <bold>DISABLED</bold>.");

        } else {
            // /tpatoggle — flip current state
            if (currentlyDisabled) {
                player.getPersistentDataContainer().remove(TPA_DISABLED_KEY);
                mm.sendRaw(player, "<green>Teleport requests are now <bold>ENABLED</bold>.");
            } else {
                player.getPersistentDataContainer().set(TPA_DISABLED_KEY, PersistentDataType.BYTE, (byte) 1);
                mm.sendRaw(player, "<red>Teleport requests are now <bold>DISABLED</bold>.");
            }
        }

        return true;
    }
}
