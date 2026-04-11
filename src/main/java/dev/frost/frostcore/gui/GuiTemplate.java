package dev.frost.frostcore.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Design library for the FrostCore GUI API.
 * <p>
 * Provides pre-styled filler items, navigation buttons, and factory methods
 * for common GUI patterns (e.g. confirm dialogs) to keep your GUIs
 * visually consistent.
 *
 * <h3>Style guide applied here</h3>
 * <ul>
 *   <li>Filler — gray stained glass pane, no name</li>
 *   <li>Prev — arrow (spectral arrow) pointing left</li>
 *   <li>Next — arrow (arrow) pointing right</li>
 *   <li>Close — barrier block, red name</li>
 *   <li>Confirm (✔) — lime dye, green name</li>
 *   <li>Cancel (✗) — red dye, red name</li>
 * </ul>
 */
public final class GuiTemplate {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private GuiTemplate() {}

    // ── Filler items ──────────────────────────────────────────────────────────

    /** Gray stained glass pane with an empty name — the standard filler. */
    public static GuiItem filler() {
        return Button.of(Material.GRAY_STAINED_GLASS_PANE)
                .name("<gray> ")
                .build();
    }

    /** Filler using a custom material (no name, no action). */
    public static GuiItem filler(Material material) {
        return Button.of(material)
                .name("<gray> ")
                .build();
    }

    /** Black stained glass pane filler — useful for dark-themed GUIs. */
    public static GuiItem blackFiller() {
        return Button.of(Material.BLACK_STAINED_GLASS_PANE)
                .name("<gray> ")
                .build();
    }

    // ── Navigation buttons ────────────────────────────────────────────────────

    /**
     * "Previous page" button for {@link PagedGui}.
     *
     * @param currentPage Current page (0-based)
     * @param totalPages  Total page count
     * @param onClick     Click action (typically {@code ctx -> gui.prevPage(ctx.getPlayer())})
     */
    public static GuiItem prevButton(int currentPage, int totalPages,
                                     GuiAction<ClickContext> onClick) {
        return Button.of(Material.SPECTRAL_ARROW)
                .name("<gradient:#55CDFC:#7B68EE>◀ Previous Page")
                .lore(
                    "<dark_gray>Page <white>" + currentPage + "<dark_gray> of <white>" + totalPages,
                    "",
                    "<gray>Click to go back"
                )
                .onClick(onClick)
                .build();
    }

    /**
     * "Next page" button for {@link PagedGui}.
     *
     * @param currentPage Current page (0-based)
     * @param totalPages  Total page count
     * @param onClick     Click action (typically {@code ctx -> gui.nextPage(ctx.getPlayer())})
     */
    public static GuiItem nextButton(int currentPage, int totalPages,
                                     GuiAction<ClickContext> onClick) {
        return Button.of(Material.ARROW)
                .name("<gradient:#55CDFC:#7B68EE>Next Page ▶")
                .lore(
                    "<dark_gray>Page <white>" + (currentPage + 2) + "<dark_gray> of <white>" + totalPages,
                    "",
                    "<gray>Click to continue"
                )
                .onClick(onClick)
                .build();
    }

    /**
     * "Previous frame" button for {@link SwitcherGui}.
     *
     * @param currentFrame Current frame index (0-based)
     * @param totalFrames  Total number of frames
     * @param onClick      Click action
     */
    public static GuiItem prevFrameButton(int currentFrame, int totalFrames,
                                          GuiAction<ClickContext> onClick) {
        return Button.of(Material.SPECTRAL_ARROW)
                .name("<gradient:#FFD700:#FFA500>◀ Previous")
                .lore(
                    "<dark_gray>" + (currentFrame + 1) + " <gray>/ <dark_gray>" + totalFrames,
                    "<gray>Click to go back"
                )
                .onClick(onClick)
                .build();
    }

    /**
     * "Next frame" button for {@link SwitcherGui}.
     *
     * @param currentFrame Current frame index (0-based)
     * @param totalFrames  Total number of frames
     * @param onClick      Click action
     */
    public static GuiItem nextFrameButton(int currentFrame, int totalFrames,
                                          GuiAction<ClickContext> onClick) {
        return Button.of(Material.ARROW)
                .name("<gradient:#FFD700:#FFA500>Next ▶")
                .lore(
                    "<dark_gray>" + (currentFrame + 1) + " <gray>/ <dark_gray>" + totalFrames,
                    "<gray>Click to continue"
                )
                .onClick(onClick)
                .build();
    }

    /**
     * A close button that closes the viewer's GUI when clicked.
     * Place anywhere in your GUI layout.
     */
    public static GuiItem closeButton() {
        return Button.of(Material.BARRIER)
                .name("<red><bold>✗ Close")
                .lore("<gray>Close this menu")
                .onClick(ClickContext::close)
                .build();
    }

    // ── Confirm dialog ────────────────────────────────────────────────────────

    /**
     * Build a 3-row confirm/cancel dialog.
     *
     * <pre>{@code
     * GuiTemplate.confirm(
     *     "<red>⚠ Disband Team?",
     *     ctx -> {
     *         teamManager.disbandTeam(teamName);
     *         ctx.close();
     *     },
     *     ctx -> ctx.openGui(mainMenu)
     * ).open(player);
     * }</pre>
     *
     * @param title     MiniMessage title string
     * @param onConfirm Action to execute when the player clicks "Yes"
     * @param onCancel  Action to execute when the player clicks "No"
     * @return A ready-to-open {@link SimpleGui}
     */
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
                .item(1, 4, filler(Material.WHITE_STAINED_GLASS_PANE))   // divider
                .item(1, 5, cancelBtn)
                .item(1, 6, cancelBtn)
                .build();
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Create an informational display item with no click action.
     *
     * @param material  The icon material
     * @param name      MiniMessage name string
     * @param lore      MiniMessage lore lines
     */
    public static GuiItem info(Material material, String name, String... lore) {
        return Button.of(material).name(name).lore(lore).build();
    }

    /**
     * An invisible placeholder that occupies a slot but shows nothing.
     * Uses AIR — clicking does nothing.
     */
    public static GuiItem empty() {
        return new GuiItem(new ItemStack(Material.AIR));
    }

    /**
     * Build a compact "page indicator" item (not clickable).
     *
     * @param current 0-based current page
     * @param total   total pages
     */
    public static GuiItem pageIndicator(int current, int total) {
        return Button.of(Material.PAPER)
                .name("<white>Page <gold>" + (current + 1) + "<white> / <gold>" + total)
                .build();
    }
}
