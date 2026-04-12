package dev.frost.frostcore.listeners;

import dev.frost.frostcore.manager.BackManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class BackListener implements Listener {

    private final BackManager backManager = BackManager.getInstance();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        PlayerTeleportEvent.TeleportCause cause = event.getCause();

        
        if (cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL ||
            cause == PlayerTeleportEvent.TeleportCause.END_PORTAL ||
            cause == PlayerTeleportEvent.TeleportCause.END_GATEWAY ||
            cause == PlayerTeleportEvent.TeleportCause.SPECTATE) {
            return;
        }

        backManager.setLastLocation(event.getPlayer().getUniqueId(), event.getFrom());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        backManager.setLastLocation(event.getEntity().getUniqueId(), event.getEntity().getLocation());
    }
}
