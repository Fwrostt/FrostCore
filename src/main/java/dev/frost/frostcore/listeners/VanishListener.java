package dev.frost.frostcore.listeners;

import dev.frost.frostcore.manager.VanishManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Ensures vanished players remain invisible to newly joining players,
 * suppresses their join/quit messages, and cleans up vanish state on quit.
 */
public class VanishListener implements Listener {

    private final VanishManager vm = VanishManager.getInstance();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player joiner = event.getPlayer();

        // If the joining player is vanished (re-vanish after relog is handled by VanishManager re-login logic)
        // For now, just hide all currently vanished players from this new joiner
        vm.hideVanishedFrom(joiner);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player quitter = event.getPlayer();

        // Suppress quit message if vanished
        if (vm.isVanished(quitter.getUniqueId())) {
            event.quitMessage(null);
        }

        // Remove vanish state when the vanished player leaves
        vm.cleanup(quitter.getUniqueId());
    }
}
