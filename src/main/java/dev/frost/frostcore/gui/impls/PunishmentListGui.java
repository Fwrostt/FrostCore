package dev.frost.frostcore.gui.impls;

import dev.frost.frostcore.gui.*;
import dev.frost.frostcore.moderation.Punishment;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PunishmentListGui extends Gui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Player viewer;
    private final String category;
    private final String displayTitle;
    private final List<Punishment> list;
    private final int[] contentSlots;
    private int currentPage = 0;

    private PunishmentListGui(Player viewer, String category, String title, List<Punishment> list) {
        super(MM.deserialize("<!italic><#D4727A>Active " + title), 5);
        this.viewer = viewer;
        this.category = category;
        this.displayTitle = title;
        this.list = list;
        this.contentSlots = Slot.rectangle(1, 1, 3, 7);
    }

    public static void open(Player viewer, String category, String title, List<Punishment> list) {
        new PunishmentListGui(viewer, category, title, list).open(viewer);
    }

    @Override
    public void populate() {
        clear();
        forceFillBorder(GuiTemplate.blackFiller());

        int pageSize = contentSlots.length;
        int total = list.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        currentPage = Math.min(currentPage, totalPages - 1);

        // Header icon based on category
        Material headerMat = switch (category.toUpperCase()) {
            case "BAN" -> Material.IRON_DOOR;
            case "MUTE" -> Material.PAPER;
            case "WARN" -> Material.YELLOW_DYE;
            default -> Material.BOOK;
        };

        setItem(0, 4, Button.of(headerMat)
                .name("<!italic><white>Active " + displayTitle)
                .lore("<!italic><dark_gray>" + total + " active punishment" + (total == 1 ? "" : "s"))
                .build());

        // Content
        int start = currentPage * pageSize;
        int itemsOnPage = Math.min(pageSize, total - start);

        for (int i = 0; i < itemsOnPage; i++) {
            Punishment p = list.get(start + i);
            setItem(contentSlots[i], buildPunishmentItem(p));
        }

        // Bottom nav
        int navRow = getRows() - 1;
        setItem(navRow, 4, Button.of(Material.PAPER)
                .name("<!italic><gray>Page <white>" + (currentPage + 1) + " <dark_gray>/ <gray>" + totalPages)
                .lore("<!italic><dark_gray>" + total + " punishment" + (total == 1 ? "" : "s") + " total")
                .build());

        if (currentPage > 0) {
            setItem(Slot.bottomLeft(getRows()), Button.of(Material.SPECTRAL_ARROW)
                    .name("<!italic><#8FA3BF>◀ Previous")
                    .lore("<!italic><gray>Page <white>" + currentPage + " <gray>of <white>" + totalPages)
                    .onClick(ctx -> { currentPage--; refresh(viewer); })
                    .build());
        }
        if (currentPage < totalPages - 1) {
            setItem(Slot.bottomRight(getRows()), Button.of(Material.ARROW)
                    .name("<!italic><#8FA3BF>Next ▶")
                    .lore("<!italic><gray>Page <white>" + (currentPage + 2) + " <gray>of <white>" + totalPages)
                    .onClick(ctx -> { currentPage++; refresh(viewer); })
                    .build());
        }
    }

    private GuiItem buildPunishmentItem(Punishment p) {
        Material mat;
        String color;

        switch (p.type().getCategory()) {
            case "BAN":  mat = Material.IRON_DOOR;  color = "<#D4727A>"; break;
            case "MUTE": mat = Material.PAPER;      color = "<#D4A76A>"; break;
            case "WARN": mat = Material.YELLOW_DYE; color = "<#C8A87C>"; break;
            default:     mat = Material.STONE;      color = "<#8FA3BF>"; break;
        }

        List<String> lore = new ArrayList<>();
        lore.add("<!italic><dark_gray>Target: <white>" + p.getTargetDisplayName());
        lore.add("<!italic><dark_gray>Type: " + color + p.type().getDisplayName());
        lore.add("<!italic><dark_gray>Staff: <white>" + p.getStaffDisplayName());
        lore.add("<!italic><dark_gray>Reason: <white>" + p.reason());
        lore.add("");
        lore.add("<!italic><dark_gray>Duration: <white>" + p.getFormattedDuration());
        if (p.active() && !p.isExpired() && !p.isPermanent()) {
            lore.add("<!italic><dark_gray>Expires In: <white>" + p.getFormattedRemaining());
        }
        lore.add("<!italic><dark_gray>Issued: <white>" + Punishment.formatDuration(System.currentTimeMillis() - p.createdAt()) + " ago");
        lore.add("<!italic><dark_gray>ID: <#8FA3BF>#" + p.randomId());

        boolean pardonable = false;
        if (p.active() && !p.isExpired()) {
            if (p.type().getCategory().equals("BAN") && viewer.hasPermission("frostcore.moderation.ban")) pardonable = true;
            if (p.type().getCategory().equals("MUTE") && viewer.hasPermission("frostcore.moderation.mute")) pardonable = true;
            if (p.type().getCategory().equals("WARN") && viewer.hasPermission("frostcore.moderation.warn")) pardonable = true;
        }

        final boolean canPardon = pardonable;

        if (canPardon) {
            lore.add("");
            lore.add("<!italic><gray>▸ Click to pardon/remove");
        }

        return Button.of(mat)
                .name("<!italic>" + color + p.getTargetDisplayName() + " <dark_gray>#" + p.randomId())
                .lore(lore)
                .onClick(ctx -> {
                    if (canPardon) {
                        ctx.close();
                        String cmd = switch (p.type().getCategory()) {
                            case "BAN" -> "/unban " + p.randomId();
                            case "MUTE" -> "/unmute " + p.randomId();
                            case "WARN" -> "/unwarn " + p.randomId();
                            default -> null;
                        };
                        if (cmd != null) viewer.chat(cmd);
                    }
                })
                .build();
    }
}
