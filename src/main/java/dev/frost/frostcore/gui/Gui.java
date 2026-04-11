package dev.frost.frostcore.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Abstract base class for all GUI types in FrostCore.
 * <p>
 * Extend this class (or use {@link SimpleGui}'s builder) to create custom GUIs.
 * Override {@link #populate()} to place items into the inventory.
 *
 * <pre>{@code
 * public class MyGui extends Gui {
 *
 *     public MyGui() {
 *         super(MiniMessage.miniMessage().deserialize("<gold>My GUI"), 3);
 *     }
 *
 *     @Override
 *     public void populate() {
 *         fillBorder(GuiTemplate.filler());
 *         setItem(1, 4, Button.of(Material.DIAMOND)
 *             .name("<aqua>Hello!")
 *             .build());
 *     }
 * }
 * }</pre>
 */
public abstract class Gui {

    protected static final MiniMessage MM = MiniMessage.miniMessage();

    // ── Inventory state ───────────────────────────────────────────────────────

    protected final Inventory inventory;
    protected final int rows;
    protected final Component title;

    /** Slot → GuiItem mapping. Drives both rendering and click dispatch. */
    protected final Map<Integer, GuiItem> items = new LinkedHashMap<>();

    // ── Config ────────────────────────────────────────────────────────────────

    /** Whether all inventory interactions are cancelled by default. Recommended: true (anti-dupe). */
    private boolean cancelAll = true;

    // ── Lifecycle callbacks ───────────────────────────────────────────────────

    private GuiAction<Player> openAction;
    private GuiAction<Player> closeAction;

    // ── Constructor ──────────────────────────────────────────────────────────

    protected Gui(Component title, int rows) {
        this.title = title;
        this.rows = Math.max(1, Math.min(6, rows));
        this.inventory = Bukkit.createInventory(null, this.rows * 9, title);
    }

    // ── Abstract lifecycle ───────────────────────────────────────────────────

    /**
     * Populate the GUI with items.
     * <p>
     * Called automatically before each {@link #open(Player)} and {@link #refresh(Player)}.
     * Override this to define your GUI's layout.
     */
    public abstract void populate();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Open this GUI for the given player.
     * Automatically calls {@link #populate()} first.
     */
    public void open(Player player) {
        clear();
        populate();
        GuiManager.track(player, this);
        player.openInventory(inventory);
        if (openAction != null) openAction.execute(player);
    }

    /**
     * Close this GUI for the given player.
     * Fires the close action and triggers the normal {@link InventoryCloseEvent}.
     */
    public void close(Player player) {
        player.closeInventory();
    }

    /**
     * Re-populate and re-render this GUI for an already-viewing player.
     * Items are cleared first, then {@link #populate()} is called again.
     */
    public void refresh(Player player) {
        clear();
        populate();
    }

    // ── Item placement ───────────────────────────────────────────────────────

    /**
     * Place a {@link GuiItem} in the given flat slot index.
     *
     * @param slot    slot index (0 = top-left)
     * @param guiItem the item to place; {@code null} clears the slot
     */
    protected void setItem(int slot, GuiItem guiItem) {
        if (slot < 0 || slot >= rows * 9) return;
        if (guiItem == null) {
            items.remove(slot);
            inventory.setItem(slot, null);
        } else {
            items.put(slot, guiItem);
            inventory.setItem(slot, guiItem.getItem());
        }
    }

    /**
     * Place a {@link GuiItem} using (row, col) coordinates.
     *
     * @param row     0-indexed row
     * @param col     0-indexed column (0–8)
     * @param guiItem the item to place
     */
    protected void setItem(int row, int col, GuiItem guiItem) {
        setItem(Slot.of(row, col), guiItem);
    }

    /**
     * Remove the item in the given slot.
     */
    protected void removeItem(int slot) {
        items.remove(slot);
        inventory.setItem(slot, null);
    }

    /**
     * Fill all EMPTY slots with the given item.
     * Existing items are not overwritten.
     */
    protected void fill(GuiItem item) {
        for (int i = 0; i < rows * 9; i++) {
            if (!items.containsKey(i)) {
                setItem(i, item);
            }
        }
    }

    /**
     * Fill the border slots (top row, bottom row, left column, right column)
     * with the given item. Skips slots that already contain an item.
     */
    protected void fillBorder(GuiItem item) {
        for (int slot : Slot.borderSlots(rows)) {
            if (!items.containsKey(slot)) {
                setItem(slot, item);
            }
        }
    }

    /**
     * Fill all border slots unconditionally (overwrites existing items).
     */
    protected void forceFillBorder(GuiItem item) {
        for (int slot : Slot.borderSlots(rows)) {
            setItem(slot, item);
        }
    }

    /**
     * Update a single slot without rebuilding the whole GUI.
     * Useful for live slot state changes (e.g. toggle button).
     */
    public void updateItem(int slot, GuiItem guiItem) {
        setItem(slot, guiItem);
    }

    /**
     * Update a single slot using (row, col) coordinates.
     */
    public void updateItem(int row, int col, GuiItem guiItem) {
        setItem(Slot.of(row, col), guiItem);
    }

    /** Clear all items from the GUI (both the map and the backing inventory). */
    protected void clear() {
        items.clear();
        inventory.clear();
    }

    // ── Event dispatch (package-private, used by GuiManager) ─────────────────

    void handleClick(InventoryClickEvent event) {
        event.setCancelled(cancelAll);

        int slot = event.getSlot();
        GuiItem item = items.get(slot);
        if (item != null && item.hasAction()) {
            item.executeAction(new ClickContext(event, this));
        }
    }

    void handleClose(InventoryCloseEvent event) {
        if (closeAction != null) closeAction.execute((Player) event.getPlayer());
    }

    // ── Configuration ─────────────────────────────────────────────────────────

    /**
     * Set whether all inventory click interactions inside this GUI are
     * automatically cancelled (default: {@code true}).
     * <p>
     * Keeping this {@code true} is strongly recommended to prevent item duplication.
     */
    public void setCancelAll(boolean cancelAll) { this.cancelAll = cancelAll; }

    /**
     * Register a callback to fire when the GUI is opened for a player.
     */
    public void setOnOpen(GuiAction<Player> action) { this.openAction = action; }

    /**
     * Register a callback to fire when the GUI is closed (manually or programmatically).
     */
    public void setOnClose(GuiAction<Player> action) { this.closeAction = action; }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public Inventory getInventory() { return inventory; }
    public Component getTitle()     { return title; }
    public int getRows()            { return rows; }
    public int getSize()            { return rows * 9; }
    public boolean isCancelAll()    { return cancelAll; }

    /** Get the {@link GuiItem} at a slot, or {@code null} if empty. */
    public GuiItem getItem(int slot) { return items.get(slot); }
}
