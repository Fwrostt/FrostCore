package dev.frost.frostcore.listeners;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.invites.InviteManager;
import dev.frost.frostcore.manager.BackManager;
import dev.frost.frostcore.manager.PrivateMessageManager;
import dev.frost.frostcore.manager.TeamEchestManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Cleans up all player-specific state when a player disconnects.
 * Prevents memory leaks and stale data across managers.
 */
public class PlayerQuitListener implements Listener {

    private final InviteManager inviteManager = Main.getInviteManager();

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Clean up pending invites (sent and received)
        inviteManager.cancelAllFor(uuid);
        inviteManager.cancelAllSentBy(uuid);

        // Clean up back location to prevent memory leak
        BackManager.getInstance().clear(uuid);

        // Clean up reply targets and socialspy
        PrivateMessageManager.getInstance().cleanup(uuid);

        // Force-close team echest if the player had it open
        if (TeamEchestManager.isViewingEchest(player)) {
            Main.getEchestManager().forceCloseForPlayer(player);
        }
    }
}
