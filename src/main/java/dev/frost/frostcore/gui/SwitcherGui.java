package dev.frost.frostcore.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An item <em>carousel</em> GUI that switches between discrete "frames" of items.
 * <p>
 * Each frame is an ordered array of {@link GuiItem}s that map 1-to-1 onto {@code displaySlots}.
 * Clicking the previous/next buttons replaces the displayed frame without closing the inventory.
 * <p>
 * Perfect for <strong>recipe viewers</strong>, equipment showcases, or any UI where you want
 * to flip through complete "pages" rather than a flat list.
 *
 * <h3>Recipe viewer example</h3>
 * <pre>{@code
 * SwitcherGui recipes = SwitcherGui.builder("<dark_blue><bold>⚗ Recipes", 5)
 *     .displaySlots(Slot.rectangle(1, 0, 3, 8))  // 27 display slots
 *     .prevSlot(Slot.of(4, 0))
 *     .nextSlot(Slot.of(4, 8))
 *     .border(GuiTemplate.filler())
 *     .build();
 *
 * recipes.addFrame(diamondSwordRecipe);   // GuiItem[]
 * recipes.addFrame(goldenAppleRecipe);
 * recipes.setAutoAdvance(60);             // auto-flip every 3 s (60 ticks)
 *
 * recipes.open(player);
 * }</pre>
 */
public class SwitcherGui extends Gui {

    /** Each frame is a GuiItem[] whose indices map to displaySlots. */
    private final List<GuiItem[]> frames = new ArrayList<>();
    private int currentFrame = 0;

    private int[] displaySlots;
    private int prevSlot;
    private int nextSlot;

    /** Item shown in display slots that have no corresponding frame item. */
    private GuiItem emptySlotItem = GuiTemplate.filler();

    /** Custom nav button providers. */
    private FrameButtonProvider prevButtonProvider;
    private FrameButtonProvider nextButtonProvider;

    /** Static items placed on every populate (e.g. border). */
    private Runnable decoration;

    private int autoAdvanceTicks = -1;
    private BukkitTask autoTask;
    private Player viewingPlayer;

    private SwitcherGui(Component title, int rows) {
        super(title, rows);
        displaySlots = Slot.rectangle(1, 0, rows - 2, 8);
        prevSlot  = Slot.bottomLeft(rows);
        nextSlot  = Slot.bottomRight(rows);
    }

    @Override
    public void populate() {
        clear();

        if (decoration != null) decoration.run();

        if (frames.isEmpty()) return;

        normaliseFrame();
        GuiItem[] frame = frames.get(currentFrame);

        for (int i = 0; i < displaySlots.length; i++) {
            GuiItem item = (i < frame.length && frame[i] != null) ? frame[i] : emptySlotItem;
            setItem(displaySlots[i], item);
        }

        if (frames.size() > 1) {
            setItem(prevSlot, resolvePrevButton());
            setItem(nextSlot, resolveNextButton());
        }
    }

    /**
     * Add a frame of items. Frame length should match (or be ≤) the number of display slots.
     * Excess slots will be filled with the {@link #emptySlotItem}.
     *
     * @param items Items for this frame (varargs — one per display slot).
     * @return {@code this} for chaining.
     */
    public SwitcherGui addFrame(GuiItem... items) {
        frames.add(Arrays.copyOf(items, items.length));
        return this;
    }

    /**
     * Add a frame from a list.
     */
    public SwitcherGui addFrame(List<GuiItem> items) {
        frames.add(items.toArray(new GuiItem[0]));
        return this;
    }

    /** Remove all frames. */
    public SwitcherGui clearFrames() {
        frames.clear();
        currentFrame = 0;
        return this;
    }

    /** Get the number of frames. */
    public int getFrameCount() { return frames.size(); }

    /** Get the currently shown frame index (0-based). */
    public int getCurrentFrame() { return currentFrame; }

    /**
     * Advance to the next frame (wraps around to 0 from the last frame).
     * Rebuilds the GUI in-place.
     */
    public void nextFrame(Player player) {
        if (frames.isEmpty()) return;
        currentFrame = (currentFrame + 1) % frames.size();
        refresh(player);
    }

    /**
     * Go to the previous frame (wraps around from 0 to last frame).
     * Rebuilds the GUI in-place.
     */
    public void prevFrame(Player player) {
        if (frames.isEmpty()) return;
        currentFrame = (currentFrame - 1 + frames.size()) % frames.size();
        refresh(player);
    }

    /**
     * Jump directly to a specific frame index.
     */
    public void setFrame(int frame, Player player) {
        if (frames.isEmpty()) return;
        currentFrame = Math.abs(frame % frames.size());
        refresh(player);
    }

    /**
     * Enable automatic frame advancement every {@code ticks} ticks.
     * The GUI must already be open when this is called.
     * <p>
     * To stop auto-advance, call {@link #stopAutoAdvance()}.
     *
     * @param ticks Ticks between frame changes (20 ticks = 1 second).
     * @param player The player viewing the GUI.
     */
    public void startAutoAdvance(int ticks, Player player) {
        stopAutoAdvance();
        this.autoAdvanceTicks  = ticks;
        this.viewingPlayer     = player;

        autoTask = dev.frost.frostcore.Main.getInstance()
                .getServer()
                .getScheduler()
                .runTaskTimer(dev.frost.frostcore.Main.getInstance(), () -> {
                    if (!player.isOnline() || GuiManager.getOpenGui(player) != this) {
                        stopAutoAdvance();
                        return;
                    }
                    nextFrame(player);
                }, ticks, ticks);
    }

