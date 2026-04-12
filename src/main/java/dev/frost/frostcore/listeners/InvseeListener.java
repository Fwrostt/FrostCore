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


public class InvseeListener implements Listener {

    
    private static final ConcurrentHashMap<UUID, InvseeGui> activeSessions = new ConcurrentHashMap<>();

    
    public static void register(UUID viewerUUID, InvseeGui gui) {
        activeSessions.put(viewerUUID, gui);
    }

    
    public static void unregister(UUID viewerUUID) {
        activeSessions.remove(viewerUUID);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;

        InvseeGui gui = activeSessions.get(viewer.getUniqueId());
        if (gui == null) return;

        
        if (event.getClickedInventory() == null) return;

        
        if (!event.getClickedInventory().equals(gui.getInventory())) {
            
            
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        int slot = event.getSlot();

        
        if (slot == InvseeGui.SLOT_CLOSE) {
            event.setCancelled(true);
            viewer.closeInventory();
            return;
        }

        
        if (gui.isLockedSlot(slot)) {
            event.setCancelled(true);
            return;
        }

        
        scheduleSync(gui);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;

        InvseeGui gui = activeSessions.get(viewer.getUniqueId());
        if (gui == null) return;

        int guiSize = gui.getInventory().getSize();

        
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < guiSize && gui.isLockedSlot(rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }

        
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

        
        gui.syncToPlayer();
    }

    
    private void scheduleSync(InvseeGui gui) {
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), gui::syncToPlayer, 1L);
    }
}
