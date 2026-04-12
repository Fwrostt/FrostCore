package dev.frost.frostcore.gui.impls;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.gui.Button;
import dev.frost.frostcore.gui.Gui;
import dev.frost.frostcore.gui.GuiItem;
import dev.frost.frostcore.gui.GuiTemplate;
import dev.frost.frostcore.gui.Slot;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.manager.HomeManager;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.teams.Team;
import dev.frost.frostcore.utils.TeleportUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomesGui extends Gui {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String BED_TITLE = "<!italic><#8BADC4>Home: <white>";
    private static final String SEP = "<!italic><dark_gray>──────────────";

    private final Player viewer;
    private final HomeManager homeManager;
    private final TeleportUtil teleportUtil;
    private final ConfigManager config;
    private final MessageManager mm;

    private final int[] contentSlots;
    private int currentPage = 0;

    public HomesGui(Player viewer) {
        super(
            MM.deserialize("<!italic><gradient:#6B8DAE:#8BADC4>Personal Homes"),
            Main.getConfigManager().getInt("homes.gui.rows", 4)
        );

        this.viewer       = viewer;
        this.homeManager  = Main.getHomeManager();
        this.teleportUtil = Main.getTeleportUtil();
        this.config       = Main.getConfigManager();
        this.mm           = Main.getMessageManager();

        int rows      = getRows();
        int startRow  = config.getInt("homes.gui.content.start-row", 1);
        int endRow    = Math.min(config.getInt("homes.gui.content.end-row",  rows - 2), rows - 2);
        int startCol  = config.getInt("homes.gui.content.start-col", 2);
        int endCol    = config.getInt("homes.gui.content.end-col",   8);

        this.contentSlots = Slot.rectangle(startRow, startCol, endRow, endCol);
    }

    @Override
    public void populate() {
        clear();
        forceFillBorder(GuiTemplate.blackFiller());

        if (config.getBoolean("gui.borders", true)) {
            GuiItem pane = GuiTemplate.filler(Material.GRAY_STAINED_GLASS_PANE);
            int rowsSeparator = getRows() - 1;
            for (int r = 1; r < rowsSeparator; r++) {
                setItem(r, 2, pane);
                setItem(r, 7, pane);
            }
        }

        populateTeamHome();

        int maxHomes = homeManager.getMaxHomes(viewer);
        Map<String, Location> currentHomesMap = homeManager.getHomes(viewer);
        List<String> homeNames = new ArrayList<>(currentHomesMap.keySet());

        int pageSize = contentSlots.length;
        int totalPages = Math.max(1, (int) Math.ceil((double) maxHomes / pageSize));
        currentPage = Math.min(currentPage, totalPages - 1);

        int start = currentPage * pageSize;

        for (int i = 0; i < pageSize; i++) {
            int slotIdx = start + i;
            if (slotIdx >= maxHomes) {

                setItem(contentSlots[i], Button.of(Material.BARRIER)
                        .name("<!italic><red>Locked Slot")
                        .lore("<!italic><dark_gray>You have reached max homes (" + maxHomes + ")")
                        .build());
                continue;
            }

            if (slotIdx < homeNames.size()) {

                String hName = homeNames.get(slotIdx);
                Location loc = currentHomesMap.get(hName);
                setItem(contentSlots[i], buildSetHome(hName, loc));
            } else {

                setItem(contentSlots[i], buildUnsetHome(slotIdx + 1));
            }
        }

        buildNavigation(totalPages);
    }

    private void populateTeamHome() {
        Team team = null;
        try {
            team = Main.getTeamManager().getTeam(viewer.getUniqueId());
        } catch (Exception ignored) {}

        if (team == null) {
            setItem(1, 1, Button.of(Material.WHITE_BANNER)
                    .name("<!italic><dark_gray>Team Home")
                    .lore("<!italic><red>You are not in a team.")
                    .build());
            return;
        }

        boolean hasHome = team.getHome() != null;

        Button.Builder banner = Button.of(Material.CYAN_BANNER)
                .name("<!italic><#8BADC4>★ Team Home")
                .glow();

        if (hasHome) {
            banner.lore("<!italic><dark_gray>Click to teleport");
            setItem(1, 1, banner.onClick(ctx -> {
                ctx.close();
                viewer.chat("/team home");
            }).build());

            boolean isAdmin = team.isOwner(viewer.getUniqueId()) || team.isAdmin(viewer.getUniqueId());
            Button.Builder delBtn = Button.of(isAdmin ? Material.LIGHT_BLUE_DYE : Material.GRAY_DYE)
                    .name(isAdmin ? "<!italic><#8BADC4>DELETE" : "<!italic><#D4727A>DELETE")
                    .lore(isAdmin ? "<!italic><white>Click to delete team home" : "<!italic><dark_gray>Requires admin/owner");

            if (isAdmin) {
                delBtn.onClick(ctx -> {
                    ctx.close();
                    viewer.chat("/team delhome");
                });
            }

            setItem(2, 1, delBtn.build());
        } else {
            banner.lore("<!italic><dark_gray>Not set yet");
            setItem(1, 1, banner.build());
        }
    }

    private GuiItem buildSetHome(String name, Location loc) {
        return Button.of(Material.RED_BED)
                .name(BED_TITLE + name)
                .lore(
                    SEP,
                    "<!italic><#8BADC4>▪ <gray>Left-Click to <white>teleport",
                    "<!italic><#8FA3BF>▪ <gray>Middle-Click to <white>rename",
                    "<!italic><#D4727A>▪ <gray>Right-Click to <white>delete",
                    SEP
                )
                .onClick(ctx -> {
                    ctx.cancel();
                    if (ctx.isLeftClick()) {
                        ctx.close();
                        viewer.chat("/home " + name);
                    } else if (ctx.isRightClick()) {
                        ctx.close();
                        viewer.chat("/delhome " + name);
                    } else if (ctx.isMiddleClick()) {
                        ctx.close();
                        Component msg = MM.deserialize("{homes.prefix}<#8FA3BF>Click here or type <white>/renamehome " + name + " <newname></white> to rename.")
                                .replaceText(b -> b.matchLiteral("{homes.prefix}").replacement(MM.deserialize(mm.getRaw("homes.prefix"))))
                                .clickEvent(ClickEvent.suggestCommand("/renamehome " + name + " "));
                        viewer.sendMessage(msg);
                    }
                })
                .build();
    }

    private GuiItem buildUnsetHome(int num) {
        return Button.of(Material.LIGHT_GRAY_DYE)
                .name("<!italic><gray>NO HOME SET")
                .lore(
                    SEP,
                    "<!italic><dark_gray>Click to save your",
                    "<!italic><dark_gray>current location here."
                )
                .onClick(ctx -> {
                    ctx.close();
                    String defName = "Home";
                    if (homeManager.getHome(viewer, defName) != null) {
                        defName = "Home" + num;
                    }
                    viewer.chat("/sethome " + defName);
                })
                .build();
    }

    private void buildNavigation(int totalPages) {
        int navRow = getRows() - 1;
        for (int c = 1; c <= 7; c++) {
            setItem(navRow, c, GuiTemplate.blackFiller());
        }

        setItem(navRow, 4, Button.of(Material.PAPER)
                .name("<!italic><gray>Page <white>" + (currentPage + 1) + " <dark_gray>/ <gray>" + totalPages)
                .build());

        if (currentPage > 0) {
            setItem(Slot.bottomLeft(getRows()), Button.of(Material.SPECTRAL_ARROW)
                    .name("<!italic><#8FA3BF>◀ Previous")
                    .onClick(ctx -> {
                        currentPage--;
                        refresh(viewer);
                    }).build());
        }

        if (currentPage < totalPages - 1) {
            setItem(Slot.bottomRight(getRows()), Button.of(Material.ARROW)
                    .name("<!italic><#8FA3BF>Next ▶")
                    .onClick(ctx -> {
                        currentPage++;
                        refresh(viewer);
                    }).build());
        }
    }
}

