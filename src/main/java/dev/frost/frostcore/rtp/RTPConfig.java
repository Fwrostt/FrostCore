package dev.frost.frostcore.rtp;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.utils.FrostLogger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parses and caches all RTP configuration from config.yml and rtp.yml.
 * Supports per-world settings, EnumSet-based unsafe block lookups,
 * GUI customization via rtp.yml, and startup validation of configured worlds.
 */
public class RTPConfig {

    private final Main plugin;
    private final ConfigManager config;

    // ── Global settings ──────────────────────────────────────────
    private boolean enabled;
    private boolean guiEnabled;
    private int cooldown;
    private int delay;
    private boolean cancelOnMove;
    private int maxAttempts;
    private int poolSize;
    private long poolRefillInterval;
    private EnumSet<Material> unsafeBlocks;

    // ── Defaults ─────────────────────────────────────────────────
    private int defaultMinRadius;
    private int defaultMaxRadius;
    private int defaultMinY;
    private int defaultMaxY;
    private double defaultCost;
    private boolean defaultAllowWater;
    private boolean defaultAllowLava;
    private int defaultCenterX;
    private int defaultCenterZ;

    // ── GUI settings (from rtp.yml) ──────────────────────────────
    private FileConfiguration rtpYml;
    private String guiTitle;
    private Material fillerMaterial;
    private String fillerName;

    // ── GUI dynamic lore formats ─────────────────────────────────
    private String loreSeparator;
    private String loreCost;
    private String loreCostFree;
    private String loreCostBypass;
    private String loreCooldown;
    private String loreCooldownReady;
    private String loreCooldownBypass;
    private String loreRadius;
    private String loreCached;
    private String loreFooter;

    // ── Per-world ────────────────────────────────────────────────
    private final Map<String, WorldSettings> worldSettings = new ConcurrentHashMap<>();

    public RTPConfig(Main plugin) {
        this.plugin = plugin;
        this.config = Main.getConfigManager();
        reload();
    }

    /**
     * Reloads all RTP configuration from config.yml and rtp.yml.
     * Validates world names against Bukkit.getWorlds() at load time.
     */
    public void reload() {
        enabled = config.getBoolean("rtp.enabled", true);
        guiEnabled = config.getBoolean("rtp.gui-enabled", true);
        cooldown = config.getInt("rtp.cooldown", 60);
        delay = config.getInt("rtp.delay", 3);
        cancelOnMove = config.getBoolean("rtp.cancel-on-move", true);
        maxAttempts = config.getInt("rtp.max-attempts", 50);
        poolSize = config.getInt("rtp.pool.size", 10);
        poolRefillInterval = config.getInt("rtp.pool.refill-interval", 100);

        // ── Defaults ─────────────────────────────────────────────
        defaultMinRadius = config.getInt("rtp.defaults.min-radius", 500);
        defaultMaxRadius = config.getInt("rtp.defaults.max-radius", 5000);
        defaultMinY = config.getInt("rtp.defaults.min-y", 60);
        defaultMaxY = config.getInt("rtp.defaults.max-y", 200);
        defaultCost = config.getDouble("rtp.defaults.cost", 0);
        defaultAllowWater = config.getBoolean("rtp.defaults.allow-water", false);
        defaultAllowLava = config.getBoolean("rtp.defaults.allow-lava", false);
        defaultCenterX = config.getInt("rtp.defaults.center-x", 0);
        defaultCenterZ = config.getInt("rtp.defaults.center-z", 0);

        // ── Unsafe blocks (EnumSet for O(1) lookup) ──────────────
        unsafeBlocks = EnumSet.noneOf(Material.class);
        List<String> unsafeList = config.getStringList("rtp.unsafe-blocks");
        for (String name : unsafeList) {
            try {
                unsafeBlocks.add(Material.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException e) {
                FrostLogger.warn("[RTP] Invalid unsafe block material: " + name);
            }
        }

        // ── Load rtp.yml (GUI config) ────────────────────────────
        loadRtpYml();

        // ── Per-world settings ───────────────────────────────────
        worldSettings.clear();
        var cfgSection = config.getConfig().getConfigurationSection("rtp.worlds");
        if (cfgSection != null) {
            for (String worldName : cfgSection.getKeys(false)) {
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    FrostLogger.warn("[RTP] Configured world '" + worldName + "' does not exist — skipping.");
                    continue;
                }

                String path = "rtp.worlds." + worldName;
                String guiPath = "worlds." + worldName;

                WorldSettings ws = new WorldSettings(
                    worldName,
                    config.getInt(path + ".min-radius", defaultMinRadius),
                    config.getInt(path + ".max-radius", defaultMaxRadius),
                    config.getInt(path + ".min-y", defaultMinY),
                    config.getInt(path + ".max-y", defaultMaxY),
                    config.getDouble(path + ".cost", defaultCost),
                    config.getBoolean(path + ".allow-water", defaultAllowWater),
                    config.getBoolean(path + ".allow-lava", defaultAllowLava),
                    config.getInt(path + ".center-x", defaultCenterX),
                    config.getInt(path + ".center-z", defaultCenterZ),
                    parseMaterial(rtpYml.getString(guiPath + ".material", guessDefaultMaterial(world))),
                    rtpYml.getString(guiPath + ".name", "<!italic><white>" + worldName),
                    rtpYml.getStringList(guiPath + ".lore"),
                    rtpYml.getBoolean(guiPath + ".glow", false)
                );
                worldSettings.put(worldName, ws);
            }
        }

        FrostLogger.info("[RTP] Loaded " + worldSettings.size() + " RTP world(s).");
    }

