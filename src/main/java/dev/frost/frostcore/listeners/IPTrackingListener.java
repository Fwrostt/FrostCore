package dev.frost.frostcore.listeners;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.moderation.ModerationManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Tracks player IPs and names on login for the moderation system.
 */
public class IPTrackingListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        var mod = ModerationManager.getInstance();
        if (mod == null) return;

        String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : null;

        // Record IP and name asynchronously
        var modDb = mod.getDatabase();
        if (ip != null) modDb.recordPlayerIpAsync(player.getUniqueId(), ip);
        modDb.recordPlayerNameAsync(player.getUniqueId(), player.getName());
    }
}
