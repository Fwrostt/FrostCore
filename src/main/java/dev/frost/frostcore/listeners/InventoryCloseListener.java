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

