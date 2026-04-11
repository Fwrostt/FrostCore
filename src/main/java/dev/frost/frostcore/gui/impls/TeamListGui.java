package dev.frost.frostcore.gui.impls;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.gui.*;
import dev.frost.frostcore.manager.TeamManager;
import dev.frost.frostcore.teams.Team;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Paginated team browser opened by {@code /team list}.
 *
 * <h3>Layout (6-row chest)</h3>
 * <pre>
 *  Row 0  ◾ ◾ ◾ ◾  [Teams icon]  ◾ ◾ ◾ ◾
 *  Row 1  ◾  [HEAD] [HEAD] [HEAD] [HEAD] [HEAD] [HEAD] [HEAD]  ◾
 *  Row 2  ◾  [HEAD] [HEAD] [HEAD] [HEAD] [HEAD] [HEAD] [HEAD]  ◾
 *  Row 3  ◾  [HEAD] [HEAD] [HEAD] [HEAD] [HEAD] [HEAD] [HEAD]  ◾
 *  Row 4  ◾  [HEAD] [HEAD] [HEAD] [HEAD] [HEAD] [HEAD] [HEAD]  ◾
 *  Row 5  ◀  ◾  ◾  ◾  [Page X/Y]  ◾  ◾  ▶
 * </pre>
 *
 * Clicking a team head opens {@link TeamInfoGui} — UNLESS the viewer's team
 * has declared that team as an enemy, in which case a denied tooltip appears
 * (the item still shows, they just can't view info).
 */
public class TeamListGui extends Gui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int[] CONTENT_SLOTS = Slot.rectangle(1, 1, 4, 7);

    private final Player viewer;
    private final TeamManager teamManager;

    /** The viewer's own team (null if not in a team). Used for enemy check. */
    private final Team viewerTeam;

    private int currentPage = 0;

    public TeamListGui(Player viewer) {
        super(MM.deserialize("<!italic><gradient:#FFD700:#FFA500>Teams"), 6);
        this.viewer      = viewer;
        this.teamManager = TeamManager.getInstance();
        this.viewerTeam  = safeGetTeam(viewer);
    }

    @Override
    public void populate() {
        clear();

        forceFillBorder(GuiTemplate.blackFiller());

        List<Team> allTeams = new ArrayList<>(teamManager.getAllTeams());
        Collections.sort(allTeams, (a, b) -> b.getTotalMembers() - a.getTotalMembers());

        int pageSize   = CONTENT_SLOTS.length;
        int total      = allTeams.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        currentPage    = Math.min(currentPage, totalPages - 1);

        setItem(0, 4, Button.of(Material.NETHER_STAR)
                .name("<!italic><gradient:#FFD700:#FFA500>Teams")
                .lore(
                    "<!italic><dark_gray>" + total + " team" + (total == 1 ? "" : "s") + " on this server",
                    "<!italic><gray>Click a team to view info"
                )
                .build());

        int start = currentPage * pageSize;
        for (int i = 0; i < pageSize; i++) {
            int idx = start + i;
            if (idx < total) {
                setItem(CONTENT_SLOTS[i], buildTeamItem(allTeams.get(idx)));
            }
        }

        int navRow = getRows() - 1;
        for (int c = 1; c <= 7; c++) setItem(navRow, c, GuiTemplate.blackFiller());

        setItem(navRow, 4, Button.of(Material.PAPER)
                .name("<!italic><gray>Page <white>" + (currentPage + 1) + " <dark_gray>/ <gray>" + totalPages)
                .lore("<!italic><dark_gray>" + total + " team" + (total == 1 ? "" : "s") + " total")
                .build());

        if (currentPage > 0) {
            int cp = currentPage;
            setItem(Slot.bottomLeft(getRows()), Button.of(Material.SPECTRAL_ARROW)
                    .name("<!italic><#A3C4FF>◀ Previous")
                    .lore("<!italic><gray>Page <white>" + cp + " <gray>of <white>" + totalPages)
                    .onClick(ctx -> { currentPage--; refresh(viewer); })
                    .build());
        }

        if (currentPage < totalPages - 1) {
            int cp = currentPage;
            setItem(Slot.bottomRight(getRows()), Button.of(Material.ARROW)
                    .name("<!italic><#A3C4FF>Next ▶")
                    .lore("<!italic><gray>Page <white>" + (cp + 2) + " <gray>of <white>" + totalPages)
                    .onClick(ctx -> { currentPage++; refresh(viewer); })
                    .build());
        }
    }

    private GuiItem buildTeamItem(Team team) {

        UUID ownerUUID = team.getOwners().isEmpty() ? null : team.getOwners().iterator().next();

        String relationLine = buildRelationLine(team);
        boolean isEnemy = viewerTeam != null && viewerTeam.isEnemy(team.getName());

        List<String> lore = new ArrayList<>();
        lore.add("<!italic><dark_gray>Tag: <white>[" + team.getTag() + "]");
        lore.add("<!italic><dark_gray>Members: <white>" + team.getTotalMembers());
        lore.add("<!italic><dark_gray>PvP: " + (team.isPvpToggle() ? "<green>ON" : "<red>OFF"));
        if (!team.getAllies().isEmpty()) {
            lore.add("<!italic><dark_gray>Allies: <green>" + team.getAllies().size());
        }
        if (!team.getEnemies().isEmpty()) {
            lore.add("<!italic><dark_gray>Enemies: <red>" + team.getEnemies().size());
        }
        lore.add("");
        lore.add(relationLine);

        if (isEnemy) {
            lore.add("<!italic><red>✘ Enemy team — info restricted");
        } else {
            lore.add("<!italic><gray>▸ Click to view team info");
        }

        ItemStack skull = makeSkull(ownerUUID);
        Button.Builder btn = Button.of(skull)
                .name("<!italic><gradient:#FFD700:#FFA500>" + team.getDisplayName())
                .lore(lore);

        if (isEnemy) {

            return btn.onClick(ctx ->
                    Main.getMessageManager().sendRaw(ctx.getPlayer(),
                            "<red>✘ <gray>You cannot view info about an enemy team.")
            ).build();
        }

        return btn.onClick(ctx ->
                GuiManager.schedule(() -> new TeamInfoGui(ctx.getPlayer(), team).open(ctx.getPlayer()))
        ).build();
    }

    private String buildRelationLine(Team team) {
        if (viewerTeam == null) return "<!italic><dark_gray>Relation: <gray>None";
        if (viewerTeam.getName().equals(team.getName())) return "<!italic><yellow>★ Your team";
        if (viewerTeam.isAlly(team.getName()))            return "<!italic><green>♥ Allied";
        if (viewerTeam.isEnemy(team.getName()))           return "<!italic><red>✘ Enemy";
        return "<!italic><dark_gray>Relation: <gray>Neutral";
    }

    private ItemStack makeSkull(UUID ownerUUID) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        if (ownerUUID != null && skull.getItemMeta() instanceof SkullMeta meta) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(ownerUUID);
            meta.setOwningPlayer(op);
            skull.setItemMeta(meta);
        }
        return skull;
    }

    private Team safeGetTeam(Player player) {
        try {
            return TeamManager.getInstance().getTeam(player.getUniqueId());
        } catch (Exception e) {
            return null;
        }
    }
}

