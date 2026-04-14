package dev.frost.frostcore.gui;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.mace.*;
import dev.frost.frostcore.utils.TeleportUtil;
import dev.frost.frostcore.manager.MaceManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public class MaceGui extends Gui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Player viewer;
    private final List<MaceEntry> entries;
    private final int[] contentSlots;
    private int currentPage = 0;

    private MaceGui(Player viewer, List<MaceEntry> entries) {
        super(MM.deserialize("<!italic><#6B8DAE>Mace Registry"), 5);
        this.viewer = viewer;
        this.entries = entries;
        this.contentSlots = Slot.rectangle(1, 1, 3, 7);
    }

    public static void openRegistry(Player viewer) {
        MaceManager mgr = Main.getMaceManager();
        List<MaceEntry> entries = new ArrayList<>(mgr.getAllActiveMaces());
        entries.sort(Comparator.comparingLong(MaceEntry::craftedAt).reversed());
        new MaceGui(viewer, entries).open(viewer);
    }

    @Override
    public void populate() {
        clear();
        forceFillBorder(GuiTemplate.blackFiller());

        MaceManager mgr = Main.getMaceManager();

        setItem(0, 4, Button.of(Material.MACE)
                .name("<!italic><gradient:#6B8DAE:#8BADC4>Mace Registry")
                .lore(
                        "<!italic><dark_gray>Active: <white>" + mgr.getActiveMaceCount() + "<dark_gray>/" + mgr.getMaxMacesOverall(),
                        "<!italic><dark_gray>Per Player: <white>" + mgr.getMaxMacesPerPlayer(),
                        "<!italic><dark_gray>Status: " + (mgr.isEnabled() ? "<#7ECFA0>Enabled" : "<#D4727A>Disabled"),
                        "",
                        "<!italic><dark_gray>Recipe: " + (mgr.hasReachedGlobalLimit() ? "<#D4727A>Locked" : "<#7ECFA0>Available"),
                        "",
                        "<!italic><gray>▸ Click to open settings"
                )
                .glow()
                .onClick(ctx -> openSettings(ctx.getPlayer()))
                .build());

        int pageSize = contentSlots.length;
        int total = entries.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        currentPage = Math.min(currentPage, totalPages - 1);

        int start = currentPage * pageSize;
        int itemsOnPage = Math.min(pageSize, total - start);

        for (int i = 0; i < itemsOnPage; i++) {
            MaceEntry entry = entries.get(start + i);
            setItem(contentSlots[i], buildMaceItem(entry));
        }

        int navRow = getRows() - 1;
        setItem(navRow, 4, Button.of(Material.PAPER)
                .name("<!italic><gray>Page <white>" + (currentPage + 1) + " <dark_gray>/ <gray>" + totalPages)
                .lore("<!italic><dark_gray>" + total + " mace" + (total == 1 ? "" : "s") + " total")
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

    private GuiItem buildMaceItem(MaceEntry entry) {
        List<String> lore = new ArrayList<>();
        lore.add("<!italic><dark_gray>ID: <#8FA3BF>#" + entry.shortId());
        lore.add("");

        lore.add("<!italic><dark_gray>Crafter: <white>" + (entry.crafterName() != null ? entry.crafterName() : "Unknown"));
        lore.add("<!italic><dark_gray>Crafted: <white>" + entry.getFormattedAge() + " ago");
        lore.add("");

        if (entry.holderName() != null) {
            lore.add("<!italic><dark_gray>Holder: <#7ECFA0>" + entry.holderName());
        } else {
            lore.add("<!italic><dark_gray>Holder: <#D4A76A>None (dropped/container)");
        }
        lore.add("<!italic><dark_gray>Last Seen: <white>" + entry.getFormattedLastSeen());

        if (entry.lastWorld() != null) {
            lore.add("<!italic><dark_gray>Location: <white>" + entry.lastWorld()
                    + " <dark_gray>(" + (int) entry.lastX() + ", " + (int) entry.lastY() + ", " + (int) entry.lastZ() + ")");
        }

        Map<String, Integer> enchants = MaceManager.parseEnchantments(entry.enchantments());
        if (!enchants.isEmpty()) {
            lore.add("");
            lore.add("<!italic><dark_gray>Enchantments:");
            for (Map.Entry<String, Integer> e : enchants.entrySet()) {
                String name = e.getKey().substring(0, 1) + e.getKey().substring(1).toLowerCase();
                lore.add("<!italic>  <#8FA3BF>▸ <white>" + name + " " + e.getValue());
            }
        }

        lore.add("");
        lore.add("<!italic><gray>▸ Left-click to teleport");
        lore.add("<!italic><gray>▸ Right-click to destroy & remove");

        Material headMat = Material.PLAYER_HEAD;
        Button.Builder btn;
        if (entry.currentHolder() != null) {
            btn = Button.of(headMat).skull(entry.currentHolder());
        } else {
            btn = Button.of(Material.MACE);
        }

        return btn
                .name("<!italic><#6B8DAE>Mace #" + entry.shortId() + " <dark_gray>» <white>"
                        + (entry.holderName() != null ? entry.holderName() : "Unknown"))
                .lore(lore)
                .onClick(ctx -> {
                    if (ctx.isRightClick()) {
                        ctx.close();
                        GuiTemplate.confirm(
                                "<!italic><#D4727A>Destroy Mace #" + entry.shortId() + "?",
                                confirmCtx -> {
                                    confirmCtx.close();
                                    Main.getMaceManager().physicallyRemoveMace(entry.maceId());
                                    Main.getMessageManager().sendRaw(ctx.getPlayer(),
                                            "<#6B8DAE>MACE <dark_gray>» <#7ECFA0>Mace #" + entry.shortId() + " destroyed and removed.");
                                },
                                cancelCtx -> {
                                    cancelCtx.close();
                                    GuiManager.schedule(() -> MaceGui.openRegistry(ctx.getPlayer()));
                                }
                        ).open(ctx.getPlayer());
                    } else {
                        if (entry.lastWorld() != null) {
                            World world = Bukkit.getWorld(entry.lastWorld());
                            if (world != null) {
                                ctx.close();
                                Location loc = new Location(world, entry.lastX(), entry.lastY(), entry.lastZ());
                                ctx.getPlayer().teleport(loc);
                                Main.getMessageManager().sendRaw(ctx.getPlayer(),
                                        "<#6B8DAE>MACE <dark_gray>» <#8FA3BF>Teleported to mace #" + entry.shortId());
                            }
                        }
                    }
                })
                .build();
    }

    public static void openSettings(Player viewer) {
        MaceManager mgr = Main.getMaceManager();

        SimpleGui.builder("<!italic><gradient:#6B8DAE:#8BADC4>Mace Settings", 5)
                .border(GuiTemplate.blackFiller())

                .item(1, 1, Button.of(mgr.isEnabled() ? Material.LIME_DYE : Material.RED_DYE)
                        .name("<!italic><white>System Status")
                        .lore(
                                "<!italic><dark_gray>Currently: " + (mgr.isEnabled() ? "<#7ECFA0>Enabled" : "<#D4727A>Disabled"),
                                "",
                                "<!italic><gray>▸ Click to toggle"
                        )
                        .glow()
                        .onClick(ctx -> {
                            mgr.setEnabled(!mgr.isEnabled());
                            openSettings(ctx.getPlayer());
                        })
                        .build())

                .item(1, 3, Button.of(Material.MACE)
                        .name("<!italic><white>Max Maces (Server)")
                        .lore(
                                "<!italic><dark_gray>Current: <white>" + mgr.getMaxMacesOverall(),
                                "<!italic><dark_gray>Active: <white>" + mgr.getActiveMaceCount(),
                                "",
                                "<!italic><gray>▸ Left-click: +1",
                                "<!italic><gray>▸ Right-click: -1",
                                "<!italic><gray>▸ Shift-click: +5 / -5"
                        )
                        .glow()
                        .onClick(ctx -> {
                            int delta = ctx.isShiftClick() ? 5 : 1;
                            if (ctx.isRightClick()) delta = -delta;
                            int newVal = Math.max(1, mgr.getMaxMacesOverall() + delta);
                            mgr.setMaxMacesOverall(newVal);
                            openSettings(ctx.getPlayer());
                        })
                        .build())

                .item(1, 5, Button.of(Material.PLAYER_HEAD)
                        .name("<!italic><white>Max Maces (Per Player)")
                        .lore(
                                "<!italic><dark_gray>Current: <white>" + mgr.getMaxMacesPerPlayer(),
                                "<!italic><dark_gray>0 = unlimited",
                                "",
                                "<!italic><gray>▸ Left-click: +1",
                                "<!italic><gray>▸ Right-click: -1"
                        )
                        .onClick(ctx -> {
                            int delta = ctx.isRightClick() ? -1 : 1;
                            int newVal = Math.max(0, mgr.getMaxMacesPerPlayer() + delta);
                            mgr.setMaxMacesPerPlayer(newVal);
                            openSettings(ctx.getPlayer());
                        })
                        .build())

                .item(1, 7, Button.of(Material.DIAMOND_SWORD)
                        .name("<!italic><white>PvP Cooldown")
                        .lore(
                                "<!italic><dark_gray>Current: <white>" + String.format("%.1f", mgr.getPvpCooldownSeconds()) + "s",
                                "<!italic><dark_gray>0 = disabled",
                                "",
                                "<!italic><gray>▸ Left-click: +0.5s",
                                "<!italic><gray>▸ Right-click: -0.5s"
                        )
                        .onClick(ctx -> {
                            double delta = ctx.isRightClick() ? -0.5 : 0.5;
                            double newVal = Math.max(0, mgr.getPvpCooldownSeconds() + delta);
                            mgr.setPvpCooldownSeconds(newVal);
                            openSettings(ctx.getPlayer());
                        })
                        .build())

                .item(2, 1, Button.of(Material.IRON_SWORD)
                        .name("<!italic><white>Damage Cap")
                        .lore(
                                "<!italic><dark_gray>Current: <white>" + (mgr.getDamageCap() <= 0 ? "Unlimited" : String.format("%.1f", mgr.getDamageCap())),
                                "",
                                "<!italic><gray>▸ Left-click: +5",
                                "<!italic><gray>▸ Right-click: -5",
                                "<!italic><gray>▸ Middle-click: Reset"
                        )
                        .onClick(ctx -> {
                            if (ctx.isMiddleClick()) {
                                mgr.setDamageCap(0);
                            } else {
                                double delta = ctx.isRightClick() ? -5 : 5;
                                double newVal = Math.max(0, mgr.getDamageCap() + delta);
                                mgr.setDamageCap(newVal);
                            }
                            openSettings(ctx.getPlayer());
                        })
                        .build())

                .item(2, 3, Button.of(mgr.isDisableOnDeath() ? Material.SKELETON_SKULL : Material.TOTEM_OF_UNDYING)
                        .name("<!italic><white>Destroy on Death")
                        .lore(
                                "<!italic><dark_gray>Currently: " + (mgr.isDisableOnDeath() ? "<#D4727A>Enabled" : "<#7ECFA0>Disabled"),
                                "",
                                "<!italic><gray>▸ Click to toggle"
                        )
                        .onClick(ctx -> {
                            mgr.setDisableOnDeath(!mgr.isDisableOnDeath());
                            openSettings(ctx.getPlayer());
                        })
                        .build())

                .item(2, 5, buildEnchantButton("DENSITY", mgr))
                .item(2, 7, buildEnchantButton("BREACH", mgr))
                .item(3, 1, buildEnchantButton("WIND_BURST", mgr))

                .item(3, 4, Button.of(Material.BOOK)
                        .name("<!italic><gradient:#6B8DAE:#8BADC4>◀ Back to Registry")
                        .lore("<!italic><gray>Return to mace list")
                        .onClick(ctx -> {
                            ctx.close();
                            GuiManager.schedule(() -> MaceGui.openRegistry(ctx.getPlayer()));
                        })
                        .build())

                .item(3, 7, Button.of(Material.TNT)
                        .name("<!italic><#D4727A>Reset All Maces")
                        .lore(
                                "<!italic><dark_gray>Removes ALL mace tracking data",
                                "<!italic><dark_gray>Does NOT delete mace items from inventories",
                                "",
                                "<!italic><gray>▸ Click for confirmation"
                        )
                        .onClick(ctx -> {
                            ctx.close();
                            GuiTemplate.confirm(
                                    "<!italic><#D4727A>Reset ALL Mace Data?",
                                    confirmCtx -> {
                                        confirmCtx.close();
                                        mgr.resetAll();
                                        Main.getMessageManager().sendRaw(confirmCtx.getPlayer(),
                                                "<#6B8DAE>MACE <dark_gray>» <#7ECFA0>All mace data has been reset.");
                                    },
                                    cancelCtx -> {
                                        cancelCtx.close();
                                        openSettings(cancelCtx.getPlayer());
                                    }
                            ).open(ctx.getPlayer());
                        })
                        .build())

                .build()
                .open(viewer);
    }

    private static GuiItem buildEnchantButton(String enchantName, MaceManager mgr) {
        int cap = mgr.getEnchantCap(enchantName);
        String display = enchantName.substring(0, 1) + enchantName.substring(1).toLowerCase().replace("_", " ");
        String capText = cap == -1 ? "Unlimited" : String.valueOf(cap);

        return Button.of(Material.ENCHANTED_BOOK)
                .name("<!italic><#C8A87C>" + display + " Limit")
                .lore(
                        "<!italic><dark_gray>Max Level: <white>" + capText,
                        "",
                        "<!italic><gray>▸ Left-click: +1",
                        "<!italic><gray>▸ Right-click: -1",
                        "<!italic><gray>▸ Middle-click: Remove limit"
                )
                .onClick(ctx -> {
                    if (ctx.isMiddleClick()) {
                        mgr.removeEnchantLimit(enchantName);
                    } else {
                        int current = mgr.getEnchantCap(enchantName);
                        if (current == -1) current = 5;
                        int delta = ctx.isRightClick() ? -1 : 1;
                        int newVal = Math.max(1, current + delta);
                        mgr.setEnchantLimit(enchantName, newVal);
                    }
                    openSettings(ctx.getPlayer());
                })
                .build();
    }
}
