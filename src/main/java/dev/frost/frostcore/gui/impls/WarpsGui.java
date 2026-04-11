package dev.frost.frostcore.gui;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.WarpItemConfig;
import dev.frost.frostcore.manager.WarpManager;
import dev.frost.frostcore.utils.TeleportUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A premium paginated warp browser opened via {@code /warps}.
 * <p>
 * <h3>Layout (5-row default)</h3>
 * <pre>
 *  Row 0  ◾ ◾ ◾ ◾  [✦ Server Warps]  ◾ ◾ ◾ ◾
 *  Row 1  ◾ W  W  W  W  W  W  W  ◾
 *  Row 2  ◾ W  W  W  W  W  W  W  ◾
 *  Row 3  ◾ W  W  W  W  W  W  W  ◾
 *  Row 4  ◀  ◾  ◾  ◾  [Page X/Y]  ◾  ◾  ▶
 * </pre>
 *
 * Content area (rows 1–3, cols 1–7) = 21 warp slots per page.
 * The content area is configurable via {@code config.yml → warps.gui.*}.
 * <p>
 * Each warp item is styled according to its {@link WarpItemConfig} entry in
 * {@code warps.yml}.  Locked warps (player lacks the required permission)
 * still appear but are decorated with a red "✘ No access" lore suffix and
 * cannot teleport.
 */
public class WarpsGui extends Gui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Player        viewer;
    private final WarpManager   warpManager;
    private final TeleportUtil  teleportUtil;
    private final ConfigManager config;
    private final MessageManager mm;

    /** Slots that contain warp items (derived from config). */
    private final int[] contentSlots;

    private int currentPage = 0;

    public WarpsGui(Player viewer) {
        super(
            MM.deserialize("<!italic><gradient:#55CDFC:#7B68EE>Server Warps"),
            Main.getConfigManager().getInt("warps.gui.rows", 5)
        );

        this.viewer       = viewer;
        this.warpManager  = Main.getWarpManager();
        this.teleportUtil = Main.getTeleportUtil();
        this.config       = Main.getConfigManager();
        this.mm           = Main.getMessageManager();

        int rows      = getRows();
        int startRow  = config.getInt("warps.gui.content.start-row", 1);
        int endRow    = Math.min(config.getInt("warps.gui.content.end-row",  rows - 2), rows - 2);
        int startCol  = config.getInt("warps.gui.content.start-col", 1);
        int endCol    = config.getInt("warps.gui.content.end-col",   7);

        this.contentSlots = Slot.rectangle(startRow, startCol, endRow, endCol);
    }

    @Override
    public void populate() {
        clear();

        forceFillBorder(GuiTemplate.blackFiller());

        int pageSize   = contentSlots.length;
        List<String> allWarps = new ArrayList<>(warpManager.getWarpNames());
        Collections.sort(allWarps);

        int total      = allWarps.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        currentPage    = Math.min(currentPage, totalPages - 1);

        setItem(0, 4, Button.of(Material.COMPASS)
                .name("<!italic><white>Server Warps")
                .lore(
                    "<!italic><dark_gray>" + total + " warp" + (total == 1 ? "" : "s") + " available",
                    "<!italic><gray>Browse and teleport instantly"
                )
                .build());

        int start = currentPage * pageSize;
        int itemsOnPage = Math.min(pageSize, total - start);
        int[] indices = Slot.getCenteredIndices(pageSize, itemsOnPage);
        
        for (int i = 0; i < itemsOnPage; i++) {
            int dataIdx = start + i;
            String warpName = allWarps.get(dataIdx);
            setItem(contentSlots[indices[i]], buildWarpItem(warpName));
        }

        int navRow = getRows() - 1;

        for (int c = 1; c <= 7; c++) {
            setItem(navRow, c, GuiTemplate.blackFiller());
        }

        setItem(navRow, 4, Button.of(Material.PAPER)
                .name("<!italic><gray>Page <white>" + (currentPage + 1)
                        + " <dark_gray>/ <gray>" + totalPages)
                .lore("<!italic><dark_gray>" + total + " warp" + (total == 1 ? "" : "s") + " total")
                .build());

        if (currentPage > 0) {
            int cp = currentPage;
            setItem(Slot.bottomLeft(getRows()), Button.of(Material.SPECTRAL_ARROW)
                    .name("<!italic><#A3C4FF>◀ Previous")
                    .lore("<!italic><gray>Page <white>" + cp + " <gray>of <white>" + totalPages)
                    .onClick(ctx -> {
                        if (!viewer.isOnline()) return;
                        currentPage--;
                        refresh(viewer);
                    })
                    .build());
        }

        if (currentPage < totalPages - 1) {
            int cp = currentPage;
            setItem(Slot.bottomRight(getRows()), Button.of(Material.ARROW)
                    .name("<!italic><#A3C4FF>Next ▶")
                    .lore("<!italic><gray>Page <white>" + (cp + 2) + " <gray>of <white>" + totalPages)
                    .onClick(ctx -> {
                        if (!viewer.isOnline()) return;
                        currentPage++;
                        refresh(viewer);
                    })
                    .build());
        }
    }

    private GuiItem buildWarpItem(String warpName) {
        WarpItemConfig cfg = warpManager.getWarpConfig(warpName);
        Location loc = warpManager.getWarp(warpName);

        boolean hasPermission = !cfg.requiresPermission()
                || viewer.hasPermission(cfg.getPermission());

        List<String> lore = new ArrayList<>(cfg.getLore());

        if (cfg.requiresPermission()) {
            lore.add("");
            if (hasPermission) {
                lore.add("<green>✔ <gray>You have access");
            } else {
                lore.add("<red>✘ <gray>No access");
                lore.add("<dark_gray>Requires: <gray>" + cfg.getPermission());
            }
        }

        if (hasPermission) {
            lore.add("");
            lore.add("<dark_gray>▸ <gray>Click to teleport");
        }

        Button.Builder btn = Button.of(cfg.getMaterial())
                .name(cfg.getDisplayName())
                .lore(lore);

        if (cfg.isGlow()) btn.glow();

        if (!hasPermission) {

            return btn.onClick(ctx ->
                    mm.sendRaw(ctx.getPlayer(),
                            "<red>✘ You don't have permission to use the <white>" + warpName + "</white> warp."))
                    .build();
        }

        return btn.onClick(ctx -> {
            ctx.close();
            teleportUtil.teleportWithCooldownAndDelay(
                    ctx.getPlayer(), loc,
                    "warp",
                    "warps.cooldown",
                    "teleport.warp-cooldown",
                    config.getInt("warps.delay", 3),
                    "teleport.warp-wait",
                    "teleport.warp-teleport",
                    "teleport.warp-teleport-cancelled",
                    Map.of("warp", warpName)
            );
        }).build();
    }

    /** Jump directly to a page (0-based). Rebuilds the GUI in place. */
    public void setPage(int page) {
        List<String> allWarps = new ArrayList<>(warpManager.getWarpNames());
        int totalPages = Math.max(1, (int) Math.ceil(
                (double) allWarps.size() / contentSlots.length));
        currentPage = Math.max(0, Math.min(page, totalPages - 1));
        refresh(viewer);
    }

    public int getCurrentPage()  { return currentPage; }
    public int[] getContentSlots() { return contentSlots; }
}