    /** Stop the auto-advance task. Safe to call even when not running. */
    public void stopAutoAdvance() {
        if (autoTask != null && !autoTask.isCancelled()) {
            autoTask.cancel();
        }
        autoTask = null;
    }

    /**
     * Set the item shown in display slots that have no frame item.
     * Defaults to a gray glass pane filler.
     */
    public void setEmptySlotItem(GuiItem item) {
        this.emptySlotItem = item;
    }

    @Override
    void handleClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        stopAutoAdvance();
        super.handleClose(event);
    }

    private void normaliseFrame() {
        currentFrame = Math.max(0, Math.min(currentFrame, frames.size() - 1));
    }

    private GuiItem resolvePrevButton() {
        if (prevButtonProvider != null) return prevButtonProvider.get(currentFrame, frames.size());
        return GuiTemplate.prevFrameButton(currentFrame, frames.size(),
                ctx -> prevFrame(ctx.getPlayer()));
    }

    private GuiItem resolveNextButton() {
        if (nextButtonProvider != null) return nextButtonProvider.get(currentFrame, frames.size());
        return GuiTemplate.nextFrameButton(currentFrame, frames.size(),
                ctx -> nextFrame(ctx.getPlayer()));
    }

    @FunctionalInterface
    public interface FrameButtonProvider {
        GuiItem get(int currentFrame, int totalFrames);
    }

    /**
     * Start building a {@link SwitcherGui}.
     *
     * @param title MiniMessage title string
     * @param rows  Row count (2–6; at least 2 to fit content + nav)
     */
    public static Builder builder(String title, int rows) {
        return new Builder(MM.deserialize(title), Math.max(2, Math.min(6, rows)));
    }

    public static final class Builder {

        private final Component title;
        private final int rows;

        private int[] displaySlots;
        private int prevSlot = -1;
        private int nextSlot = -1;
        private FrameButtonProvider prevProvider;
        private FrameButtonProvider nextProvider;
        private GuiItem emptySlotItem;
        private Runnable decoration;
        private final List<GuiItem[]> initialFrames = new ArrayList<>();
        private int autoAdvanceTicks = -1;
        private GuiAction<Player> openAction;
        private GuiAction<Player> closeAction;

        private Builder(Component title, int rows) {
            this.title = title;
            this.rows  = rows;
        }

        /** Slots that will be swapped out per frame. */
        public Builder displaySlots(int... slots) {
            this.displaySlots = slots;
            return this;
        }

        /** Slot of the "previous frame" button. */
        public Builder prevSlot(int slot) {
            this.prevSlot = slot;
            return this;
        }

        /** Slot of the "next frame" button. */
        public Builder nextSlot(int slot) {
            this.nextSlot = slot;
            return this;
        }

        /** Override the "previous frame" button appearance. */
        public Builder prevButton(FrameButtonProvider provider) {
            this.prevProvider = provider;
            return this;
        }

        /** Override the "next frame" button appearance. */
        public Builder nextButton(FrameButtonProvider provider) {
            this.nextProvider = provider;
            return this;
        }

        /** Item shown where a frame has no item for a slot. */
        public Builder emptySlotItem(GuiItem item) {
            this.emptySlotItem = item;
            return this;
        }

        /** Static decoration applied on every populate (border, labels, etc.). */
        public Builder decorate(Runnable decorator) {
            this.decoration = decorator;
            return this;
        }

        /** Add an initial frame at construction time. */
        public Builder addFrame(GuiItem... items) {
            initialFrames.add(items);
            return this;
        }

        /** Auto-advance frames every N ticks. Call after open(). */
        public Builder autoAdvance(int ticks) {
            this.autoAdvanceTicks = ticks;
            return this;
        }

        public Builder onOpen(GuiAction<Player> action)  { this.openAction  = action; return this; }
        public Builder onClose(GuiAction<Player> action) { this.closeAction = action; return this; }

        public SwitcherGui build() {
            SwitcherGui gui = new SwitcherGui(title, rows);

            if (displaySlots != null)   gui.displaySlots         = displaySlots;
            if (prevSlot >= 0)          gui.prevSlot             = prevSlot;
            if (nextSlot >= 0)          gui.nextSlot             = nextSlot;
            if (prevProvider != null)   gui.prevButtonProvider   = prevProvider;
            if (nextProvider != null)   gui.nextButtonProvider   = nextProvider;
            if (emptySlotItem != null)  gui.emptySlotItem        = emptySlotItem;
            if (decoration != null)     gui.decoration           = decoration;
            if (openAction != null)     gui.setOnOpen(openAction);
            if (closeAction != null)    gui.setOnClose(closeAction);

            gui.frames.addAll(initialFrames);

            if (autoAdvanceTicks > 0) {
                int ticks = autoAdvanceTicks;
                GuiAction<Player> existing = openAction;
                gui.setOnOpen(p -> {
                    if (existing != null) existing.execute(p);
                    gui.startAutoAdvance(ticks, p);
                });
            }

            return gui;
        }
    }
}

