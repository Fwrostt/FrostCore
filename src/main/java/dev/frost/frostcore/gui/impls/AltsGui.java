package dev.frost.frostcore.gui.impls;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.gui.Button;
import dev.frost.frostcore.gui.GuiItem;
import dev.frost.frostcore.gui.PagedGui;
import dev.frost.frostcore.moderation.ModerationDatabase;
import dev.frost.frostcore.moderation.ModerationManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AltsGui {

    public static void open(Player viewer, OfflinePlayer target, Set<UUID> alts) {
        String title = "<!italic><gradient:#D4727A:#A35560>Alts</gradient> <dark_gray>» <white>" + target.getName();
        
        PagedGui gui = PagedGui.builder(title, 6).build();
        
        ModerationDatabase modDb = ModerationManager.getInstance().getDatabase();
        List<GuiItem> items = new ArrayList<>();
        
        for (UUID altUuid : alts) {
            String altName = modDb.getLastKnownName(altUuid);
            if (altName == null) altName = altUuid.toString().substring(0, 8);
            
            boolean banned = ModerationManager.getInstance().isBanned(altUuid);
            boolean muted = ModerationManager.getInstance().isMuted(altUuid);
            OfflinePlayer altPlayer = Bukkit.getOfflinePlayer(altUuid);
            boolean online = altPlayer.isOnline();
            
            Material mat = banned ? Material.RED_TERRACOTTA : (online ? Material.LIME_TERRACOTTA : Material.GRAY_TERRACOTTA);
            String statusColor = banned ? "<#D4727A>" : (online ? "<#7ECFA0>" : "<#8FA3BF>");
            String onlineStatus = online ? "Online" : "Offline";
            
            List<String> lore = new ArrayList<>();
            lore.add("<!italic><dark_gray>Status: " + statusColor + onlineStatus);
            if (banned) lore.add("<!italic><dark_gray>Punishment: <#D4727A>Banned");
            else if (muted) lore.add("<!italic><dark_gray>Punishment: <#D4A76A>Muted");
            else lore.add("<!italic><dark_gray>Punishment: <#7ECFA0>Clean");
            
            lore.add("");
            lore.add("<!italic><gray>▸ Left-Click to view history");
            
            final String finalAltName = altName;
            items.add(Button.of(mat)
                    .name("<!italic>" + statusColor + altName)
                    .lore(lore)
                    .onClick(ctx -> {
                        ctx.close();
                        viewer.chat("/history " + finalAltName);
                    })
                    .build());
        }
        
        gui.addItems(items);
        gui.open(viewer);
    }
}
