package dev.frost.frostcore.listeners;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.cmds.moderation.ScreenshareCmd;
import dev.frost.frostcore.invites.InviteManager;
import dev.frost.frostcore.manager.BackManager;
import dev.frost.frostcore.manager.PrivateMessageManager;
import dev.frost.frostcore.manager.TeamEchestManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;


public class PlayerQuitListener implements Listener {

    private final InviteManager inviteManager = Main.getInviteManager();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        
        if (ScreenshareCmd.isInScreenshare(uuid)) {
            ScreenshareCmd.handleScreenshareDisconnect(player);
        }

        
        inviteManager.cancelAllFor(uuid);
        inviteManager.cancelAllSentBy(uuid);

        
        BackManager.getInstance().clear(uuid);

        
        PrivateMessageManager.getInstance().cleanup(uuid);

        
        if (TeamEchestManager.isViewingEchest(player)) {
            Main.getEchestManager().forceCloseForPlayer(player);
        }
    }
}
