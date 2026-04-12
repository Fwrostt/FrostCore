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


public abstract class Gui {

    protected static final MiniMessage MM = MiniMessage.miniMessage();

    protected final Inventory inventory;
    protected final int rows;
    protected final Component title;

    
    protected final Map<Integer, GuiItem> items = new LinkedHashMap<>();

    
    private boolean cancelAll = true;

    private GuiAction<Player> openAction;
    private GuiAction<Player> closeAction;

    protected Gui(Component title, int rows) {
        this.title = title;
        this.rows = Math.max(1, Math.min(6, rows));
        this.inventory = Bukkit.createInventory(null, this.rows * 9, title);
    }

    
    public abstract void populate();

    
    public void open(Player player) {
        clear();
        populate();
        GuiManager.track(player, this);
        player.openInventory(inventory);
        if (openAction != null) openAction.execute(player);
    }

    
    public void close(Player player) {
        player.closeInventory();
    }

    
    public void refresh(Player player) {
        clear();
        populate();
    }

    
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

    
    protected void setItem(int row, int col, GuiItem guiItem) {
        setItem(Slot.of(row, col), guiItem);
    }

    
    protected void removeItem(int slot) {
        items.remove(slot);
        inventory.setItem(slot, null);
    }

    
    protected void fill(GuiItem item) {
        for (int i = 0; i < rows * 9; i++) {
            if (!items.containsKey(i)) {
                setItem(i, item);
            }
        }
    }

    
    protected void fillBorder(GuiItem item) {
        if (!dev.frost.frostcore.Main.getConfigManager().getBoolean("gui.borders", true)) return;
        for (int slot : Slot.borderSlots(rows)) {
            if (!items.containsKey(slot)) {
                setItem(slot, item);
            }
        }
    }

    
    protected void forceFillBorder(GuiItem item) {
        if (!dev.frost.frostcore.Main.getConfigManager().getBoolean("gui.borders", true)) return;
        for (int slot : Slot.borderSlots(rows)) {
            setItem(slot, item);
        }
    }

    
    public void updateItem(int slot, GuiItem guiItem) {
        setItem(slot, guiItem);
    }

    
    public void updateItem(int row, int col, GuiItem guiItem) {
        setItem(Slot.of(row, col), guiItem);
    }

    
    protected void clear() {
        items.clear();
        inventory.clear();
    }

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

    
    public void setCancelAll(boolean cancelAll) { this.cancelAll = cancelAll; }

    
    public void setOnOpen(GuiAction<Player> action) { this.openAction = action; }

    
    public void setOnClose(GuiAction<Player> action) { this.closeAction = action; }

    public Inventory getInventory() { return inventory; }
    public Component getTitle()     { return title; }
    public int getRows()            { return rows; }
    public int getSize()            { return rows * 9; }
    public boolean isCancelAll()    { return cancelAll; }

    
    public GuiItem getItem(int slot) { return items.get(slot); }
}

