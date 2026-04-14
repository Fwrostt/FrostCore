package dev.frost.frostcore.gui.impls;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.gui.*;
import dev.frost.frostcore.glow.*;
import dev.frost.frostcore.manager.GlowManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class GlowGui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void open(Player viewer) {
        GlowManager mgr = Main.getGlowManager();
        GlowColor currentGlow = mgr.getGlow(viewer);

        SimpleGui.Builder builder = SimpleGui.builder("<!italic><gradient:#6B8DAE:#8BADC4>Glow Colors", 4)
                .border(GuiTemplate.blackFiller());

        builder.item(0, 4, Button.of(Material.GLOWSTONE)
                .name("<!italic><gradient:#6B8DAE:#8BADC4>Glow Selector")
                .lore(
                        "<!italic><dark_gray>Current: " + (currentGlow != null ? currentGlow.getColoredName() : "<#707880>None"),
                        "",
                        "<!italic><gray>Select a color below",
                        "<!italic><gray>to toggle your glow."
                )
                .glow()
                .build());

        if (currentGlow != null) {
            builder.item(3, 4, Button.of(Material.BARRIER)
                    .name("<!italic><#D4727A>Remove Glow")
                    .lore(
                            "<!italic><dark_gray>Currently glowing: " + currentGlow.getColoredName(),
                            "",
                            "<!italic><gray>▸ Click to remove"
                    )
                    .onClick(ctx -> {
                        mgr.removeGlow(ctx.getPlayer());
                        Main.getMessageManager().sendRaw(ctx.getPlayer(),
                                "<#6B8DAE>GLOW <dark_gray>» <#8FA3BF>Glow removed.");
                        ctx.close();
                        GuiManager.schedule(() -> GlowGui.open(ctx.getPlayer()));
                    })
                    .build());
        }

        GlowColor[] colors = GlowColor.values();
        int[] slots = Slot.rectangle(1, 1, 2, 7);

        for (int i = 0; i < colors.length && i < slots.length; i++) {
            GlowColor color = colors[i];
            boolean hasAccess = mgr.hasPermission(viewer, color);
            boolean isActive = color == currentGlow;

            if (hasAccess) {
                Button.Builder btn = Button.of(color.getGuiMaterial())
                        .name(color.getColoredName())
                        .lore(
                                "<!italic><dark_gray>Permission: <#7ECFA0>Unlocked",
                                isActive ? "<!italic><dark_gray>Status: <#7ECFA0>Active\n\n" : "",
                                isActive ? "<!italic><gray>▸ Click to remove" : "<!italic><gray>▸ Click to apply"
                        );

                if (isActive) btn.glow();

                btn.onClick(ctx -> {
                    Player clicker = ctx.getPlayer();
                    if (isActive) {
                        mgr.removeGlow(clicker);
                        Main.getMessageManager().sendRaw(clicker,
                                "<#6B8DAE>GLOW <dark_gray>» <#8FA3BF>Glow removed.");
                    } else {
                        mgr.setGlow(clicker, color);
                        Main.getMessageManager().sendRaw(clicker,
                                "<#6B8DAE>GLOW <dark_gray>» " + color.getColoredName() + " <#8FA3BF>glow applied.");
                    }
                    ctx.close();
                    GuiManager.schedule(() -> GlowGui.open(clicker));
                });

                builder.item(slots[i], btn.build());
            } else {
                builder.item(slots[i], Button.of(Material.GRAY_STAINED_GLASS_PANE)
                        .name("<!italic><#707880>" + color.getDisplayName())
                        .lore(
                                "<!italic><dark_gray>Permission: <#D4727A>Locked",
                                "",
                                "<!italic><#707880>" + color.getPermission()
                        )
                        .build());
            }
        }

        builder.build().open(viewer);
    }
}
