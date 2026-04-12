package dev.frost.frostcore.gui.impls;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.gui.Button;
import dev.frost.frostcore.gui.GuiItem;
import dev.frost.frostcore.gui.GuiTemplate;
import dev.frost.frostcore.gui.PagedGui;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.moderation.ModerationManager;
import dev.frost.frostcore.moderation.Punishment;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class HistoryGui {

    public static void open(Player viewer, OfflinePlayer target, List<Punishment> history, boolean isStaffHistory) {
        String title = "<!italic><gradient:#D4727A:#A35560>" + (isStaffHistory ? "Staff History" : "History") + "</gradient> <dark_gray>» <white>" + target.getName();
        
        PagedGui gui = PagedGui.builder(title, 6)
                .decorate(() -> {
                })
                .build();
                
        List<GuiItem> items = new ArrayList<>();
        for (Punishment p : history) {
            items.add(buildPunishmentItem(viewer, p, isStaffHistory));
        }
        
        gui.addItems(items);
        gui.open(viewer);
    }
    
    private static GuiItem buildPunishmentItem(Player viewer, Punishment p, boolean isStaffHistory) {
        Material mat;
        String color;
        
        switch (p.type().getCategory()) {
            case "BAN": 
                mat = Material.IRON_DOOR; color = "<#D4727A>"; break;
            case "MUTE": 
                mat = Material.PAPER; color = "<#D4A76A>"; break;
            case "WARN":
                mat = Material.YELLOW_DYE; color = "<#C8A87C>"; break;
            case "JAIL":
                mat = Material.IRON_BARS; color = "<#6B8DAE>"; break;
            case "KICK":
                mat = Material.LEATHER_BOOTS; color = "<#A35560>"; break;
            default:
                mat = Material.STONE; color = "<#8FA3BF>"; break;
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
                        if (cmd != null) {
                            viewer.chat(cmd);
                        }
                    }
                })
                .build();
    }
}
