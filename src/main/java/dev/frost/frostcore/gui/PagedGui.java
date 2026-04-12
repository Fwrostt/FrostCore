package dev.frost.frostcore.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


public class PagedGui extends Gui {

    private final List<GuiItem> contentItems = new ArrayList<>();
    private int currentPage = 0;

    
    private int[] contentSlots;

    
    private int prevSlot;

    
    private int nextSlot;

    
    private NavButtonProvider prevButtonProvider;
    private NavButtonProvider nextButtonProvider;

    
    private Runnable decoration;

    private PagedGui(Component title, int rows) {
        super(title, rows);

        contentSlots = Slot.rectangle(1, 0, rows - 2, 8);
        prevSlot  = Slot.bottomLeft(rows);
        nextSlot  = Slot.bottomRight(rows);
    }

    @Override
    public void populate() {
        clear();

        if (decoration != null) decoration.run();

        int pageSize   = contentSlots.length;
        int totalPages = Math.max(1, (int) Math.ceil((double) contentItems.size() / pageSize));
        currentPage    = Math.min(currentPage, totalPages - 1);

        int start = currentPage * pageSize;
        for (int i = 0; i < pageSize; i++) {
            int idx = start + i;
            if (idx < contentItems.size()) {
                setItem(contentSlots[i], contentItems.get(idx));
            }
        }

        if (currentPage > 0) {
            setItem(prevSlot, resolvePrevButton());
        }
        if (currentPage < totalPages - 1) {
            setItem(nextSlot, resolveNextButton(totalPages));
        }
    }

    
    public void nextPage(Player player) {
        int totalPages = getTotalPages();
        if (currentPage < totalPages - 1) {
            currentPage++;
            refresh(player);
        }
    }

    
    public void prevPage(Player player) {
        if (currentPage > 0) {
            currentPage--;
            refresh(player);
        }
    }

    
    public void setPage(int page, Player player) {
        currentPage = Math.max(0, Math.min(page, getTotalPages() - 1));
        refresh(player);
    }

    
    public int getCurrentPage() { return currentPage; }

    
    public int getTotalPages() {
        int pageSize = contentSlots.length;
        return Math.max(1, (int) Math.ceil((double) contentItems.size() / pageSize));
    }

    public PagedGui addItem(GuiItem item) {
        contentItems.add(item);
        return this;
    }

    public PagedGui addItems(Collection<GuiItem> items) {
        contentItems.addAll(items);
        return this;
    }

    public PagedGui clearContent() {
        contentItems.clear();
        currentPage = 0;
        return this;
    }

    public List<GuiItem> getContentItems() {
        return Collections.unmodifiableList(contentItems);
    }

    private GuiItem resolvePrevButton() {
        if (prevButtonProvider != null) return prevButtonProvider.get(currentPage, getTotalPages());
        return GuiTemplate.prevButton(currentPage, getTotalPages(), ctx -> prevPage(ctx.getPlayer()));
    }

    private GuiItem resolveNextButton(int totalPages) {
        if (nextButtonProvider != null) return nextButtonProvider.get(currentPage, totalPages);
        return GuiTemplate.nextButton(currentPage, totalPages, ctx -> nextPage(ctx.getPlayer()));
    }

    @FunctionalInterface
    public interface NavButtonProvider {
        GuiItem get(int currentPage, int totalPages);
    }

    
    public static Builder builder(String title, int rows) {
        return new Builder(MM.deserialize(title), Math.max(2, Math.min(6, rows)));
    }

    public static final class Builder {

        private final Component title;
        private final int rows;

        private int[] contentSlots;
        private int prevSlot = -1;
        private int nextSlot = -1;
        private NavButtonProvider prevProvider;
        private NavButtonProvider nextProvider;
        private final List<GuiItem> items = new ArrayList<>();
        private Runnable decoration;
        private GuiAction<Player> openAction;
        private GuiAction<Player> closeAction;

        private Builder(Component title, int rows) {
            this.title = title;
            this.rows  = rows;
        }

        
        public Builder contentSlots(int... slots) {
            this.contentSlots = slots;
            return this;
        }

        
        public Builder prevSlot(int slot) {
            this.prevSlot = slot;
            return this;
        }

        
        public Builder nextSlot(int slot) {
            this.nextSlot = slot;
            return this;
        }

        
        public Builder prevButton(NavButtonProvider provider) {
            this.prevProvider = provider;
            return this;
        }

        
        public Builder nextButton(NavButtonProvider provider) {
            this.nextProvider = provider;
            return this;
        }

        
        public Builder items(List<GuiItem> items) {
            this.items.addAll(items);
            return this;
        }

        
        public Builder decorate(Runnable decorator) {
            this.decoration = decorator;
            return this;
        }

        
        public Builder border(GuiItem item) {
            return this;
        }

        public Builder onOpen(GuiAction<Player> action) { this.openAction = action; return this; }
        public Builder onClose(GuiAction<Player> action) { this.closeAction = action; return this; }

        public PagedGui build() {
            PagedGui gui = new PagedGui(title, rows);

            if (contentSlots != null) gui.contentSlots = contentSlots;
            if (prevSlot >= 0)        gui.prevSlot      = prevSlot;
            if (nextSlot >= 0)        gui.nextSlot      = nextSlot;
            if (prevProvider != null) gui.prevButtonProvider = prevProvider;
            if (nextProvider != null) gui.nextButtonProvider = nextProvider;
            if (decoration != null)   gui.decoration    = decoration;
            if (openAction != null)   gui.setOnOpen(openAction);
            if (closeAction != null)  gui.setOnClose(closeAction);

            gui.contentItems.addAll(items);
            return gui;
        }
    }
}

