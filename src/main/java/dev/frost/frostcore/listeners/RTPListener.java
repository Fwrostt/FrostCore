package dev.frost.frostcore.listeners;

import dev.frost.frostcore.rtp.RTPService;
import dev.frost.frostcore.rtp.RTPStateTracker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Event listener for RTP edge-case handling.
 * <p>
 * Movement cancellation uses {@link PlayerMoveEvent} with block-position
 * comparison to avoid false cancels from head rotation.
 */
public class RTPListener implements Listener {

    private final RTPService rtpService;
    private final RTPStateTracker stateTracker;

    public RTPListener(RTPService rtpService) {
        this.rtpService = rtpService;
        this.stateTracker = rtpService.getStateTracker();
    }

    /**
     * Cancel RTP on logout — prevents dangling async tasks.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (stateTracker.isPending(player.getUniqueId())) {
            rtpService.cancelRTP(player, false);
        }
    }

    /**
     * Cancel RTP on death — player shouldn't teleport after dying.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (stateTracker.isPending(player.getUniqueId())) {
            rtpService.cancelRTP(player, false);
        }
    }

    /**
     * Cancel warmup if player changes world (e.g. enters a portal).
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (stateTracker.hasWarmup(player.getUniqueId())) {
            rtpService.cancelRTP(player, true);
        }
    }

    /**
     * Cancel warmup on block-level movement.
     * Uses block position comparison only — head rotation does NOT cancel.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!rtpService.getConfig().isCancelOnMove()) return;
        if (!stateTracker.hasWarmup(event.getPlayer().getUniqueId())) return;

        // Block-position comparison — ignore sub-block movement and head rotation
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
         || event.getFrom().getBlockY() != event.getTo().getBlockY()
         || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            rtpService.cancelRTP(event.getPlayer(), true);
        }
    }
}
