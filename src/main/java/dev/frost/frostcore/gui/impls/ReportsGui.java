package dev.frost.frostcore.gui.impls;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.gui.Button;
import dev.frost.frostcore.gui.GuiItem;
import dev.frost.frostcore.gui.PagedGui;
import dev.frost.frostcore.moderation.ModerationManager;
import dev.frost.frostcore.moderation.Report;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ReportsGui {

    public static void open(Player viewer, List<Report> reports) {
        String guiTitle = "<!italic><gradient:#6B8DAE:#8BADC4>Open Reports</gradient>";
        
        PagedGui gui = PagedGui.builder(guiTitle, 6)
                .decorate(() -> {})
                .build();
                
        List<GuiItem> items = new ArrayList<>();
        for (Report r : reports) {
            items.add(buildReportItem(viewer, r));
        }
        
        gui.addItems(items);
        gui.open(viewer);
    }
    
    private static GuiItem buildReportItem(Player viewer, Report r) {
        List<String> lore = new ArrayList<>();
        lore.add("<!italic><dark_gray>Target: <white>" + r.getTargetDisplayName());
        lore.add("<!italic><dark_gray>Reporter: <white>" + r.getReporterDisplayName());
        lore.add("<!italic><dark_gray>Reason: <white>" + r.reason());
        lore.add("");
        lore.add("<!italic><dark_gray>Submitted: <white>" + r.getAge());
        
        boolean canHandle = viewer.hasPermission("frostcore.moderation.reports");
        
        if (canHandle) {
            lore.add("");
            lore.add("<!italic><gray>▸ Middle-Click to teleport to target");
            lore.add("<!italic><gray>▸ Right-Click to resolve/close report");
        }
        
        return Button.of(Material.WRITTEN_BOOK)
                .name("<!italic><#8BADC4>Report #" + r.id())
                .lore(lore)
                .onClick(ctx -> {
                    if (canHandle) {
                        if (ctx.isRightClick()) {
                            ctx.close();
                            // Handle via backend
                            Main.getInstance().getServer().getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
                                ModerationManager.getInstance().getDatabase().handleReport(r.id(), viewer.getUniqueId(), viewer.getName());
                                ctx.getPlayer().sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                                    .deserialize("<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#8FA3BF>Resolved report <white>#" + r.id()));
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
