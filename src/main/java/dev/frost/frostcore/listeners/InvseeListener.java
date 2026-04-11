package dev.frost.frostcore.listeners;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.gui.impls.InvseeGui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener for InvSee++ GUI interactions.
 * Handles click routing, slot locking, and real-time inventory syncing.
 */
public class InvseeListener implements Listener {

    /** Active InvSee sessions: viewer UUID → their InvseeGui instance. */
    private static final ConcurrentHashMap<UUID, InvseeGui> activeSessions = new ConcurrentHashMap<>();

    /**
     * Register a viewer as having an InvSee GUI open.
     */
    public static void register(UUID viewerUUID, InvseeGui gui) {
        activeSessions.put(viewerUUID, gui);
    }

    /**
     * Unregister a viewer's InvSee session.
     */
    public static void unregister(UUID viewerUUID) {
        activeSessions.remove(viewerUUID);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;

        InvseeGui gui = activeSessions.get(viewer.getUniqueId());
        if (gui == null) return;

        // Ignore clicks outside the GUI
        if (event.getClickedInventory() == null) return;

        // If clicking in the viewer's own inventory (bottom half), allow shift-click only to interactive slots
        if (!event.getClickedInventory().equals(gui.getInventory())) {
            // Allow normal clicks in the viewer's own inventory
            // But block shift-clicks to prevent items going into locked slots
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        int slot = event.getSlot();

        // Close button
        if (slot == InvseeGui.SLOT_CLOSE) {
            event.setCancelled(true);
            viewer.closeInventory();
            return;
        }

        // Locked decoration slots - block all interaction
        if (gui.isLockedSlot(slot)) {
            event.setCancelled(true);
            return;
        }

        // Interactive slots — allow the click through, then sync after 1 tick
        scheduleSync(gui);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;

        InvseeGui gui = activeSessions.get(viewer.getUniqueId());
        if (gui == null) return;

        int guiSize = gui.getInventory().getSize();

        // Check if any dragged slots are locked
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < guiSize && gui.isLockedSlot(rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }

        // If drag touches GUI slots, schedule sync
        boolean touchesGui = event.getRawSlots().stream().anyMatch(s -> s < guiSize);
        if (touchesGui) {
            scheduleSync(gui);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player viewer)) return;

        InvseeGui gui = activeSessions.remove(viewer.getUniqueId());
        if (gui == null) return;

        // Final sync on close
        gui.syncToPlayer();
    }

    /**
     * Schedule a 1-tick delayed sync from GUI → target player.
     * The delay ensures Bukkit has finished processing the click event
     * before we read the modified inventory contents.
     */
    private void scheduleSync(InvseeGui gui) {
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), gui::syncToPlayer, 1L);
    }
}
