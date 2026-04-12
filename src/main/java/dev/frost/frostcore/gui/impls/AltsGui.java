package dev.frost.frostcore.gui.impls;

import dev.frost.frostcore.gui.*;
import dev.frost.frostcore.moderation.ModerationDatabase;
import dev.frost.frostcore.moderation.ModerationManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class AltsGui extends Gui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Player viewer;
    private final OfflinePlayer target;
    private final List<UUID> altList;
    private final int[] contentSlots;
    private int currentPage = 0;

    private AltsGui(Player viewer, OfflinePlayer target, Set<UUID> alts) {
        super(MM.deserialize("<!italic><#D4727A>Alts <dark_gray>» <white>" + target.getName()), 4);
        this.viewer = viewer;
        this.target = target;
        this.altList = new ArrayList<>(alts);
        this.contentSlots = Slot.rectangle(1, 1, 2, 7);
    }

    public static void open(Player viewer, OfflinePlayer target, Set<UUID> alts) {
        new AltsGui(viewer, target, alts).open(viewer);
    }

    @Override
    public void populate() {
        clear();
        forceFillBorder(GuiTemplate.blackFiller());

        int pageSize = contentSlots.length;
        int total = altList.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        currentPage = Math.min(currentPage, totalPages - 1);

        ModerationDatabase modDb = ModerationManager.getInstance().getDatabase();
        ModerationManager mod = ModerationManager.getInstance();

        
        setItem(0, 4, Button.of(Material.PLAYER_HEAD)
                .skull(target.getUniqueId())
                .name("<!italic><white>" + target.getName() + "'s Alts")
                .lore("<!italic><dark_gray>" + total + " alt" + (total == 1 ? "" : "s") + " found")
                .build());

        
        int start = currentPage * pageSize;
        int itemsOnPage = Math.min(pageSize, total - start);

        for (int i = 0; i < itemsOnPage; i++) {
            UUID altUuid = altList.get(start + i);
            String altName = modDb.getLastKnownName(altUuid);
            if (altName == null) altName = altUuid.toString().substring(0, 8);

            boolean banned = mod.isBanned(altUuid);
            boolean muted = mod.isMuted(altUuid);
            OfflinePlayer altPlayer = Bukkit.getOfflinePlayer(altUuid);
            boolean online = altPlayer.isOnline();

            String statusColor = banned ? "<#D4727A>" : (online ? "<#7ECFA0>" : "<#8FA3BF>");
            String onlineStatus = online ? "Online" : "Offline";

            List<String> lore = new ArrayList<>();
            lore.add("<!italic><dark_gray>Status: " + statusColor + onlineStatus);
            if (banned) lore.add("<!italic><dark_gray>Punishment: <#D4727A>Banned");
            else if (muted) lore.add("<!italic><dark_gray>Punishment: <#D4A76A>Muted");
            else lore.add("<!italic><dark_gray>Punishment: <#7ECFA0>Clean");
            lore.add("");
            lore.add("<!italic><gray>▸ Click to view history");

            final String finalAltName = altName;
            setItem(contentSlots[i], Button.of(Material.PLAYER_HEAD)
                    .skull(altUuid)
                    .name("<!italic>" + statusColor + altName)
                    .lore(lore)
                    .onClick(ctx -> {
                        ctx.close();
                        viewer.chat("/history " + finalAltName);
                    })
                    .build());
        }

        
        int navRow = getRows() - 1;
        setItem(navRow, 4, Button.of(Material.PAPER)
                .name("<!italic><gray>Page <white>" + (currentPage + 1) + " <dark_gray>/ <gray>" + totalPages)
                .lore("<!italic><dark_gray>" + total + " alt" + (total == 1 ? "" : "s") + " total")
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
}
