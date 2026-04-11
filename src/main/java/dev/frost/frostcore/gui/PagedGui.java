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

/**
 * A scrollable, paginated GUI for displaying large lists of items.
 * <p>
 * Content items are placed into configurable <em>content slots</em>.
 * Navigation buttons are shown automatically in designated slots:
 * the "previous" button appears only when there's a prior page;
 * the "next" button only when there's a following page.
 * Both buttons display the current page number in their lore.
 *
 * <h3>Quick start</h3>
 * <pre>{@code
 * PagedGui shop = PagedGui.builder("<gradient:#FFD700:#FFA500>Shop", 6)
 *     .items(shopItems)                 // List<GuiItem>
 *     .border(GuiTemplate.filler())
 *     .build();
 *
 * shop.open(player);
 * }</pre>
 *
 * <h3>Custom layout</h3>
 * <pre>{@code
 * PagedGui gui = PagedGui.builder("<blue>Custom Layout", 5)
 *     .contentSlots(Slot.rectangle(1, 1, 3, 7))  // 3×7 inner area
 *     .prevSlot(Slot.of(4, 0))
 *     .nextSlot(Slot.of(4, 8))
 *     .items(myItems)
 *     .build();
 * }</pre>
 */
public class PagedGui extends Gui {

    private final List<GuiItem> contentItems = new ArrayList<>();
    private int currentPage = 0;

    /** Slots that show paginated content (ordered). */
    private int[] contentSlots;

    /** Slot for the "previous page" button. */
    private int prevSlot;

    /** Slot for the "next page" button. */
    private int nextSlot;

    /** Custom nav button providers (or null → use GuiTemplate defaults). */
    private NavButtonProvider prevButtonProvider;
    private NavButtonProvider nextButtonProvider;

    /** Optional decorations applied once per populate (e.g. border fill). */
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

    /**
     * Advance to the next page. Rebuilds the GUI in-place.
     * Does nothing if already on the last page.
     */
    public void nextPage(Player player) {
        int totalPages = getTotalPages();
        if (currentPage < totalPages - 1) {
            currentPage++;
            refresh(player);
        }
    }

    /**
     * Return to the previous page. Rebuilds the GUI in-place.
     * Does nothing if already on page 0.
     */
    public void prevPage(Player player) {
        if (currentPage > 0) {
            currentPage--;
            refresh(player);
        }
    }

    /** Jump directly to a page by index (0-based). */
    public void setPage(int page, Player player) {
        currentPage = Math.max(0, Math.min(page, getTotalPages() - 1));
        refresh(player);
    }

    /** Current page index (0-based). */
    public int getCurrentPage() { return currentPage; }

    /** Total number of pages given current content. */
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

    /**
     * Start building a {@link PagedGui}.
     *
     * @param title MiniMessage title string
     * @param rows  Number of rows (2–6; at least 2 needed for nav buttons)
     */
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

        /** Define which slots are used for content (overrides default). */
        public Builder contentSlots(int... slots) {
            this.contentSlots = slots;
            return this;
        }

        /** Slot for the "previous" nav button (overrides default bottom-left). */
        public Builder prevSlot(int slot) {
            this.prevSlot = slot;
            return this;
        }

        /** Slot for the "next" nav button (overrides default bottom-right). */
        public Builder nextSlot(int slot) {
            this.nextSlot = slot;
            return this;
        }

        /** Override the default "previous" button. */
        public Builder prevButton(NavButtonProvider provider) {
            this.prevProvider = provider;
            return this;
        }

        /** Override the default "next" button. */
        public Builder nextButton(NavButtonProvider provider) {
            this.nextProvider = provider;
            return this;
        }

        /** Pre-load a list of content items. */
        public Builder items(List<GuiItem> items) {
            this.items.addAll(items);
            return this;
        }

        /**
         * Apply decoration (border, static items, etc.) before content is rendered.
         * The runnable receives the GUI via closure — capture a reference externally.
         */
        public Builder decorate(Runnable decorator) {
            this.decoration = decorator;
            return this;
        }

        /** Border fill with the given {@link GuiItem}. Convenience shorthand. */
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

