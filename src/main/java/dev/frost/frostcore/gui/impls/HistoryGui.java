package dev.frost.frostcore.gui.impls;

import dev.frost.frostcore.gui.*;
import dev.frost.frostcore.moderation.Punishment;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class HistoryGui extends Gui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Player viewer;
    private final OfflinePlayer target;
    private final List<Punishment> history;
    private final boolean isStaffHistory;
    private final int[] contentSlots;
    private int currentPage = 0;

    private HistoryGui(Player viewer, OfflinePlayer target, List<Punishment> history, boolean isStaffHistory) {
        super(MM.deserialize("<!italic><#D4727A>" + (isStaffHistory ? "Staff History" : "History")
                + " <dark_gray>» <white>" + target.getName()), 5);
        this.viewer = viewer;
        this.target = target;
        this.history = history;
        this.isStaffHistory = isStaffHistory;
        this.contentSlots = Slot.rectangle(1, 1, 3, 7);
    }

    public static void open(Player viewer, OfflinePlayer target, List<Punishment> history, boolean isStaffHistory) {
        new HistoryGui(viewer, target, history, isStaffHistory).open(viewer);
    }

    @Override
    public void populate() {
        clear();
        forceFillBorder(GuiTemplate.blackFiller());

        int pageSize = contentSlots.length;
        int total = history.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        currentPage = Math.min(currentPage, totalPages - 1);

        // Header
        String headerLabel = isStaffHistory ? "Staff Actions" : "Punishment History";
        setItem(0, 4, Button.of(Material.BOOK)
                .name("<!italic><white>" + headerLabel)
                .lore(
                        "<!italic><dark_gray>" + total + " record" + (total == 1 ? "" : "s"),
                        "<!italic><dark_gray>Player: <white>" + target.getName()
                )
                .build());

        // Content
        int start = currentPage * pageSize;
        int itemsOnPage = Math.min(pageSize, total - start);

        for (int i = 0; i < itemsOnPage; i++) {
            Punishment p = history.get(start + i);
            setItem(contentSlots[i], buildPunishmentItem(p));
        }

        // Bottom nav
        int navRow = getRows() - 1;
        setItem(navRow, 4, Button.of(Material.PAPER)
                .name("<!italic><gray>Page <white>" + (currentPage + 1) + " <dark_gray>/ <gray>" + totalPages)
                .lore("<!italic><dark_gray>" + total + " record" + (total == 1 ? "" : "s") + " total")
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
            case "BAN":  mat = Material.IRON_DOOR;     color = "<#D4727A>"; break;
            case "MUTE": mat = Material.PAPER;         color = "<#D4A76A>"; break;
            case "WARN": mat = Material.YELLOW_DYE;    color = "<#C8A87C>"; break;
            case "JAIL": mat = Material.IRON_BARS;     color = "<#6B8DAE>"; break;
            case "KICK": mat = Material.LEATHER_BOOTS; color = "<#A35560>"; break;
            default:     mat = Material.STONE;         color = "<#8FA3BF>"; break;
        }

        String status;
        if (!p.active()) {
            status = "<#7ECFA0>Inactive (Pardoned)";
        } else if (p.isExpired()) {
            status = "<#D4A76A>Inactive (Expired)";
        } else {
            status = "<#D4727A>Active";
        }

        List<String> lore = new ArrayList<>();
        lore.add("<!italic><dark_gray>Type: " + color + p.type().getDisplayName());
        lore.add("<!italic><dark_gray>Status: " + status);
        if (isStaffHistory) {
            lore.add("<!italic><dark_gray>Target: <white>" + p.getTargetDisplayName());
        } else {
            lore.add("<!italic><dark_gray>Staff: <white>" + p.getStaffDisplayName());
        }
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
            if (p.type().getCategory().equals("JAIL") && viewer.hasPermission("frostcore.moderation.jail")) pardonable = true;
        }

        final boolean canPardon = pardonable;

        if (canPardon) {
            lore.add("");
            lore.add("<!italic><gray>▸ Click to pardon/remove");
        }

        return Button.of(mat)
                .name("<!italic>" + color + p.type().getDisplayName() + " <dark_gray>#" + p.randomId())
                .lore(lore)
                .hideAll()
                .onClick(ctx -> {
                    if (canPardon) {
                        ctx.close();
                        String cmd = switch (p.type().getCategory()) {
                            case "BAN" -> "/unban " + p.randomId();
                            case "MUTE" -> "/unmute " + p.randomId();
                            case "WARN" -> "/unwarn " + p.randomId();
                            case "JAIL" -> "/unjail " + p.randomId();
                            default -> null;
                        };
                        if (cmd != null) viewer.chat(cmd);
                    }
                })
                .build();
    }
}
