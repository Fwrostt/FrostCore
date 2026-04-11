package dev.frost.frostcore.gui;

import org.bukkit.inventory.ItemStack;

/**
 * Pairs an {@link ItemStack} (display) with an optional {@link GuiAction} (behaviour).
 * <p>
 * {@code GuiItem}s are the atoms of the GUI API — every slot in every GUI is
 * backed by one.  Use {@link Button} for a fluent builder, or construct directly
 * for simple static items.
 */
public class GuiItem {

    private ItemStack item;
    private GuiAction<ClickContext> action;

    // ── Constructors ─────────────────────────────────────────────────────────

    /** Display-only item — clicking does nothing. */
    public GuiItem(ItemStack item) {
        this.item = item;
        this.action = null;
    }

    /** Item with a click action. */
    public GuiItem(ItemStack item, GuiAction<ClickContext> action) {
        this.item = item;
        this.action = action;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public ItemStack getItem() { return item; }

    /**
     * Swap the display item. If the {@link GuiItem} is currently placed in an
     * open inventory, call {@link Gui#updateItem(int, GuiItem)} to reflect the change.
     */
    public void setItem(ItemStack item) { this.item = item; }

    public GuiAction<ClickContext> getAction() { return action; }
    public void setAction(GuiAction<ClickContext> action) { this.action = action; }

    public boolean hasAction() { return action != null; }

    // ── Internal ─────────────────────────────────────────────────────────────

    void executeAction(ClickContext ctx) {
        if (action != null) action.execute(ctx);
    }
}
