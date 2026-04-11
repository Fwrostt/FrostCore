package dev.frost.frostcore.listeners;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.TeamEchestManager;
import dev.frost.frostcore.manager.TeamManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Handles echest inventory events:
 * - Saves contents on close
 * - Blocks interaction for players who are no longer in the team (anti-dupe)
 * - Blocks drag events from non-members (anti-dupe)
 */
public class InventoryCloseListener implements Listener {

    private final TeamEchestManager echestManager = Main.getEchestManager();
    private final TeamManager teamManager = TeamManager.getInstance();

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (TeamEchestManager.isViewingEchest(player)) {
            echestManager.handleClose(player);
        }
    }

    /**
     * Anti-dupe: Block clicks in the echest if the player is no longer in a team.
     * This catches the edge case where a player is kicked/leaves while the echest is still open.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!TeamEchestManager.isViewingEchest(player)) return;

        if (!teamManager.hasTeam(player.getUniqueId())) {
            event.setCancelled(true);
            player.closeInventory();
            player.sendMessage(Main.getMessageManager().getComponent("teams.no-team"));
        }
    }

    /**
     * Anti-dupe: Block drag events in the echest if the player is no longer in a team.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!TeamEchestManager.isViewingEchest(player)) return;

        if (!teamManager.hasTeam(player.getUniqueId())) {
            event.setCancelled(true);
            player.closeInventory();
        }
    }
}

