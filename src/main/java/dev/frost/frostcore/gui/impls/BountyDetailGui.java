package dev.frost.frostcore.gui.impls;

import dev.frost.frostcore.manager.BountyManager;
import dev.frost.frostcore.bounty.model.Bounty;
import dev.frost.frostcore.bounty.model.BountyContributor;
import dev.frost.frostcore.gui.*;
import dev.frost.frostcore.utils.EconomyUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BountyDetailGui extends Gui {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final BountyManager manager;
    private final Player viewer;
    private final Bounty bounty;
    private final int[] contentSlots;
    private int currentPage = 0;

    public BountyDetailGui(BountyManager manager, Player viewer, Bounty bounty) {
        super(MM.deserialize("<!italic><dark_gray>» <#8FA3BF>Bounty: <white>" + bounty.getTargetName() + " <dark_gray>«"), 6);
        this.manager = manager;
        this.viewer = viewer;
        this.bounty = bounty;
        this.contentSlots = Slot.innerSlots(6);
    }

    public void open() {
        super.open(viewer);
    }

    @Override
    public void populate() {
        clear();
        forceFillBorder(GuiTemplate.blackFiller());

        List<BountyContributor> contributors = new ArrayList<>(bounty.getContributors());
        contributors.sort((a, b) -> Double.compare(b.getAmount(), a.getAmount()));

        int pageSize = contentSlots.length;
        int total = contributors.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        currentPage = Math.min(currentPage, totalPages - 1);

        int start = currentPage * pageSize;
        int itemsOnPage = Math.min(pageSize, total - start);

        for (int i = 0; i < itemsOnPage; i++) {
            BountyContributor c = contributors.get(start + i);
            setItem(contentSlots[i], buildContributorItem(c, start + i + 1));
        }

        // Info stack
        setItem(Slot.TOP_CENTER, Button.of(Material.PLAYER_HEAD)
                .skull(bounty.getTargetUuid())
                .name("<!italic><white>" + bounty.getTargetName())
                .lore(
                    "<!italic><dark_gray>» <gray>Total Bounty: <white>" + EconomyUtil.formatCompact(bounty.getTotalAmount()),
                    "<!italic><dark_gray>» <gray>Contributors: <white>" + bounty.getContributorCount(),
                    "<!italic><dark_gray>» <gray>Since: <white>" + DATE_FMT.format(Instant.ofEpochMilli(bounty.getCreatedAt())),
                    bounty.getExpiresAt() > 0
                         ? "<!italic><dark_gray>» <gray>Expires: <#D4727A>" + DATE_FMT.format(Instant.ofEpochMilli(bounty.getExpiresAt()))
                         : "<!italic><dark_gray>» <gray>Expires: <white>Never"
                )
                .build());

        int navRow = 5;
        
        // Back Button
        setItem(Slot.bottomLeft(6) + 4, Button.of(Material.SPECTRAL_ARROW)
                .name("<!italic><#8FA3BF>◀ Back to List")
                .lore("<!italic><gray>Return to active bounties")
                .onClick(ctx -> {
                    ctx.close();
                    new BountyListGui(manager, ctx.getPlayer()).open();
                })
                .build());

        // Navigation
        if (currentPage > 0) {
            setItem(navRow, 0, Button.of(Material.ARROW)
                    .name("<!italic><#8FA3BF>◀ Previous")
                    .onClick(ctx -> { currentPage--; refresh(viewer); })
                    .build());
        }
        if (currentPage < totalPages - 1) {
            setItem(navRow, 8, Button.of(Material.ARROW)
                    .name("<!italic><#8FA3BF>Next ▶")
                    .onClick(ctx -> { currentPage++; refresh(viewer); })
                    .build());
        }
        
    }

    private GuiItem buildContributorItem(BountyContributor c, int rank) {
        String isYou = c.getContributorUuid().equals(viewer.getUniqueId()) ? " <#7ECFA0>(You)" : "";
        
        String rankStr = switch (rank) {
            case 1 -> "<#F5A623>#1";
            case 2 -> "<gray>#2";
            case 3 -> "<#CD7F32>#3";
            default -> "<dark_gray>#" + rank;
        };

        return Button.of(Material.PLAYER_HEAD)
                .skull(c.getContributorUuid())
                .name("<!italic>" + rankStr + " <white>" + c.getContributorName() + isYou)
                .lore(
                    "<!italic><dark_gray>» <gray>Contributed: <white>" + EconomyUtil.formatCompact(c.getAmount()),
                    "<!italic><dark_gray>» <gray>At: <white>" + DATE_FMT.format(Instant.ofEpochMilli(c.getCreatedAt()))
                )
                .build();
    }
}
