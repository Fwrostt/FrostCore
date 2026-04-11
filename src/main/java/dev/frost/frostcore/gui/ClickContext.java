package dev.frost.frostcore.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Rich wrapper around {@link InventoryClickEvent} passed to every {@link GuiAction}.
 * <p>
 * Provides convenient accessors for player, slot, click type and common
 * actions (cancel, close, swap GUI) so handler code stays clean.
 */
public class ClickContext {

    private final InventoryClickEvent event;
    private final Gui gui;

    ClickContext(InventoryClickEvent event, Gui gui) {
        this.event = event;
        this.gui = gui;
    }

    /** The player who clicked. */
    public Player getPlayer() {
        return (Player) event.getWhoClicked();
    }

    /** The slot index inside the GUI inventory that was clicked. */
    public int getSlot() {
        return event.getSlot();
    }

    /** Row of the clicked slot (0-indexed). */
    public int getRow() {
        return event.getSlot() / 9;
    }

    /** Column of the clicked slot (0-indexed). */
    public int getCol() {
        return event.getSlot() % 9;
    }

    public ClickType getClickType() { return event.getClick(); }
    public boolean isLeftClick()    { return event.isLeftClick(); }
    public boolean isRightClick()   { return event.isRightClick(); }
    public boolean isShiftClick()   { return event.isShiftClick(); }
    public boolean isMiddleClick()  { return event.getClick() == ClickType.MIDDLE; }
    public boolean isNumberKey()    { return event.getClick() == ClickType.NUMBER_KEY; }

    /**
     * Cancel the underlying event (prevents item movement).
     * This is often already done automatically — see {@link Gui#setCancelAll(boolean)}.
     */
    public void cancel() {
        event.setCancelled(true);
    }

    /** Close the player's currently open GUI. */
    public void close() {
        event.getWhoClicked().closeInventory();
    }

    /**
     * Safely open another GUI from within a click handler.
     * Schedules the open 1 tick later to avoid Bukkit inventory-switch quirks.
     */
    public void openGui(Gui newGui) {
        GuiManager.schedule(() -> newGui.open(getPlayer()));
    }

    /** The underlying Bukkit click event, for advanced use. */
    public InventoryClickEvent getEvent() { return event; }

    /** The GUI that was clicked. */
    public Gui getGui() { return gui; }
}

