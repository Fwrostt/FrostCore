package dev.frost.frostcore.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;


public class ClickContext {

    private final InventoryClickEvent event;
    private final Gui gui;

    ClickContext(InventoryClickEvent event, Gui gui) {
        this.event = event;
        this.gui = gui;
    }

    
    public Player getPlayer() {
        return (Player) event.getWhoClicked();
    }

    
    public int getSlot() {
        return event.getSlot();
    }

    
    public int getRow() {
        return event.getSlot() / 9;
    }

    
    public int getCol() {
        return event.getSlot() % 9;
    }

    public ClickType getClickType() { return event.getClick(); }
    public boolean isLeftClick()    { return event.isLeftClick(); }
    public boolean isRightClick()   { return event.isRightClick(); }
    public boolean isShiftClick()   { return event.isShiftClick(); }
    public boolean isMiddleClick()  { return event.getClick() == ClickType.MIDDLE; }
    public boolean isNumberKey()    { return event.getClick() == ClickType.NUMBER_KEY; }

    
    public void cancel() {
        event.setCancelled(true);
    }

    
    public void close() {
        event.getWhoClicked().closeInventory();
    }

    
    public void openGui(Gui newGui) {
        GuiManager.schedule(() -> newGui.open(getPlayer()));
    }

    
    public InventoryClickEvent getEvent() { return event; }

    
    public Gui getGui() { return gui; }
}

