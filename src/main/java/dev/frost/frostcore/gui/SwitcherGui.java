package dev.frost.frostcore.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class SwitcherGui extends Gui {

    
    private final List<GuiItem[]> frames = new ArrayList<>();
    private int currentFrame = 0;

    private int[] displaySlots;
    private int prevSlot;
    private int nextSlot;

    
    private GuiItem emptySlotItem = GuiTemplate.filler();

    
    private FrameButtonProvider prevButtonProvider;
    private FrameButtonProvider nextButtonProvider;

    
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

    
    public SwitcherGui addFrame(GuiItem... items) {
        frames.add(Arrays.copyOf(items, items.length));
        return this;
    }

    
    public SwitcherGui addFrame(List<GuiItem> items) {
        frames.add(items.toArray(new GuiItem[0]));
        return this;
    }

    
    public SwitcherGui clearFrames() {
        frames.clear();
        currentFrame = 0;
        return this;
    }

    
    public int getFrameCount() { return frames.size(); }

    
    public int getCurrentFrame() { return currentFrame; }

    
    public void nextFrame(Player player) {
        if (frames.isEmpty()) return;
        currentFrame = (currentFrame + 1) % frames.size();
        refresh(player);
    }

    
    public void prevFrame(Player player) {
        if (frames.isEmpty()) return;
        currentFrame = (currentFrame - 1 + frames.size()) % frames.size();
        refresh(player);
    }

    
    public void setFrame(int frame, Player player) {
        if (frames.isEmpty()) return;
        currentFrame = Math.abs(frame % frames.size());
        refresh(player);
    }

    
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

    
    public void stopAutoAdvance() {
        if (autoTask != null && !autoTask.isCancelled()) {
            autoTask.cancel();
        }
        autoTask = null;
    }

    
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

        
        public Builder displaySlots(int... slots) {
            this.displaySlots = slots;
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

        
        public Builder prevButton(FrameButtonProvider provider) {
            this.prevProvider = provider;
            return this;
        }

        
        public Builder nextButton(FrameButtonProvider provider) {
            this.nextProvider = provider;
            return this;
        }

        
        public Builder emptySlotItem(GuiItem item) {
            this.emptySlotItem = item;
            return this;
        }

        
        public Builder decorate(Runnable decorator) {
            this.decoration = decorator;
            return this;
        }

        
        public Builder addFrame(GuiItem... items) {
            initialFrames.add(items);
            return this;
        }

        
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

