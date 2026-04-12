package dev.frost.frostcore.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public final class GuiManager implements Listener {

    private static GuiManager instance;
    private static JavaPlugin plugin;

    private GuiManager() {}

    
    public static void init(JavaPlugin javaPlugin) {
        if (instance != null) return;
        plugin   = javaPlugin;
        instance = new GuiManager();
        javaPlugin.getServer().getPluginManager().registerEvents(instance, javaPlugin);
    }

    
    private final ConcurrentHashMap<UUID, Gui> openGuis = new ConcurrentHashMap<>();

    
    static void track(Player player, Gui gui) {
        requireInit();
        instance.openGuis.put(player.getUniqueId(), gui);
    }

    
    static void untrack(Player player) {
        if (instance != null) instance.openGuis.remove(player.getUniqueId());
    }

    
    public static Gui getOpenGui(Player player) {
        requireInit();
        return instance.openGuis.get(player.getUniqueId());
    }

    
    public static boolean hasOpenGui(Player player) {
        return instance != null && instance.openGuis.containsKey(player.getUniqueId());
    }

    
    public static void schedule(Runnable task) {
        requireInit();
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    
    public static void schedule(Runnable task, long delayTicks) {
        requireInit();
        plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Gui gui = openGuis.get(player.getUniqueId());
        if (gui == null) return;

        if (event.getClickedInventory() == null) return;

        if (event.getClickedInventory().equals(gui.getInventory())) {

            gui.handleClick(event);
            return;
        }

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
        }
    }

    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Gui gui = openGuis.get(player.getUniqueId());
        if (gui == null) return;

        int guiSize = gui.getInventory().getSize();

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < guiSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Gui gui = openGuis.get(player.getUniqueId());
        if (gui == null) return;

        if (!gui.getInventory().equals(event.getInventory())) return;

        openGuis.remove(player.getUniqueId());
        gui.handleClose(event);
    }

    private static void requireInit() {
        if (instance == null) {
            throw new IllegalStateException("GuiManager not initialised! Call GuiManager.init(plugin) in onEnable.");
        }
    }
}

