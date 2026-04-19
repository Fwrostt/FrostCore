package dev.frost.frostcore.gui.impls;

import dev.frost.frostcore.manager.BountyManager;
import dev.frost.frostcore.bounty.model.Bounty;
import dev.frost.frostcore.bounty.model.BountyContributor;
import dev.frost.frostcore.gui.*;
import dev.frost.frostcore.utils.EconomyUtil;
import dev.frost.frostcore.utils.SignPrompt;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BountyListGui extends Gui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final BountyManager manager;
    private final Player viewer;
    private final int[] contentSlots;
    private int currentPage = 0;
    
    private SortMode sortMode = SortMode.MOST;
    private String searchQuery = "";

    public enum SortMode {
        MOST("Highest Amount First"),
        LEAST("Lowest Amount First"),
        LATEST("Recently Placed First"),
        OLDEST("Oldest Placed First");

        private final String display;
        SortMode(String display) { this.display = display; }
        public String getDisplay() { return display; }

        public SortMode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
    }

    public BountyListGui(BountyManager manager, Player viewer) {
        super(MM.deserialize("<!italic><dark_gray>» <#8FA3BF>Active Bounties <dark_gray>«"), 6);
        this.manager = manager;
        this.viewer = viewer;
        // Inner 4-row grid for heads: rows 1 to 4, cols 1 to 7 (28 slots)
        this.contentSlots = Slot.innerSlots(6);
    }

    public void open() {
        super.open(viewer);
    }

    @Override
    public void populate() {
        clear();
        forceFillBorder(GuiTemplate.blackFiller());

        List<Bounty> bounties = getFilteredAndSortedBounties();

        int pageSize = contentSlots.length;
        int total = bounties.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        currentPage = Math.min(currentPage, totalPages - 1);

        int start = currentPage * pageSize;
        int itemsOnPage = Math.min(pageSize, total - start);

        for (int i = 0; i < itemsOnPage; i++) {
            Bounty b = bounties.get(start + i);
            setItem(contentSlots[i], buildBountyItem(b, start + i + 1));
        }

        if (total == 0) {
            setItem(Slot.of(2, 4), Button.of(Material.BARRIER)
                    .name("<!italic><#D4727A>No Bounties Found")
                    .lore("<!italic><gray>No bounties match your filters.")
                    .build());
        }

        // Stats summary
        setItem(Slot.TOP_CENTER, Button.of(Material.GOLD_INGOT)
                .name("<!italic><white>Bounty Board")
                .lore(
                    "<!italic><dark_gray>» <gray>Total Bounties: <white>" + manager.getLeaderboard().size(),
                    "<!italic><dark_gray>» <gray>Showing: <white>" + total,
                    "",
                    "<!italic><gray>Click a player to view details"
                )
                .build());

        // Sort Button
        setItem(Slot.bottomLeft(6) + 3, Button.of(Material.HOPPER)
                .name("<!italic><#8FA3BF>Sort Filter")
                .lore(
                    "<!italic><dark_gray>Current: <white>" + sortMode.getDisplay(),
                    "",
                    "<!italic><gray>▸ Click to cycle"
                )
                .onClick(ctx -> {
                    sortMode = sortMode.next();
                    currentPage = 0;
                    refresh(viewer);
                })
                .build());

        // Search Button
        setItem(Slot.bottomLeft(6) + 5, Button.of(Material.OAK_SIGN)
                .name("<!italic><#8FA3BF>Search Target")
                .lore(
                    "<!italic><dark_gray>Current: " + (searchQuery.isEmpty() ? "<gray>None" : "<white>" + searchQuery),
                    "",
                    "<!italic><gray>▸ Click to search by name"
                )
                .onClick(ctx -> {
                    ctx.close();
                    SignPrompt.prompt(viewer, new String[]{"", "^^^", "Type name", "to search"}, input -> {
                        this.searchQuery = input.trim();
                        this.currentPage = 0;
                        this.open();
                    });
                })
                .build());

        // Pagination
        int navRow = 5;
        setItem(navRow, 4, Button.of(Material.PAPER)
                .name("<!italic><gray>Page <white>" + (currentPage + 1) + " <dark_gray>/ <gray>" + totalPages)
                .lore("<!italic><dark_gray>" + total + " record" + (total == 1 ? "" : "s") + " total")
                .build());

        if (currentPage > 0) {
            setItem(navRow, 0, Button.of(Material.SPECTRAL_ARROW)
                    .name("<!italic><#8FA3BF>◀ Previous")
                    .lore("<!italic><gray>Page <white>" + currentPage + " <gray>of <white>" + totalPages)
                    .onClick(ctx -> { currentPage--; refresh(viewer); })
                    .build());
        }
        if (currentPage < totalPages - 1) {
            setItem(navRow, 8, Button.of(Material.ARROW)
                    .name("<!italic><#8FA3BF>Next ▶")
                    .lore("<!italic><gray>Page <white>" + (currentPage + 2) + " <gray>of <white>" + totalPages)
                    .onClick(ctx -> { currentPage++; refresh(viewer); })
                    .build());
        }
    }

    private List<Bounty> getFilteredAndSortedBounties() {
        List<Bounty> list = new ArrayList<>(manager.getLeaderboard());

        // Filter
        if (!searchQuery.isEmpty()) {
            String lowerQ = searchQuery.toLowerCase();
            list.removeIf(b -> !b.getTargetName().toLowerCase().contains(lowerQ));
        }

        // Sort
        switch (sortMode) {
            case MOST -> list.sort((a, b) -> Double.compare(b.getTotalAmount(), a.getTotalAmount()));
            case LEAST -> list.sort(Comparator.comparingDouble(Bounty::getTotalAmount));
            case LATEST -> list.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
            case OLDEST -> list.sort(Comparator.comparingLong(Bounty::getCreatedAt));
        }

        return list;
    }

    private GuiItem buildBountyItem(Bounty b, int displayRank) {
        BountyContributor top = b.getTopContributor();
        String topLine = top != null
                ? "<!italic><dark_gray>» <gray>Top Funder: <white>" + top.getContributorName()
                : "<!italic><dark_gray>» <gray>No contributors";

        String rankStr = switch (displayRank) {
            case 1 -> "<#F5A623>#1";
            case 2 -> "<gray>#2";
            case 3 -> "<#CD7F32>#3";
            default -> "<dark_gray>#" + displayRank;
        };

        return Button.of(Material.PLAYER_HEAD)
                .skull(b.getTargetUuid())
                .name("<!italic>" + rankStr + " <white>" + b.getTargetName())
                .lore(
                    "<!italic><dark_gray>» <gray>Total: <#D4727A>" + EconomyUtil.formatCompact(b.getTotalAmount()),
                    "<!italic><dark_gray>» <gray>Funders: <white>" + b.getContributorCount(),
                    topLine,
                    "",
                    "<!italic><gray>▸ Click to view details"
                )
                .onClick(ctx -> new BountyDetailGui(manager, ctx.getPlayer(), b).open())
                .build();
    }
}