    /**
     * Loads rtp.yml GUI configuration. Creates the file from defaults if missing.
     */
    private void loadRtpYml() {
        File rtpFile = new File(plugin.getDataFolder(), "rtp.yml");
        if (!rtpFile.exists()) {
            plugin.saveResource("rtp.yml", false);
        }
        rtpYml = YamlConfiguration.loadConfiguration(rtpFile);

        guiTitle = rtpYml.getString("gui.title", "<!italic><gradient:#6B8DAE:#8BADC4>✦ Random Teleport ✦");
        fillerMaterial = parseMaterial(rtpYml.getString("gui.filler.material", "GRAY_STAINED_GLASS_PANE"));
        fillerName = rtpYml.getString("gui.filler.name", "<reset>");

        loreSeparator = rtpYml.getString("gui.dynamic-lore.separator", "<!italic><dark_gray>  ━━━━━━━━━━━━━━━━━━━━━━");
        loreCost = rtpYml.getString("gui.dynamic-lore.cost", "<!italic><dark_gray>  ▸ <gray>Cost: <white>{cost}");
        loreCostFree = rtpYml.getString("gui.dynamic-lore.cost-free", "<!italic><dark_gray>  ▸ <#7ECFA0>Free");
        loreCostBypass = rtpYml.getString("gui.dynamic-lore.cost-bypass", "<!italic><dark_gray>  ▸ <#7ECFA0>Free <dark_gray>(bypass)");
        loreCooldown = rtpYml.getString("gui.dynamic-lore.cooldown", "<!italic><dark_gray>  ▸ <#D4727A>Cooldown: <white>{time}s");
        loreCooldownReady = rtpYml.getString("gui.dynamic-lore.cooldown-ready", "<!italic><dark_gray>  ▸ <#7ECFA0>Ready");
        loreCooldownBypass = rtpYml.getString("gui.dynamic-lore.cooldown-bypass", "<!italic><dark_gray>  ▸ <#7ECFA0>Ready <dark_gray>(bypass)");
        loreRadius = rtpYml.getString("gui.dynamic-lore.radius", "<!italic><dark_gray>  ▸ <gray>Radius: <white>{min} - {max}");
        loreCached = rtpYml.getString("gui.dynamic-lore.cached", "<!italic><dark_gray>  ▸ <gray>Cached: <white>{count} location{s}");
        loreFooter = rtpYml.getString("gui.dynamic-lore.footer", "<!italic>  <#8BADC4>▶ Click to teleport!");
    }

