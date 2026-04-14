package dev.frost.frostcore.manager;

import dev.frost.frostcore.mace.*;
import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.TeamEchestManager;
import dev.frost.frostcore.manager.TeamManager;
import dev.frost.frostcore.utils.FrostLogger;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bukkit.scheduler.BukkitTask;

public class MaceManager {

    private static MaceManager instance;
    private final MaceDatabase maceDb;
    private final ConfigManager config;
    private final NamespacedKey maceIdKey;

    private final Map<String, MaceEntry> activeMaces = new ConcurrentHashMap<>();
    private final Set<String> pendingRemovals = ConcurrentHashMap.newKeySet();

    private boolean enabled;
    private int maxMacesOverall;
    private int maxMacesPerPlayer;
    private String untrackedAction;
    private boolean notifyStaff;
    private final Map<String, Integer> enchantmentLimits = new LinkedHashMap<>();
    private List<String> restrictedWorlds = new ArrayList<>();
    private double pvpCooldown;
    private double damageCap;
    private boolean disableOnDeath;
    private List<Material> trackedMaterials = new ArrayList<>();

    private final Map<UUID, Long> pvpCooldowns = new ConcurrentHashMap<>();

    private BukkitTask pendingRemovalTask;

    public MaceManager(MaceDatabase maceDb) {
        this.maceDb = maceDb;
        this.config = Main.getConfigManager();
        this.maceIdKey = new NamespacedKey(Main.getInstance(), "mace_id");
        instance = this;
        reload();
        startPendingRemovalTask();
    }

    public static MaceManager getInstance() {
        return instance;
    }

    public void reload() {
        enabled = config.getBoolean("mace-limiter.enabled", true);
        maxMacesOverall = config.getInt("mace-limiter.max-maces-overall", 3);
        maxMacesPerPlayer = config.getInt("mace-limiter.max-maces-per-player", 1);
        untrackedAction = config.getString("mace-limiter.untracked-mace-action", "TRACK");
        notifyStaff = config.getBoolean("mace-limiter.notify-staff", true);
        pvpCooldown = config.getDouble("mace-limiter.pvp-cooldown", 0);
        damageCap = config.getDouble("mace-limiter.damage-cap", 0);
        disableOnDeath = config.getBoolean("mace-limiter.disable-on-death", false);

        enchantmentLimits.clear();
        var enchSection = config.getConfig().getConfigurationSection("mace-limiter.enchantment-limits");
        if (enchSection != null) {
            for (String key : enchSection.getKeys(false)) {
                enchantmentLimits.put(key.toUpperCase(), enchSection.getInt(key));
            }
        }

        restrictedWorlds = config.getStringList("mace-limiter.restricted-worlds")
                .stream().map(String::toLowerCase).collect(Collectors.toList());

        trackedMaterials.clear();
        List<String> matNames = config.getStringList("mace-limiter.tracked-materials");
        if (matNames.isEmpty()) matNames = List.of("MACE");
        for (String name : matNames) {
            try {
                trackedMaterials.add(Material.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException e) {
                FrostLogger.warn("Unknown material in mace-limiter.tracked-materials: " + name);
            }
        }

        activeMaces.clear();
        for (MaceEntry entry : maceDb.loadAllActiveMaces()) {
            activeMaces.put(entry.maceId(), entry);
        }

        pendingRemovals.clear();
        pendingRemovals.addAll(maceDb.loadPendingRemovals());

        FrostLogger.info("Mace Limiter: " + (enabled ? "enabled" : "disabled")
                + " | " + activeMaces.size() + "/" + maxMacesOverall + " maces tracked."
                + (pendingRemovals.isEmpty() ? "" : " | " + pendingRemovals.size() + " pending removals."));
    }

    public boolean isEnabled() { return enabled; }

    public boolean isTrackedMaterial(Material mat) {
        return trackedMaterials.contains(mat);
    }

    public boolean isTrackedMace(ItemStack item) {
        if (item == null || !isTrackedMaterial(item.getType())) return false;
        return getMaceId(item) != null;
    }

    public String getMaceId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.get(maceIdKey, PersistentDataType.STRING);
    }

