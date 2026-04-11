package dev.frost.frostcore.listeners;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.invites.InviteManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Cleans up pending invites when a player disconnects.
 */
public class PlayerQuitListener implements Listener {

    private final InviteManager inviteManager = Main.getInviteManager();

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Remove all pending invites targeted at or sent by this player
        inviteManager.cancelAllFor(event.getPlayer().getUniqueId());
    }
}
