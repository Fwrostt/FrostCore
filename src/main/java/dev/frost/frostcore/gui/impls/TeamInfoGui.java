package dev.frost.frostcore.gui.impls;

import dev.frost.frostcore.gui.*;
import dev.frost.frostcore.teams.Team;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * Public team info panel.
 *
 * <h3>5-row layout</h3>
 * <pre>
 *  Row 0  ◾ ◾ ◾ ◾  [Team skull + header]  ◾ ◾ ◾ ◾
 *  Row 1  ◾ P  P  P  P  P  P  │  [Stats    ]
 *  Row 2  ◾ P  P  P  P  P  P  │  [Relations]
 *  Row 3  ◾ P  P  P  P  P  P  │  ◾
 *  Row 4  ◾ ◾ ◾ ◾  [← Back]  ◾ ◾ ◾ ◾
 * </pre>
 *
 * Player heads fill cols 1–6 across rows 1–3 (18 slots).
 * Order: owners → admins → members.
 * If the team has more than 18 members the overflow count appears in the Stats lore.
 * Warp names are intentionally omitted (private information).
 */
public class TeamInfoGui extends Gui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final String GOLD      = "<#FFD27F>";
    private static final String BLUE      = "<#A3C4FF>";
    private static final String SKY       = "<#81CFFA>";
    private static final String SILVER    = "<#BBBBBB>";
    private static final String DIM       = "<#888888>";
    private static final String POS       = "<#78D97B>";
    private static final String NEG       = "<#E07070>";
    private static final String SEP_LINE  = "<!italic><dark_gray>──────────────";

    private static final int[] PLAYER_SLOTS = Slot.rectangle(1, 1, 3, 6);

    private final Team   team;
    private final Player viewer;

    public TeamInfoGui(Player viewer, Team team) {
        super(MM.deserialize("<!italic><gradient:#FFD700:#FFA500>" + team.getDisplayName()), 5);
        this.team   = team;
        this.viewer = viewer;
    }

    @Override
    public void populate() {
        clear();

        forceFillBorder(GuiTemplate.blackFiller());

        if (dev.frost.frostcore.Main.getConfigManager().getBoolean("gui.borders", true)) {
            for (int r = 1; r <= 3; r++) {
                setItem(r, 7, GuiTemplate.filler(Material.GRAY_STAINED_GLASS_PANE));
            }
        }

        UUID primaryOwner = team.getOwners().isEmpty() ? null
                : team.getOwners().iterator().next();
        setItem(0, 4, buildHeaderItem(primaryOwner));

        List<RosterEntry> roster = buildRoster();
        int shown    = Math.min(roster.size(), PLAYER_SLOTS.length);
        int overflow = Math.max(0, roster.size() - PLAYER_SLOTS.length);

        for (int i = 0; i < shown; i++) {
            setItem(PLAYER_SLOTS[i], buildMemberHead(roster.get(i)));
        }

        setItem(1, 8, buildStatsItem(overflow));
        setItem(2, 8, buildRelationsItem());

        setItem(4, 4, Button.of(Material.ARROW)
                .name("<!italic>" + BLUE + "← Back to Teams")
                .lore("<!italic>" + DIM + "Return to the team list")
                .onClick(ctx -> GuiManager.schedule(() -> new TeamListGui(viewer).open(viewer)))
                .build());
    }

    private GuiItem buildHeaderItem(UUID ownerUUID) {
        return Button.of(makeSkull(ownerUUID))
                .name("<!italic><gradient:#FFD700:#FFA500>" + team.getDisplayName())
                .lore(
                    "<!italic><dark_gray>Tag  <dark_gray>│ <white>[" + team.getTag() + "]",
                    "<!italic><dark_gray>Size <dark_gray>│ <white>" + team.getTotalMembers() + " members",
                    "<!italic><dark_gray>PvP  <dark_gray>│ " + (team.isPvpToggle() ? POS + "Enabled" : NEG + "Disabled"),
                    "<!italic><dark_gray>Home <dark_gray>│ " + (team.getHome() != null ? POS + "Set" : DIM + "Not set")
                )
                .build();
    }

    private GuiItem buildMemberHead(RosterEntry entry) {
        OfflinePlayer op   = Bukkit.getOfflinePlayer(entry.uuid());
        String        name = op.getName() != null ? op.getName()
                                                  : entry.uuid().toString().substring(0, 8);

        String roleColor = switch (entry.role()) {
            case OWNER  -> GOLD;
            case ADMIN  -> SKY;
            case MEMBER -> SILVER;
        };
        String roleLabel = switch (entry.role()) {
            case OWNER  -> "Owner";
            case ADMIN  -> "Admin";
            case MEMBER -> "Member";
        };

        return Button.of(makeSkull(entry.uuid()))
                .name("<!italic>" + roleColor + name)
                .lore(
                    "<!italic><dark_gray>Role   │ " + roleColor + roleLabel,
                    "<!italic><dark_gray>Status │ " + (op.isOnline() ? POS + "Online" : DIM + "Offline")
                )
                .build();
    }

    private GuiItem buildStatsItem(int overflow) {
        List<String> lore = new ArrayList<>();
        lore.add(SEP_LINE);
        lore.add("<!italic>" + GOLD   + "Owners  <dark_gray>│ <white>" + team.getOwners().size());
        lore.add("<!italic>" + SKY    + "Admins  <dark_gray>│ <white>" + team.getAdmins().size());
        lore.add("<!italic>" + SILVER + "Members <dark_gray>│ <white>" + team.getMembers().size());
        lore.add(SEP_LINE);
        lore.add("<!italic><dark_gray>PvP  <dark_gray>│ " + (team.isPvpToggle() ? POS + "ON" : NEG + "OFF"));
        lore.add("<!italic><dark_gray>Home <dark_gray>│ " + (team.getHome() != null ? POS + "Set" : DIM + "─"));
        if (overflow > 0) {
            lore.add(SEP_LINE);
            lore.add("<!italic>" + DIM + "+ " + overflow + " more member" + (overflow == 1 ? "" : "s") + " not shown");
        }

        return Button.of(Material.PAPER)
                .name("<!italic>" + BLUE + "Team Stats")
                .lore(lore)
                .build();
    }

    private GuiItem buildRelationsItem() {
        List<String> lore = new ArrayList<>();

        boolean hasRelations = !team.getAllies().isEmpty() || !team.getEnemies().isEmpty();
        if (!hasRelations) {
            lore.add(SEP_LINE);
            lore.add("<!italic>" + DIM + "No declared relations");
        } else {
            if (!team.getAllies().isEmpty()) {
                lore.add(SEP_LINE);
                lore.add("<!italic>" + POS + "Allies");
                for (String ally : team.getAllies()) {
                    lore.add("<!italic><dark_gray>  · " + POS + capitalize(ally));
                }
            }
            if (!team.getEnemies().isEmpty()) {
                lore.add(SEP_LINE);
                lore.add("<!italic>" + NEG + "Enemies");
                for (String enemy : team.getEnemies()) {
                    lore.add("<!italic><dark_gray>  · " + NEG + capitalize(enemy));
                }
            }
        }

        return Button.of(Material.COMPASS)
                .name("<!italic>" + BLUE + "Relations")
                .lore(lore)
                .build();
    }

    private enum Role { OWNER, ADMIN, MEMBER }

    private record RosterEntry(UUID uuid, Role role) {}

    private List<RosterEntry> buildRoster() {
        List<RosterEntry> list = new ArrayList<>();
        for (UUID u : team.getOwners())  list.add(new RosterEntry(u, Role.OWNER));
        for (UUID u : team.getAdmins())  list.add(new RosterEntry(u, Role.ADMIN));
        for (UUID u : team.getMembers()) list.add(new RosterEntry(u, Role.MEMBER));
        return list;
    }

    private ItemStack makeSkull(UUID uuid) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        if (uuid != null && skull.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
            skull.setItemMeta(meta);
        }
        return skull;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}

