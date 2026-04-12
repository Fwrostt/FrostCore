package dev.frost.frostcore.gui.impls;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.gui.*;
import dev.frost.frostcore.moderation.ModerationManager;
import dev.frost.frostcore.moderation.Report;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ReportsGui extends Gui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Player viewer;
    private final List<Report> reports;
    private final int[] contentSlots;
    private int currentPage = 0;

    private ReportsGui(Player viewer, List<Report> reports) {
        super(MM.deserialize("<!italic><#D4727A>Open Reports"), 4);
        this.viewer = viewer;
        this.reports = reports;
        this.contentSlots = Slot.rectangle(1, 1, 2, 7);
    }

    public static void open(Player viewer, List<Report> reports) {
        new ReportsGui(viewer, reports).open(viewer);
    }

    @Override
    public void populate() {
        clear();
        forceFillBorder(GuiTemplate.blackFiller());

        int pageSize = contentSlots.length;
        int total = reports.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        currentPage = Math.min(currentPage, totalPages - 1);

        setItem(0, 4, Button.of(Material.WRITABLE_BOOK)
                .name("<!italic><white>Open Reports")
                .lore("<!italic><dark_gray>" + total + " report" + (total == 1 ? "" : "s") + " pending")
                .build());

        int start = currentPage * pageSize;
        int itemsOnPage = Math.min(pageSize, total - start);

        for (int i = 0; i < itemsOnPage; i++) {
            Report r = reports.get(start + i);
            setItem(contentSlots[i], buildReportItem(r));
        }

        int navRow = getRows() - 1;
        setItem(navRow, 4, Button.of(Material.PAPER)
                .name("<!italic><gray>Page <white>" + (currentPage + 1) + " <dark_gray>/ <gray>" + totalPages)
                .lore("<!italic><dark_gray>" + total + " report" + (total == 1 ? "" : "s") + " total")
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

    private GuiItem buildReportItem(Report r) {
        List<String> lore = new ArrayList<>();
        lore.add("<!italic><dark_gray>Target: <white>" + r.getTargetDisplayName());
        lore.add("<!italic><dark_gray>Reporter: <white>" + r.getReporterDisplayName());
        lore.add("<!italic><dark_gray>Reason: <white>" + r.reason());
        lore.add("");
        lore.add("<!italic><dark_gray>Submitted: <white>" + r.getAge());

        boolean canHandle = viewer.hasPermission("frostcore.moderation.reports");

        if (canHandle) {
            lore.add("");
            lore.add("<!italic><gray>▸ Middle-Click to teleport");
            lore.add("<!italic><gray>▸ Right-Click to resolve");
        }

        return Button.of(Material.WRITTEN_BOOK)
                .name("<!italic><#8BADC4>Report #" + r.id())
                .lore(lore)
                .onClick(ctx -> {
                    if (canHandle) {
                        if (ctx.isRightClick()) {
                            ctx.close();
                            Main.getInstance().getServer().getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
                                ModerationManager.getInstance().getDatabase().handleReport(r.id(), viewer.getUniqueId(), viewer.getName());
                                ctx.getPlayer().sendMessage(MM.deserialize(
                                        "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>Resolved report <white>#" + r.id()));
                            });
                        } else if (ctx.isMiddleClick()) {
                            ctx.close();
                            viewer.chat("/tp " + r.getTargetDisplayName());
                        }
                    }
                })
                .build();
    }
}
