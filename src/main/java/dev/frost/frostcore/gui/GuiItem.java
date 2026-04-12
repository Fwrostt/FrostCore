package dev.frost.frostcore.gui;

import org.bukkit.inventory.ItemStack;


public class GuiItem {

    private ItemStack item;
    private GuiAction<ClickContext> action;

    
    public GuiItem(ItemStack item) {
        this.item = item;
        this.action = null;
    }

    
    public GuiItem(ItemStack item, GuiAction<ClickContext> action) {
        this.item = item;
        this.action = action;
    }

    public ItemStack getItem() { return item; }

    
    public void setItem(ItemStack item) { this.item = item; }

    public GuiAction<ClickContext> getAction() { return action; }
    public void setAction(GuiAction<ClickContext> action) { this.action = action; }

    public boolean hasAction() { return action != null; }

    void executeAction(ClickContext ctx) {
        if (action != null) action.execute(ctx);
    }
}

