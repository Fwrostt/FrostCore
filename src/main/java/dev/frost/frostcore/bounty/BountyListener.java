package dev.frost.frostcore.bounty;

import dev.frost.frostcore.bounty.model.Bounty;
import dev.frost.frostcore.bounty.model.BountyContributor;
import dev.frost.frostcore.utils.EconomyUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Player;

/**
 * Listens to {@link PlayerDeathEvent} and triggers bounty claim logic.
 *
 * <p>Priority is MONITOR so we run after other plugins have had a chance to cancel
 * or modify the event. We never cancel the event ourselves.</p>
 */
public class BountyListener implements Listener {

    private final BountyManager manager;
    private final BountyService service;

    public BountyListener(BountyManager manager, BountyService service) {
        this.manager = manager;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!manager.isEnabled() || !EconomyUtil.isEnabled()) return;

        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // No killer, environment kill, or suicide — skip
        if (killer == null || killer.equals(victim)) return;

        // Delegate to service (main-thread entry, async work inside)
        service.tryClaimBounty(killer, victim);
    }
}
