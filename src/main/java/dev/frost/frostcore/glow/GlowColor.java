package dev.frost.frostcore.glow;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

public enum GlowColor {

    WHITE       ("White",        "<white>",        NamedTextColor.WHITE,        Material.WHITE_WOOL,          "#FFFFFF"),
    LIGHT_GRAY  ("Light Gray",   "<gray>",         NamedTextColor.GRAY,         Material.LIGHT_GRAY_WOOL,     "#AAAAAA"),
    GRAY        ("Gray",         "<dark_gray>",    NamedTextColor.DARK_GRAY,    Material.GRAY_WOOL,           "#555555"),
    BLACK       ("Black",        "<black>",        NamedTextColor.BLACK,        Material.BLACK_WOOL,          "#000000"),

    RED         ("Red",          "<red>",          NamedTextColor.RED,          Material.RED_WOOL,            "#FF5555"),
    DARK_RED    ("Dark Red",     "<dark_red>",     NamedTextColor.DARK_RED,     Material.RED_WOOL,            "#AA0000"),
    ORANGE      ("Orange",       "<gold>",         NamedTextColor.GOLD,         Material.ORANGE_WOOL,         "#FFAA00"),
    YELLOW      ("Yellow",       "<yellow>",       NamedTextColor.YELLOW,       Material.YELLOW_WOOL,         "#FFFF55"),

    LIME        ("Lime",         "<green>",        NamedTextColor.GREEN,        Material.LIME_WOOL,           "#55FF55"),
    GREEN       ("Green",        "<dark_green>",   NamedTextColor.DARK_GREEN,   Material.GREEN_WOOL,          "#00AA00"),

    AQUA        ("Aqua",         "<aqua>",         NamedTextColor.AQUA,         Material.LIGHT_BLUE_WOOL,     "#55FFFF"),
    CYAN        ("Cyan",         "<dark_aqua>",    NamedTextColor.DARK_AQUA,    Material.CYAN_WOOL,           "#00AAAA"),

    BLUE        ("Blue",         "<blue>",         NamedTextColor.BLUE,         Material.BLUE_WOOL,           "#5555FF"),
    DARK_BLUE   ("Dark Blue",    "<dark_blue>",    NamedTextColor.DARK_BLUE,    Material.BLUE_WOOL,           "#0000AA"),

    PINK        ("Pink",         "<light_purple>", NamedTextColor.LIGHT_PURPLE, Material.PINK_WOOL,           "#FF55FF"),
    PURPLE      ("Purple",       "<dark_purple>",  NamedTextColor.DARK_PURPLE,  Material.PURPLE_WOOL,         "#AA00AA");
    
    private final String displayName;
    private final String miniMessageTag;
    private final NamedTextColor namedColor;
    private final Material guiMaterial;
    private final String hex;

    GlowColor(String displayName, String miniMessageTag, NamedTextColor namedColor,
              Material guiMaterial, String hex) {
        this.displayName = displayName;
        this.miniMessageTag = miniMessageTag;
        this.namedColor = namedColor;
        this.guiMaterial = guiMaterial;
        this.hex = hex;
    }

    public String getDisplayName()    { return displayName; }
    public String getMiniMessageTag() { return miniMessageTag; }
    public NamedTextColor getNamedColor() { return namedColor; }
    public Material getGuiMaterial()  { return guiMaterial; }
    public String getHex()            { return hex; }

    public String getPermission() {
        return "frostcore.glow." + name().toLowerCase();
    }

    public String getColoredName() {
        return "<!italic><" + hex + ">" + displayName;
    }

    public static GlowColor fromName(String name) {
        if (name == null) return null;
        for (GlowColor c : values()) {
            if (c.name().equalsIgnoreCase(name) || c.displayName.equalsIgnoreCase(name)
                    || c.name().replace("_", "").equalsIgnoreCase(name.replace("_", "").replace(" ", ""))) {
                return c;
            }
        }
        return null;
    }
}
