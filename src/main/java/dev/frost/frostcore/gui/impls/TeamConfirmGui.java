package dev.frost.frostcore.gui.impls;

import dev.frost.frostcore.gui.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;


public class TeamConfirmGui extends Gui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final String GOLD    = "<#D4C4A8>";
    private static final String BLUE    = "<#8FA3BF>";
    private static final String DIM     = "<#707880>";
    private static final String POS     = "<#7ECFA0>";
    private static final String NEG     = "<#D4727A>";
    private static final String SEP     = "<!italic><dark_gray>──────────────";

    private final Material           infoIcon;
    private final String             infoTitle;
    private final List<String>       infoLore;
    private final GuiAction<ClickContext> onConfirm;
    private final GuiAction<ClickContext> onCancel;

    private TeamConfirmGui(String title, Material infoIcon, String infoTitle,
                           List<String> infoLore,
                           GuiAction<ClickContext> onConfirm,
                           GuiAction<ClickContext> onCancel) {
        super(MM.deserialize(title), 3);
        this.infoIcon  = infoIcon;
        this.infoTitle = infoTitle;
        this.infoLore  = List.copyOf(infoLore);
        this.onConfirm = onConfirm;
        this.onCancel  = onCancel;
    }

    @Override
    public void populate() {
        clear();
        forceFillBorder(GuiTemplate.blackFiller());

        setItem(0, 4, Button.of(infoIcon)
                .name(infoTitle)
                .lore(infoLore)
                .build());

        GuiItem confirmBtn = Button.of(Material.LIME_DYE)
                .name("<!italic>" + POS + "✔  Confirm")
                .lore("<!italic>" + DIM + "Click to confirm")
                .glow()
                .onClick(onConfirm)
                .build();

        GuiItem cancelBtn = Button.of(Material.RED_DYE)
                .name("<!italic>" + NEG + "✗  Cancel")
                .lore("<!italic>" + DIM + "Click to cancel")
                .onClick(onCancel)
                .build();

        GuiItem divider = GuiTemplate.filler(Material.WHITE_STAINED_GLASS_PANE);

        setItem(1, 2, confirmBtn);
        setItem(1, 4, divider);
        setItem(1, 6, cancelBtn);
    }

    
    public static void openDisband(Player player, String teamName,
                                   GuiAction<ClickContext> onConfirm) {
        List<String> lore = new ArrayList<>();
        lore.add(SEP);
        lore.add("<!italic>" + GOLD + "Team: <white>" + teamName);
        lore.add(SEP);
        lore.add("<!italic>" + NEG + "This cannot be undone.");
        lore.add("<!italic><dark_gray>All members will lose their team.");
        lore.add("<!italic><dark_gray>Team data will be deleted.");

        new TeamConfirmGui(
                "<!italic>" + NEG + "Disband Team?",
                Material.TNT,
                "<!italic>" + NEG + "Disband " + teamName + "?",
                lore,
                onConfirm,
                ClickContext::close
        ).open(player);
    }

    
    public static void openLeave(Player player, String teamName,
                                 GuiAction<ClickContext> onConfirm) {
        List<String> lore = new ArrayList<>();
        lore.add(SEP);
        lore.add("<!italic>" + GOLD + "Team: <white>" + teamName);
        lore.add(SEP);
        lore.add("<!italic><dark_gray>You will lose access to team features.");
        lore.add("<!italic><dark_gray>You can be re-invited at any time.");

        new TeamConfirmGui(
                "<!italic>" + GOLD + "Leave Team?",
                Material.OAK_DOOR,
                "<!italic>" + GOLD + "Leave " + teamName + "?",
                lore,
                onConfirm,
                ClickContext::close
        ).open(player);
    }

    
    public static void openJoin(Player player, String teamName, String senderName,
                                GuiAction<ClickContext> onConfirm) {
        List<String> lore = new ArrayList<>();
        lore.add(SEP);
        lore.add("<!italic>" + GOLD + "Team: <white>" + teamName);
        lore.add("<!italic><dark_gray>Invited by: <white>" + senderName);
        lore.add(SEP);
        lore.add("<!italic><dark_gray>You will become a member of");
        lore.add("<!italic><white>" + teamName + "<dark_gray>.");

        new TeamConfirmGui(
                "<!italic>" + POS + "Join Team?",
                Material.EMERALD,
                "<!italic>" + POS + "Join " + teamName + "?",
                lore,
                onConfirm,
                ClickContext::close
        ).open(player);
    }
}

