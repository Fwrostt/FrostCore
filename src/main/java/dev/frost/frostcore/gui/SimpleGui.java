package dev.frost.frostcore.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A simple, single-page GUI built with a fluent builder.
 * <p>
 * Use the {@link Builder} for quick GUIs, or extend {@link Gui} directly
 * and override {@link #populate()} for more complex logic.
 *
 * <pre>{@code
 * SimpleGui gui = SimpleGui.builder("<gradient:#55CDFC:#7B68EE>My Menu", 3)
 *     .border(GuiTemplate.filler())
 *     .item(1, 4, Button.of(Material.NETHER_STAR)
 *         .name("<gold><bold>★ Options")
 *         .glow()
 *         .onClick(ctx -> ctx.getPlayer().sendMessage("Options!"))
 *         .build())
 *     .onClose(p -> p.sendMessage("Closed!"))
 *     .build();
 *
 * gui.open(player);
 * }</pre>
 */
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

    /**
     * Start building a {@link SimpleGui}.
     *
     * @param title MiniMessage-formatted title string
     * @param rows  Number of rows (1–6)
     */
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

        /** Place an item in the given flat slot. */
        public Builder item(int slot, GuiItem item) {
            configurators.add(gui -> gui.setItem(slot, item));
            return this;
        }

        /** Place an item at (row, col). */
        public Builder item(int row, int col, GuiItem item) {
            configurators.add(gui -> gui.setItem(row, col, item));
            return this;
        }

        /** Place items across an array of slot indices. */
        public Builder items(int[] slots, GuiItem item) {
            for (int slot : slots) {
                final int s = slot;
                configurators.add(gui -> gui.setItem(s, item));
            }
            return this;
        }

        /** Fill all empty slots with a filler item. */
        public Builder fill(GuiItem item) {
            configurators.add(gui -> gui.fill(item));
            return this;
        }

        /** Fill the border (top/bottom rows + left/right columns) with an item. */
        public Builder border(GuiItem item) {
            configurators.add(gui -> gui.fillBorder(item));
            return this;
        }

        /** Callback fired when the GUI opens for a player. */
        public Builder onOpen(GuiAction<Player> action) {
            this.openAction = action;
            return this;
        }

        /** Callback fired when the player closes the GUI. */
        public Builder onClose(GuiAction<Player> action) {
            this.closeAction = action;
            return this;
        }

        /**
         * Allow players to interact with (pick up / move) items in this GUI.
         * Disabled by default to prevent exploits. Use with caution.
         */
        public Builder allowInteraction() {
            this.cancelAll = false;
            return this;
        }

        /** Apply a custom populator function to this GUI at populate-time. */
        public Builder populate(Consumer<SimpleGui> configurator) {
            configurators.add(configurator);
            return this;
        }

        /** Build the {@link SimpleGui} instance. Does NOT open it. */
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

