package dev.frost.frostcore.listeners;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.invites.InviteManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Cleans up pending invites when a player disconnects.
 */
public class PlayerQuitListener implements Listener {

    private final InviteManager inviteManager = Main.getInviteManager();

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        inviteManager.cancelAllFor(uuid);

        inviteManager.cancelAllSentBy(uuid);
    }
}

