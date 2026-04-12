package dev.frost.frostcore.listeners;

import dev.frost.frostcore.manager.VanishManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;


public class VanishListener implements Listener {

    private final VanishManager vm = VanishManager.getInstance();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player joiner = event.getPlayer();

        
        if (vm.isVanished(joiner.getUniqueId())) {
            event.joinMessage(null);
        }

        
        vm.hideVanishedFrom(joiner);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player quitter = event.getPlayer();

        
        if (vm.isVanished(quitter.getUniqueId())) {
            event.quitMessage(null);
        }

        
        vm.cleanup(quitter.getUniqueId());
    }
}
