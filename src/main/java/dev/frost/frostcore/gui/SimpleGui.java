package dev.frost.frostcore.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


public class SimpleGui extends Gui {

    private final List<Consumer<SimpleGui>> populators = new ArrayList<>();

    private SimpleGui(Component title, int rows) {
        super(title, rows);
    }

    @Override
    public void populate() {
        for (Consumer<SimpleGui> pop : populators) {
            pop.accept(this);
        }
    }

    @Override
    public void setItem(int slot, GuiItem item) { super.setItem(slot, item); }

    @Override
    public void setItem(int row, int col, GuiItem item) { super.setItem(row, col, item); }

    @Override
    public void fill(GuiItem item) { super.fill(item); }

    @Override
    public void fillBorder(GuiItem item) { super.fillBorder(item); }

    
    public static Builder builder(String title, int rows) {
        return new Builder(MiniMessage.miniMessage().deserialize(title), rows);
    }

    public static final class Builder {

        private final Component title;
        private final int rows;
        private final List<Consumer<SimpleGui>> configurators = new ArrayList<>();
        private GuiAction<Player> openAction;
        private GuiAction<Player> closeAction;
        private boolean cancelAll = true;

        private Builder(Component title, int rows) {
            this.title = title;
            this.rows = Math.max(1, Math.min(6, rows));
        }

        
        public Builder item(int slot, GuiItem item) {
            configurators.add(gui -> gui.setItem(slot, item));
            return this;
        }

        
        public Builder item(int row, int col, GuiItem item) {
            configurators.add(gui -> gui.setItem(row, col, item));
            return this;
        }

        
        public Builder items(int[] slots, GuiItem item) {
            for (int slot : slots) {
                final int s = slot;
                configurators.add(gui -> gui.setItem(s, item));
            }
            return this;
        }

        
        public Builder fill(GuiItem item) {
            configurators.add(gui -> gui.fill(item));
            return this;
        }

        
        public Builder border(GuiItem item) {
            configurators.add(gui -> gui.fillBorder(item));
            return this;
        }

        
        public Builder onOpen(GuiAction<Player> action) {
            this.openAction = action;
            return this;
        }

        
        public Builder onClose(GuiAction<Player> action) {
            this.closeAction = action;
            return this;
        }

        
        public Builder allowInteraction() {
            this.cancelAll = false;
            return this;
        }

        
        public Builder populate(Consumer<SimpleGui> configurator) {
            configurators.add(configurator);
            return this;
        }

        
        public SimpleGui build() {
            SimpleGui gui = new SimpleGui(title, rows);
            gui.setCancelAll(cancelAll);
            if (openAction != null)  gui.setOnOpen(openAction);
            if (closeAction != null) gui.setOnClose(closeAction);
            gui.populators.addAll(configurators);
            return gui;
        }
    }
}