    public NamespacedKey getMaceIdKey() {
        return maceIdKey;
    }

    public ItemStack stampMace(ItemStack item, String maceId) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.getPersistentDataContainer().set(maceIdKey, PersistentDataType.STRING, maceId);
        item.setItemMeta(meta);
        return item;
    }

    public boolean canCraftMace(Player player) {
        if (!enabled) return true;
        if (player.hasPermission("frostcore.mace.bypass")) return true;
        if (activeMaces.size() >= maxMacesOverall) return false;
        if (maxMacesPerPlayer > 0) {
            long held = activeMaces.values().stream()
                    .filter(e -> player.getUniqueId().equals(e.currentHolder()))
                    .count();
            return held < maxMacesPerPlayer;
        }
        return true;
    }

    public boolean hasReachedPlayerLimit(Player player) {
        if (!enabled || maxMacesPerPlayer <= 0) return false;
        if (player.hasPermission("frostcore.mace.bypass")) return false;
        long held = activeMaces.values().stream()
                .filter(e -> player.getUniqueId().equals(e.currentHolder()))
                .count();
        return held >= maxMacesPerPlayer;
    }

    public boolean hasReachedGlobalLimit() {
        return enabled && activeMaces.size() >= maxMacesOverall;
    }

    public boolean isEnchantAllowed(String enchantName, int level) {
        Integer cap = enchantmentLimits.get(enchantName.toUpperCase());
        if (cap == null) return true;
        return level <= cap;
    }

    public int getEnchantCap(String enchantName) {
        return enchantmentLimits.getOrDefault(enchantName.toUpperCase(), -1);
    }

    public boolean isWorldRestricted(String worldName) {
        return restrictedWorlds.contains(worldName.toLowerCase());
    }

    public boolean hasPvpCooldown() { return pvpCooldown > 0; }

    public boolean isOnPvpCooldown(UUID uuid) {
        if (pvpCooldown <= 0) return false;
        Long last = pvpCooldowns.get(uuid);
        if (last == null) return false;
        return (System.currentTimeMillis() - last) < (pvpCooldown * 1000);
    }

    public long getPvpCooldownRemaining(UUID uuid) {
        if (pvpCooldown <= 0) return 0;
        Long last = pvpCooldowns.get(uuid);
        if (last == null) return 0;
        long remaining = (long)(pvpCooldown * 1000) - (System.currentTimeMillis() - last);
        return Math.max(0, remaining);
    }

    public void setPvpCooldown(UUID uuid) {
        pvpCooldowns.put(uuid, System.currentTimeMillis());
    }

    public double getDamageCap() { return damageCap; }
    public boolean isDisableOnDeath() { return disableOnDeath; }
    public String getUntrackedAction() { return untrackedAction; }
    public int getMaxMacesOverall() { return maxMacesOverall; }
    public int getMaxMacesPerPlayer() { return maxMacesPerPlayer; }
    public Map<String, Integer> getEnchantmentLimits() { return Collections.unmodifiableMap(enchantmentLimits); }
    public List<String> getRestrictedWorlds() { return Collections.unmodifiableList(restrictedWorlds); }
    public double getPvpCooldownSeconds() { return pvpCooldown; }

    public MaceEntry registerMace(Player crafter, ItemStack item) {
        String id = UUID.randomUUID().toString();
        stampMace(item, id);

        Location loc = crafter.getLocation();
        String enchants = serializeEnchantments(item);

        MaceEntry entry = new MaceEntry(
                id, crafter.getUniqueId(), crafter.getName(), System.currentTimeMillis(),
                crafter.getUniqueId(), crafter.getName(), System.currentTimeMillis(),
                loc.getWorld() != null ? loc.getWorld().getName() : "unknown",
                loc.getX(), loc.getY(), loc.getZ(), enchants, false
        );

        activeMaces.put(id, entry);
        maceDb.insertMaceAsync(entry);

        if (notifyStaff) {
            notifyStaff("<#6B8DAE>MACE <dark_gray>» <#8FA3BF>" + crafter.getName()
                    + " <dark_gray>crafted a mace <#8FA3BF>#" + entry.shortId()
                    + " <dark_gray>(" + activeMaces.size() + "/" + maxMacesOverall + ")");
        }

        FrostLogger.audit("[MACE] " + crafter.getName() + " crafted mace #" + entry.shortId());
        return entry;
    }

    public MaceEntry registerUntrackedMace(Player holder, ItemStack item) {
        String id = UUID.randomUUID().toString();
        stampMace(item, id);

        Location loc = holder.getLocation();
        String enchants = serializeEnchantments(item);

        MaceEntry entry = new MaceEntry(
                id, null, "Unknown", System.currentTimeMillis(),
                holder.getUniqueId(), holder.getName(), System.currentTimeMillis(),
                loc.getWorld() != null ? loc.getWorld().getName() : "unknown",
                loc.getX(), loc.getY(), loc.getZ(), enchants, false
        );

        activeMaces.put(id, entry);
        maceDb.insertMaceAsync(entry);

        if (notifyStaff) {
            notifyStaff("<#6B8DAE>MACE <dark_gray>» <#D4A76A>Auto-tracked <#8FA3BF>unregistered mace from "
                    + holder.getName() + " <dark_gray>#" + entry.shortId());
        }

        return entry;
    }

    public void updateHolder(String maceId, Player player) {
        MaceEntry entry = activeMaces.get(maceId);
        if (entry == null) return;

        Location loc = player.getLocation();
        MaceEntry updated = entry.withHolder(player.getUniqueId(), player.getName())
                .withLocation(loc.getWorld() != null ? loc.getWorld().getName() : "unknown",
                        loc.getX(), loc.getY(), loc.getZ());
        activeMaces.put(maceId, updated);
        maceDb.updateMaceAsync(updated);
    }

    public void updateLocation(String maceId, Location loc) {
        MaceEntry entry = activeMaces.get(maceId);
        if (entry == null) return;

        MaceEntry updated = entry.withLocation(
                loc.getWorld() != null ? loc.getWorld().getName() : "unknown",
                loc.getX(), loc.getY(), loc.getZ());
        activeMaces.put(maceId, updated);
        maceDb.updateMaceAsync(updated);
    }

    public void updateEnchantments(String maceId, ItemStack item) {
        MaceEntry entry = activeMaces.get(maceId);
        if (entry == null) return;

        MaceEntry updated = entry.withEnchantments(serializeEnchantments(item));
        activeMaces.put(maceId, updated);
        maceDb.updateMaceAsync(updated);
    }

    public void clearHolder(String maceId) {
        MaceEntry entry = activeMaces.get(maceId);
        if (entry == null) return;

        MaceEntry updated = new MaceEntry(
                entry.maceId(), entry.originalCrafter(), entry.crafterName(), entry.craftedAt(),
                null, null, System.currentTimeMillis(),
                entry.lastWorld(), entry.lastX(), entry.lastY(), entry.lastZ(),
                entry.enchantments(), entry.destroyed()
        );
        activeMaces.put(maceId, updated);
        maceDb.updateMaceAsync(updated);
    }

    public void destroyMace(String maceId) {
        MaceEntry entry = activeMaces.get(maceId);
        if (entry == null) return;

        MaceEntry updated = entry.withDestroyed(true);
        activeMaces.remove(maceId);
        maceDb.updateMaceAsync(updated);

        if (notifyStaff) {
            notifyStaff("<#6B8DAE>MACE <dark_gray>» <#D4727A>Mace #" + entry.shortId()
                    + " destroyed <dark_gray>(" + activeMaces.size() + "/" + maxMacesOverall + ")");
        }

        FrostLogger.audit("[MACE] Mace #" + entry.shortId() + " destroyed.");
    }

    public boolean physicallyRemoveMace(String maceId) {
        boolean found = false;

        MaceEntry entry = activeMaces.get(maceId);

        if (entry != null && entry.currentHolder() != null) {
            Player holder = Bukkit.getPlayer(entry.currentHolder());
            if (holder != null && holder.isOnline()) {
                found = removeFromInventory(holder.getInventory(), maceId);
            }
        }

        if (!found) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (removeFromInventory(online.getInventory(), maceId)) {
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Item itemEntity) {
                        ItemStack stack = itemEntity.getItemStack();
                        String id = getMaceId(stack);
                        if (maceId.equals(id)) {
                            itemEntity.remove();
                            found = true;
                            break;
                        }
                    }
                }
                if (found) break;
            }
        }

        destroyMace(maceId);

        if (!found) {
            pendingRemovals.add(maceId);
            maceDb.insertPendingRemovalAsync(maceId);
            FrostLogger.audit("[MACE] Mace #" + maceId.substring(0, 8) + " queued for removal on next holder login.");
        } else {
            pendingRemovals.remove(maceId);
            maceDb.removePendingRemovalAsync(maceId);
        }

        return found;
    }

    private boolean removeFromInventory(Inventory inv, String maceId) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null) continue;
            String id = getMaceId(item);
            if (maceId.equals(id)) {
                inv.setItem(i, null);
                return true;
            }
        }
        return false;
    }

    public void resetAll() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < online.getInventory().getSize(); i++) {
                ItemStack item = online.getInventory().getItem(i);
                if (item != null && getMaceId(item) != null) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.getPersistentDataContainer().remove(maceIdKey);
                        item.setItemMeta(meta);
                    }
                }
            }
        }
        activeMaces.clear();
        pendingRemovals.clear();
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            maceDb.hardDeleteAllDestroyed();
            for (String id : maceDb.loadPendingRemovals()) {
                maceDb.removePendingRemoval(id);
            }
        });
        FrostLogger.info("Mace registry reset.");
    }

    public void setMaxMacesOverall(int max) {
        this.maxMacesOverall = max;
        config.setAndSave("mace-limiter.max-maces-overall", max);
    }

    public void setMaxMacesPerPlayer(int max) {
        this.maxMacesPerPlayer = max;
        config.setAndSave("mace-limiter.max-maces-per-player", max);
    }

    public void setEnchantLimit(String enchant, int level) {
        enchantmentLimits.put(enchant.toUpperCase(), level);
        config.set("mace-limiter.enchantment-limits." + enchant.toUpperCase(), level);
        config.saveConfig();
    }

    public void removeEnchantLimit(String enchant) {
        enchantmentLimits.remove(enchant.toUpperCase());
        config.set("mace-limiter.enchantment-limits." + enchant.toUpperCase(), null);
        config.saveConfig();
    }

    public void setPvpCooldownSeconds(double seconds) {
        this.pvpCooldown = seconds;
        config.setAndSave("mace-limiter.pvp-cooldown", seconds);
    }

    public void setDamageCap(double cap) {
        this.damageCap = cap;
        config.setAndSave("mace-limiter.damage-cap", cap);
    }

    public void setDisableOnDeath(boolean val) {
        this.disableOnDeath = val;
        config.setAndSave("mace-limiter.disable-on-death", val);
    }

    public void setEnabled(boolean val) {
        this.enabled = val;
        config.setAndSave("mace-limiter.enabled", val);
    }

    public MaceEntry getMaceEntry(String maceId) {
        return activeMaces.get(maceId);
    }

    public Collection<MaceEntry> getAllActiveMaces() {
        return Collections.unmodifiableCollection(activeMaces.values());
    }

    public List<MaceEntry> getMacesByPlayer(UUID uuid) {
        return activeMaces.values().stream()
                .filter(e -> uuid.equals(e.currentHolder()))
                .collect(Collectors.toList());
    }

    public int getActiveMaceCount() {
        return activeMaces.size();
    }

    public MaceDatabase getDatabase() { return maceDb; }

    public Set<String> getPendingRemovals() { return Collections.unmodifiableSet(pendingRemovals); }

    public void processPendingRemovals(Player player) {
        if (pendingRemovals.isEmpty()) return;

        scanAndRemovePending(player.getInventory(), player.getName());

        scanAndRemovePending(player.getEnderChest(), player.getName() + "'s enderchest");

        TeamManager tm = Main.getTeamManager();
        TeamEchestManager echestMgr = Main.getEchestManager();
        if (tm != null && echestMgr != null && tm.hasTeam(player.getUniqueId())) {
            try {
                dev.frost.frostcore.teams.Team team = tm.getTeam(player.getUniqueId());
                String teamKey = team.getName().toLowerCase();

                Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
                    String base64 = Main.getDatabaseManager().loadEchest(teamKey);
                    if (base64 == null) return;

                    ItemStack[] items = dev.frost.frostcore.utils.ItemStackSerializer.fromBase64(base64);
                    if (items == null) return;

                    boolean modified = false;
                    for (int i = 0; i < items.length; i++) {
                        if (items[i] == null) continue;
                        String maceId = getMaceId(items[i]);
                        if (maceId != null && pendingRemovals.contains(maceId)) {
                            items[i] = null;
                            pendingRemovals.remove(maceId);
                            maceDb.removePendingRemovalAsync(maceId);
                            modified = true;

                            if (notifyStaff) {
                                final String shortId = maceId.substring(0, 8);
                                Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                                    notifyStaff("<#6B8DAE>MACE <dark_gray>» <#7ECFA0>Queued mace #" + shortId
                                            + " removed from team echest (" + teamKey + ").")
                                );
                            }
                            FrostLogger.audit("[MACE] Pending mace #" + maceId.substring(0, 8)
                                    + " removed from team echest (" + teamKey + ").");
                        }
                    }

                    if (modified) {
                        String newBase64 = dev.frost.frostcore.utils.ItemStackSerializer.toBase64(items);
                        Main.getDatabaseManager().saveEchest(teamKey, newBase64);
                    }
                });
            } catch (dev.frost.frostcore.exceptions.TeamException ignored) {}
        }
    }

    private void scanAndRemovePending(Inventory inv, String source) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null) continue;
            String maceId = getMaceId(item);
            if (maceId != null && pendingRemovals.contains(maceId)) {
                inv.setItem(i, null);
                pendingRemovals.remove(maceId);
                maceDb.removePendingRemovalAsync(maceId);

                if (notifyStaff) {
                    notifyStaff("<#6B8DAE>MACE <dark_gray>» <#7ECFA0>Queued mace #" + maceId.substring(0, 8)
                            + " removed from " + source + ".");
                }
                FrostLogger.audit("[MACE] Pending mace #" + maceId.substring(0, 8)
                        + " removed from " + source + ".");
            }
        }
    }

    private void startPendingRemovalTask() {
        if (pendingRemovalTask != null) pendingRemovalTask.cancel();

        pendingRemovalTask = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            if (!enabled || pendingRemovals.isEmpty()) return;

            for (Player online : Bukkit.getOnlinePlayers()) {
                scanAndRemovePending(online.getInventory(), online.getName());
                scanAndRemovePending(online.getEnderChest(), online.getName() + "'s enderchest");
            }
        }, 1200L, 1200L);
    }

    public static String serializeEnchantments(ItemStack item) {
        if (item == null || item.getEnchantments().isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Enchantment, Integer> e : item.getEnchantments().entrySet()) {
            if (!sb.isEmpty()) sb.append(",");
            sb.append(e.getKey().getKey().getKey().toUpperCase()).append(":").append(e.getValue());
        }
        return sb.toString();
    }

    public static Map<String, Integer> parseEnchantments(String serialized) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (serialized == null || serialized.isEmpty()) return result;
        for (String part : serialized.split(",")) {
            String[] split = part.split(":");
            if (split.length == 2) {
                try {
                    result.put(split[0].toUpperCase(), Integer.parseInt(split[1]));
                } catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }

    private void notifyStaff(String minimsg) {
        MessageManager mm = Main.getMessageManager();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("frostcore.mace.notify")) {
                mm.sendRaw(online, minimsg);
            }
        }
        FrostLogger.info("[MACE] " + net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(minimsg)));
    }
}