    private String guessDefaultMaterial(World world) {
        return switch (world.getEnvironment()) {
            case NETHER -> "NETHERRACK";
            case THE_END -> "END_STONE";
            default -> "GRASS_BLOCK";
        };
    }

    private Material parseMaterial(String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.GRASS_BLOCK;
        }
    }

    // ── Getters ──────────────────────────────────────────────────

    public boolean isEnabled()             { return enabled; }
    public boolean isGuiEnabled()          { return guiEnabled; }
    public int getCooldown()               { return cooldown; }
    public int getDelay()                  { return delay; }
    public boolean isCancelOnMove()        { return cancelOnMove; }
    public int getMaxAttempts()            { return maxAttempts; }
    public int getPoolSize()               { return poolSize; }
    public long getPoolRefillInterval()    { return poolRefillInterval; }
    public EnumSet<Material> getUnsafeBlocks() { return unsafeBlocks; }

    // ── GUI getters ──────────────────────────────────────────────

    public String getGuiTitle()            { return guiTitle; }
    public Material getFillerMaterial()    { return fillerMaterial; }
    public String getFillerName()          { return fillerName; }

    // ── GUI dynamic lore getters ─────────────────────────────────

    public String getLoreSeparator()       { return loreSeparator; }
    public String getLoreCost()            { return loreCost; }
    public String getLoreCostFree()        { return loreCostFree; }
    public String getLoreCostBypass()      { return loreCostBypass; }
    public String getLoreCooldown()        { return loreCooldown; }
    public String getLoreCooldownReady()   { return loreCooldownReady; }
    public String getLoreCooldownBypass()  { return loreCooldownBypass; }
    public String getLoreRadius()          { return loreRadius; }
    public String getLoreCached()          { return loreCached; }
    public String getLoreFooter()          { return loreFooter; }

    public WorldSettings getWorldSettings(String worldName) {
        return worldSettings.get(worldName);
    }

    public Set<String> getEnabledWorlds() {
        return Collections.unmodifiableSet(worldSettings.keySet());
    }

    // ── WorldSettings inner class ────────────────────────────────

    public static class WorldSettings {
        private final String worldName;
        private final int minRadius;
        private final int maxRadius;
        private final int minY;
        private final int maxY;
        private final double cost;
        private final boolean allowWater;
        private final boolean allowLava;
        private final int centerX;
        private final int centerZ;
        private final Material guiMaterial;
        private final String guiName;
        private final List<String> guiLore;
        private final boolean guiGlow;

        public WorldSettings(String worldName, int minRadius, int maxRadius,
                             int minY, int maxY, double cost,
                             boolean allowWater, boolean allowLava,
                             int centerX, int centerZ,
                             Material guiMaterial, String guiName,
                             List<String> guiLore, boolean guiGlow) {
            this.worldName = worldName;
            this.minRadius = minRadius;
            this.maxRadius = maxRadius;
            this.minY = minY;
            this.maxY = maxY;
            this.cost = cost;
            this.allowWater = allowWater;
            this.allowLava = allowLava;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.guiMaterial = guiMaterial;
            this.guiName = guiName;
            this.guiLore = guiLore != null ? guiLore : Collections.emptyList();
            this.guiGlow = guiGlow;
        }

        public String getWorldName()      { return worldName; }
        public int getMinRadius()          { return minRadius; }
        public int getMaxRadius()          { return maxRadius; }
        public int getMinY()               { return minY; }
        public int getMaxY()               { return maxY; }
        public double getCost()            { return cost; }
        public boolean isAllowWater()      { return allowWater; }
        public boolean isAllowLava()       { return allowLava; }
        public int getCenterX()            { return centerX; }
        public int getCenterZ()            { return centerZ; }
        public Material getGuiMaterial()   { return guiMaterial; }
        public String getGuiName()         { return guiName; }
        public List<String> getGuiLore()   { return guiLore; }
        public boolean isGuiGlow()         { return guiGlow; }
    }
}
