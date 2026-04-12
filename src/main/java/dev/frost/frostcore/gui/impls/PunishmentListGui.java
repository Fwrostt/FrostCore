package dev.frost.frostcore.gui.impls;

import dev.frost.frostcore.gui.Button;
import dev.frost.frostcore.gui.GuiItem;
import dev.frost.frostcore.gui.PagedGui;
import dev.frost.frostcore.moderation.Punishment;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PunishmentListGui {

    public static void open(Player viewer, String category, String title, List<Punishment> list) {
        String guiTitle = "<!italic><gradient:#D4727A:#A35560>Active " + title + "</gradient>";
        
        PagedGui gui = PagedGui.builder(guiTitle, 6)
                .decorate(() -> {})
                .build();
                
        List<GuiItem> items = new ArrayList<>();
        for (Punishment p : list) {
            items.add(buildPunishmentItem(viewer, p));
        }
        
        gui.addItems(items);
        gui.open(viewer);
    }
    
    private static GuiItem buildPunishmentItem(Player viewer, Punishment p) {
        Material mat;
        String color;
        
        switch (p.type().getCategory()) {
            case "BAN": 
                mat = Material.IRON_DOOR; color = "<#D4727A>"; break;
            case "MUTE": 
                mat = Material.PAPER; color = "<#D4A76A>"; break;
            case "WARN":
                mat = Material.YELLOW_DYE; color = "<#C8A87C>"; break;
            default:
                mat = Material.STONE; color = "<#8FA3BF>"; break;
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
                        if (cmd != null) {
                            viewer.chat(cmd);
                        }
                    }
                })
                .build();
    }
}
