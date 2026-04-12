package dev.frost.frostcore.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public final class GuiTemplate {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private GuiTemplate() {}

    
    public static GuiItem filler() {
        return Button.of(Material.GRAY_STAINED_GLASS_PANE)
                .name("<gray> ")
                .build();
    }

    
    public static GuiItem filler(Material material) {
        return Button.of(material)
                .name("<gray> ")
                .build();
    }

    
    public static GuiItem blackFiller() {
        return Button.of(Material.BLACK_STAINED_GLASS_PANE)
                .name("<gray> ")
                .build();
    }

    
    public static GuiItem prevButton(int currentPage, int totalPages,
                                     GuiAction<ClickContext> onClick) {
        return Button.of(Material.SPECTRAL_ARROW)
                .name("<gradient:#6B8DAE:#8BADC4>◀ Previous Page")
                .lore(
                    "<dark_gray>Page <white>" + currentPage + "<dark_gray> of <white>" + totalPages,
                    "",
                    "<gray>Click to go back"
                )
                .onClick(onClick)
                .build();
    }

    
    public static GuiItem nextButton(int currentPage, int totalPages,
                                     GuiAction<ClickContext> onClick) {
        return Button.of(Material.ARROW)
                .name("<gradient:#6B8DAE:#8BADC4>Next Page ▶")
                .lore(
                    "<dark_gray>Page <white>" + (currentPage + 2) + "<dark_gray> of <white>" + totalPages,
                    "",
                    "<gray>Click to continue"
                )
                .onClick(onClick)
                .build();
    }

    
    public static GuiItem prevFrameButton(int currentFrame, int totalFrames,
                                          GuiAction<ClickContext> onClick) {
        return Button.of(Material.SPECTRAL_ARROW)
                .name("<gradient:#C8A87C:#A68B5B>◀ Previous")
                .lore(
                    "<dark_gray>" + (currentFrame + 1) + " <gray>/ <dark_gray>" + totalFrames,
                    "<gray>Click to go back"
                )
                .onClick(onClick)
                .build();
    }

    
    public static GuiItem nextFrameButton(int currentFrame, int totalFrames,
                                          GuiAction<ClickContext> onClick) {
        return Button.of(Material.ARROW)
                .name("<gradient:#C8A87C:#A68B5B>Next ▶")
                .lore(
                    "<dark_gray>" + (currentFrame + 1) + " <gray>/ <dark_gray>" + totalFrames,
                    "<gray>Click to continue"
                )
                .onClick(onClick)
                .build();
    }

    
    public static GuiItem closeButton() {
        return Button.of(Material.BARRIER)
                .name("<red><bold>✗ Close")
                .lore("<gray>Close this menu")
                .onClick(ClickContext::close)
                .build();
    }

    
    public static SimpleGui confirm(String title,
                                    GuiAction<ClickContext> onConfirm,
                                    GuiAction<ClickContext> onCancel) {
        GuiItem confirmBtn = Button.of(Material.LIME_DYE)
                .name("<green><bold>✔ Confirm")
                .lore("<gray>Click to confirm")
                .glow()
                .onClick(onConfirm)
                .build();

        GuiItem cancelBtn = Button.of(Material.RED_DYE)
                .name("<red><bold>✗ Cancel")
                .lore("<gray>Click to cancel")
                .glow()
                .onClick(onCancel)
                .build();

        return SimpleGui.builder(title, 3)
                .border(filler())
                .item(1, 2, confirmBtn)
                .item(1, 3, confirmBtn)
                .item(1, 4, filler(Material.WHITE_STAINED_GLASS_PANE))
                .item(1, 5, cancelBtn)
                .item(1, 6, cancelBtn)
                .build();
    }

    
    public static GuiItem info(Material material, String name, String... lore) {
        return Button.of(material).name(name).lore(lore).build();
    }

    
    public static GuiItem empty() {
        return new GuiItem(new ItemStack(Material.AIR));
    }

    
    public static GuiItem pageIndicator(int current, int total) {
        return Button.of(Material.PAPER)
                .name("<white>Page <gold>" + (current + 1) + "<white> / <gold>" + total)
                .build();
    }
}

