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

/**
 * Central event hub for the FrostCore GUI API.
 * <p>
 * Listens for all inventory events and routes them to the appropriate
 * open {@link Gui} instance.  Also acts as the registry for which player
 * has which GUI open.
 *
 * <h3>Setup</h3>
 * Call {@link #init(JavaPlugin)} once from your plugin's {@code onEnable}:
 * <pre>{@code
 * GuiManager.init(this);
 * }</pre>
 */
public final class GuiManager implements Listener {

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static GuiManager instance;
    private static JavaPlugin plugin;

    private GuiManager() {}

    /**
     * Initialise the GUI system.  Must be called before any GUI is opened.
     *
     * @param plugin Your plugin instance.
     */
    public static void init(JavaPlugin javaPlugin) {
        if (instance != null) return;   // idempotent
        plugin   = javaPlugin;
        instance = new GuiManager();
        javaPlugin.getServer().getPluginManager().registerEvents(instance, javaPlugin);
    }

    // ── Open-GUI registry ─────────────────────────────────────────────────────

    /** Thread-safe map: player UUID → their open Gui. */
    private final ConcurrentHashMap<UUID, Gui> openGuis = new ConcurrentHashMap<>();

    /** Track a player as viewing the given GUI. Package-private — called by {@link Gui#open}. */
    static void track(Player player, Gui gui) {
        requireInit();
        instance.openGuis.put(player.getUniqueId(), gui);
    }

    /** Untrack a player (they have closed their GUI). Package-private. */
    static void untrack(Player player) {
        if (instance != null) instance.openGuis.remove(player.getUniqueId());
    }

    /**
     * Get the GUI currently open for a player, or {@code null}.
     */
    public static Gui getOpenGui(Player player) {
        requireInit();
        return instance.openGuis.get(player.getUniqueId());
    }

    /** Returns true if the player currently has a FrostCore GUI open. */
    public static boolean hasOpenGui(Player player) {
        return instance != null && instance.openGuis.containsKey(player.getUniqueId());
    }

    // ── Scheduling helper ─────────────────────────────────────────────────────

    /**
     * Schedule a task to run on the main thread on the next tick.
     * Used by {@link ClickContext#openGui(Gui)} to safely swap GUIs from
     * within a click handler without triggering Bukkit inventory bugs.
     */
    public static void schedule(Runnable task) {
        requireInit();
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    /**
     * Schedule a task to run on the main thread after {@code delayTicks} ticks.
     */
    public static void schedule(Runnable task, long delayTicks) {
        requireInit();
        plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Gui gui = openGuis.get(player.getUniqueId());
        if (gui == null) return;

        // Only intercept clicks inside the GUI's top inventory
        if (event.getClickedInventory() == null) return;

        if (event.getClickedInventory().equals(gui.getInventory())) {
            // Direct click inside the GUI — dispatch to the Gui
            gui.handleClick(event);
            return;
        }

        // Click was in the player's own inventory while a GUI is open.
        // Block shift-clicks that would move items INTO the GUI (anti-dupe).
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
        }
    }

    /**
     * Block ALL drag events that touch any slot inside an open GUI inventory.
     * This prevents splitting/spreading stacks into GUI slots.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Gui gui = openGuis.get(player.getUniqueId());
        if (gui == null) return;

        int guiSize = gui.getInventory().getSize();
        // rawSlots 0..guiSize-1 are inside the top (GUI) inventory
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < guiSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * On inventory close: fire the Gui's close callback and untrack the player.
     * <p>
     * Guards against the scenario where {@link Gui#open(Player)} replaces one
     * GUI with another — it checks that the closed inventory matches the
     * currently tracked GUI before firing the close action.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Gui gui = openGuis.get(player.getUniqueId());
        if (gui == null) return;

        // Only fire close logic if THIS inventory is what's tracked
        // (prevents firing when the player swaps to a different FrostCore GUI)
        if (!gui.getInventory().equals(event.getInventory())) return;

        openGuis.remove(player.getUniqueId());
        gui.handleClose(event);
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private static void requireInit() {
        if (instance == null) {
            throw new IllegalStateException("GuiManager not initialised! Call GuiManager.init(plugin) in onEnable.");
        }
    }
}
