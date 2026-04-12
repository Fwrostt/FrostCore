package dev.frost.frostcore.manager;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the GUI display configuration for a single server warp.
 * <p>
 * Values are persisted to {@code warps.yml} and loaded on startup.
 * A default configuration is auto-generated for every new warp created via {@code /setwarp}.
 */
public class WarpItemConfig {

    private String displayName;
    private Material material;
    private List<String> lore;
    private boolean glow;
    private String permission;

    public WarpItemConfig(String displayName, Material material,
                          List<String> lore, boolean glow, String permission) {
        this.displayName = displayName;
        this.material    = material;
        this.lore        = new ArrayList<>(lore);
        this.glow        = glow;
        this.permission  = permission == null ? "" : permission;
    }

    /**
     * Build a sensible default config for a warp that doesn't yet have one.
     * Called automatically when {@code /setwarp <name>} is run.
     */
    public static WarpItemConfig defaultFor(String warpName) {
        String capitalized = warpName.substring(0, 1).toUpperCase() + warpName.substring(1);
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("<gray>▸ Click to teleport to <white>" + capitalized);
        lore.add("");
        return new WarpItemConfig(
                "<gradient:#6B8DAE:#8BADC4><bold>⬡ " + capitalized + "</bold>",
                Material.ENDER_PEARL,
                lore,
                false,
                ""
        );
    }

    /**
     * Write this config into a {@link ConfigurationSection} (a key under {@code warps.<name>}).
     */
    public void saveTo(ConfigurationSection section) {
        section.set("display-name", displayName);
        section.set("material", material.name());
        section.set("lore", lore);
        section.set("glow", glow);
        section.set("permission", permission);
    }

    /**
     * Load a {@link WarpItemConfig} from a {@link ConfigurationSection}.
     * Falls back to {@link #defaultFor(String)} for any missing fields.
     */
    public static WarpItemConfig loadFrom(ConfigurationSection section, String warpName) {
        String displayName = section.getString("display-name",
                "<gradient:#6B8DAE:#8BADC4><bold>⬡ " + warpName + "</bold>");

        String matName = section.getString("material", "ENDER_PEARL");
        Material material;
        try {
            material = Material.valueOf(matName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.ENDER_PEARL;
        }

        List<String> lore = section.getStringList("lore");
        if (lore.isEmpty()) {
            lore = List.of("", "<gray>▸ Click to teleport", "");
        }

        boolean glow       = section.getBoolean("glow", false);
        String  permission = section.getString("permission", "");

        return new WarpItemConfig(displayName, material, lore, glow, permission);
    }

    public String       getDisplayName()              { return displayName; }
    public void         setDisplayName(String n)      { this.displayName = n; }

    public Material     getMaterial()                 { return material; }
    public void         setMaterial(Material m)       { this.material = m; }

    public List<String> getLore()                     { return lore; }
    public void         setLore(List<String> l)       { this.lore = new ArrayList<>(l); }

    public boolean      isGlow()                      { return glow; }
    public void         setGlow(boolean g)            { this.glow = g; }

    public String       getPermission()               { return permission; }
    public void         setPermission(String p)       { this.permission = p == null ? "" : p; }

    /** Returns true if this warp requires a specific permission. */
    public boolean requiresPermission()               { return !permission.isEmpty(); }
}

