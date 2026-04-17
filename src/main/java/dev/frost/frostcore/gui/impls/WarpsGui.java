package dev.frost.frostcore.gui;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.WarpItemConfig;
import dev.frost.frostcore.manager.WarpManager;
import dev.frost.frostcore.utils.FrostLogger;
import dev.frost.frostcore.utils.TeleportUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import org.bukkit.configuration.ConfigurationSection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class WarpsGui extends Gui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Player        viewer;
    private final WarpManager   warpManager;
    private final TeleportUtil  teleportUtil;
    private final ConfigManager config;
    private final MessageManager mm;

    
    private final int[] contentSlots;

    private int currentPage = 0;

    public WarpsGui(Player viewer) {
        super(
            MM.deserialize("<!italic><gradient:#6B8DAE:#8BADC4>Server Warps"),
            Main.getWarpManager().getGuiRows()
        );

        this.viewer       = viewer;
        this.warpManager  = Main.getWarpManager();
        this.teleportUtil = Main.getTeleportUtil();
        this.config       = Main.getConfigManager();
        this.mm           = Main.getMessageManager();

        int rows = getRows();
        // Content occupies rows 1 to (rows-2), columns 1 to 7 (inside the border).
        // Both bounds auto-adapt to whatever gui.rows is set to.
        org.bukkit.configuration.ConfigurationSection guiSec = warpManager.getGuiSection();
        int startRow = guiSec != null ? guiSec.getInt("content.start-row", 1)       : 1;
        int endRow   = guiSec != null ? guiSec.getInt("content.end-row",   rows - 2) : rows - 2;
        int startCol = guiSec != null ? guiSec.getInt("content.start-col", 1)        : 1;
        int endCol   = guiSec != null ? guiSec.getInt("content.end-col",   7)        : 7;
        endRow = Math.min(endRow, rows - 2); // never bleed into the nav row

        this.contentSlots = Slot.rectangle(startRow, startCol, endRow, endCol);
    }

    @Override
    public void populate() {
        clear();

        ConfigurationSection guiSec = warpManager.getGuiSection();

        // ── Border ────────────────────────────────────────────────────────────
        boolean borderEnabled = guiSec == null || guiSec.getBoolean("border.enabled", true);
        if (borderEnabled) {
            String borderMat = guiSec != null
                    ? guiSec.getString("border.material", "BLACK_STAINED_GLASS_PANE")
                    : "BLACK_STAINED_GLASS_PANE";
            Material borderMaterial;
            try { borderMaterial = Material.valueOf(borderMat.toUpperCase()); }
            catch (Exception e) { borderMaterial = Material.BLACK_STAINED_GLASS_PANE; }
            forceFillBorder(Button.of(borderMaterial).name("<reset>").build());
        }

        // ── Warp items ────────────────────────────────────────────────────────
        List<String> allWarps = new ArrayList<>(warpManager.getWarpNames());
        Collections.sort(allWarps);

        int total  = allWarps.size();
        String pluralS = total == 1 ? "" : "s";

        // Split into pinned (fixed slot) and auto (paginated) warps.
        // Conflict rule: if two warps claim the same slot index, last one
        // alphabetically wins (we process in sorted order) and a warning is logged.
        java.util.LinkedHashMap<Integer, String> pinnedSlotMap = new java.util.LinkedHashMap<>();
        List<String> autoWarps = new ArrayList<>();

        for (String warpName : allWarps) {
            WarpItemConfig cfg = warpManager.getWarpConfig(warpName);
            if (cfg.isPinned()) {
                int slotIdx = cfg.getSlot();
                if (slotIdx >= contentSlots.length) {
                    FrostLogger.warn("[WarpsGui] Warp '" + warpName + "' has slot " + slotIdx
                            + " but only " + contentSlots.length + " content slots exist — treating as auto.");
                    autoWarps.add(warpName);
                } else if (pinnedSlotMap.containsKey(slotIdx)) {
                    String previous = pinnedSlotMap.get(slotIdx);
                    FrostLogger.warn("[WarpsGui] Slot conflict at index " + slotIdx
                            + ": warps '" + previous + "' and '" + warpName
                            + "' both claim the same slot. '" + warpName + "' will be used (last loaded wins).");
                    autoWarps.add(previous); // bump the old one to auto
                    pinnedSlotMap.put(slotIdx, warpName);
                } else {
                    pinnedSlotMap.put(slotIdx, warpName);
                }
            } else {
                autoWarps.add(warpName);
            }
        }

        // Slots available for auto warps = content slots not claimed by pinned warps.
        List<Integer> freeSlotIndices = new ArrayList<>();
        for (int i = 0; i < contentSlots.length; i++) {
            if (!pinnedSlotMap.containsKey(i)) freeSlotIndices.add(i);
        }

        int freePerPage = freeSlotIndices.size();
        int totalPages  = Math.max(1, (int) Math.ceil((double) autoWarps.size() / Math.max(1, freePerPage)));
        currentPage     = Math.min(currentPage, totalPages - 1);

        // ── Header (compass) ──────────────────────────────────────────────────
        boolean headerEnabled = guiSec == null || guiSec.getBoolean("header.enabled", true);
        if (headerEnabled) {
            String hMatName = guiSec != null ? guiSec.getString("header.material", "COMPASS") : "COMPASS";
            Material hMat;
            try { hMat = Material.valueOf(hMatName.toUpperCase()); }
            catch (Exception e) { hMat = Material.COMPASS; }

            String hName = guiSec != null
                    ? guiSec.getString("header.name", "<!italic><white>Server Warps")
                    : "<!italic><white>Server Warps";
            hName = hName.replace("{total}", String.valueOf(total)).replace("{s}", pluralS);

            List<String> hLore = guiSec != null ? guiSec.getStringList("header.lore") : List.of();
            if (hLore.isEmpty()) {
                hLore = List.of(
                        "<!italic><dark_gray>" + total + " warp" + pluralS + " available",
                        "<!italic><gray>Browse and teleport instantly"
                );
            } else {
                hLore = hLore.stream()
                        .map(l -> l.replace("{total}", String.valueOf(total)).replace("{s}", pluralS))
                        .toList();
            }

            boolean hGlow = guiSec != null && guiSec.getBoolean("header.glow", false);
            Button.Builder hBtn = Button.of(hMat).name(hName).lore(hLore);
            if (hGlow) hBtn.glow();
            setItem(0, 4, hBtn.build());
        }

        // ── Place pinned warps (always visible, every page) ───────────────────
        for (Map.Entry<Integer, String> entry : pinnedSlotMap.entrySet()) {
            setItem(contentSlots[entry.getKey()], buildWarpItem(entry.getValue()));
        }

        // ── Place auto warps (paginated across free slots) ────────────────────
        int autoStart   = currentPage * freePerPage;
        int autoOnPage  = Math.min(freePerPage, autoWarps.size() - autoStart);

        // Centre auto warps within the free slots for this page
        int[] autoIndices = freePerPage > 0
                ? Slot.getCenteredIndices(freePerPage, Math.max(0, autoOnPage))
                : new int[0];

        for (int i = 0; i < autoOnPage; i++) {
            String warpName = autoWarps.get(autoStart + i);
            int freeSlotListIdx = autoIndices[i];           // index into freeSlotIndices
            int contentSlotIdx  = freeSlotIndices.get(freeSlotListIdx); // actual content slot index
            setItem(contentSlots[contentSlotIdx], buildWarpItem(warpName));
        }

        // ── Nav row fill ──────────────────────────────────────────────────────
        int navRow = getRows() - 1;
        for (int c = 1; c <= 7; c++) {
            setItem(navRow, c, GuiTemplate.blackFiller());
        }

        // ── Page indicator (paper) ────────────────────────────────────────────
        boolean pageItemEnabled = guiSec == null || guiSec.getBoolean("page-item.enabled", true);
        if (pageItemEnabled) {
            String pMatName = guiSec != null ? guiSec.getString("page-item.material", "PAPER") : "PAPER";
            Material pMat;
            try { pMat = Material.valueOf(pMatName.toUpperCase()); }
            catch (Exception e) { pMat = Material.PAPER; }

            String pName = guiSec != null
                    ? guiSec.getString("page-item.name",
                        "<!italic><gray>Page <white>{page} <dark_gray>/ <gray>{total_pages}")
                    : "<!italic><gray>Page <white>" + (currentPage + 1) + " <dark_gray>/ <gray>" + totalPages;
            pName = pName
                    .replace("{page}", String.valueOf(currentPage + 1))
                    .replace("{total_pages}", String.valueOf(totalPages))
                    .replace("{total}", String.valueOf(total))
                    .replace("{s}", pluralS);

            List<String> pLore = guiSec != null ? guiSec.getStringList("page-item.lore") : List.of();
            if (pLore.isEmpty()) {
                pLore = List.of("<!italic><dark_gray>" + total + " warp" + pluralS + " total");
            } else {
                pLore = pLore.stream()
                        .map(l -> l
                                .replace("{total}", String.valueOf(total))
                                .replace("{s}", pluralS)
                                .replace("{page}", String.valueOf(currentPage + 1))
                                .replace("{total_pages}", String.valueOf(totalPages)))
                        .toList();
            }

            boolean pGlow = guiSec != null && guiSec.getBoolean("page-item.glow", false);
            Button.Builder pBtn = Button.of(pMat).name(pName).lore(pLore);
            if (pGlow) pBtn.glow();
            setItem(navRow, 4, pBtn.build());
        }

        // ── Prev / Next buttons ───────────────────────────────────────────────
        if (currentPage > 0) {
            int cp = currentPage;
            setItem(Slot.bottomLeft(getRows()), Button.of(Material.SPECTRAL_ARROW)
                    .name("<!italic><#8FA3BF>◀ Previous")
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
                    .name("<!italic><#8FA3BF>Next ▶")
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
                            "<#D4727A>✘ You don't have permission to use the <white>" + warpName + "</white> warp."))
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

