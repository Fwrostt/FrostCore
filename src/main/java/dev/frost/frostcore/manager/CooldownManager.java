package dev.frost.frostcore.manager;

import dev.frost.frostcore.database.DatabaseManager;
import dev.frost.frostcore.utils.FrostLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, globally-accessible cooldown tracker.
 * Cooldowns survive server restarts via the {@code player_cooldowns} database table.
 * All mutating methods are safe to call from async threads.
 */
public class CooldownManager {

    private static final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    private static DatabaseManager db;

    private CooldownManager() {}

    /**
     * Call once during onEnable, after DatabaseManager is initialised.
     * Injects the database reference and loads persisted cooldowns into memory.
     */
    public static void init(DatabaseManager database) {
        db = database;
        loadCooldowns();
    }

    /**
     * Set a cooldown for a player.
     *
     * @param player          The player.
     * @param id              Cooldown ID (e.g. "tpa", "spawn").
     * @param durationSeconds Cooldown duration in seconds.
     */
    public static void setCooldown(Player player, String id, int durationSeconds) {
        long expiry = System.currentTimeMillis() + durationSeconds * 1000L;
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                 .put(id, expiry);
        saveCooldownAsync(player.getUniqueId(), id, expiry);
    }

    /**
     * Checks if a player is currently on cooldown for a specific ID.
     *
     * @return True if on cooldown, false otherwise.
     */
    public static boolean isOnCooldown(Player player, String id) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return false;

        Long expiry = playerCooldowns.get(id);
        if (expiry == null) return false;

        if (System.currentTimeMillis() >= expiry) {
            playerCooldowns.remove(id);
            if (playerCooldowns.isEmpty()) cooldowns.remove(player.getUniqueId());
            deleteCooldownAsync(player.getUniqueId(), id);
            return false;
        }
        return true;
    }

    /**
     * Gets the remaining cooldown time in seconds (0 if not on cooldown).
     */
    public static int getRemainingTime(Player player, String id) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return 0;

        Long expiry = playerCooldowns.get(id);
        if (expiry == null) return 0;

        long remainingMs = expiry - System.currentTimeMillis();
        if (remainingMs <= 0) {
            playerCooldowns.remove(id);
            if (playerCooldowns.isEmpty()) cooldowns.remove(player.getUniqueId());
            deleteCooldownAsync(player.getUniqueId(), id);
            return 0;
        }
        return (int) Math.ceil(remainingMs / 1000.0);
    }

    /**
     * Clears a specific cooldown for a player.
     */
    public static void clearCooldown(Player player, String id) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns != null) {
            playerCooldowns.remove(id);
            if (playerCooldowns.isEmpty()) cooldowns.remove(player.getUniqueId());
            deleteCooldownAsync(player.getUniqueId(), id);
        }
    }

    /**
     * Clears all cooldowns for a player.
     */
    public static void clearAllCooldowns(Player player) {
        cooldowns.remove(player.getUniqueId());
        deleteAllCooldownsAsync(player.getUniqueId());
    }

    /**
     * Load all non-expired cooldowns from the database into memory.
     * Called synchronously on startup.
     */
    public static void loadCooldowns() {
        if (db == null) return;
        Map<UUID, Map<String, Long>> loaded = db.loadAllCooldowns();
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Map<String, Long>> entry : loaded.entrySet()) {
            Map<String, Long> active = new ConcurrentHashMap<>();
            for (Map.Entry<String, Long> cd : entry.getValue().entrySet()) {
                if (cd.getValue() > now) {
                    active.put(cd.getKey(), cd.getValue());
                }
            }
            if (!active.isEmpty()) {
                cooldowns.put(entry.getKey(), active);
            }
        }
        FrostLogger.info("Loaded cooldowns for " + cooldowns.size() + " player(s).");
    }

    private static void saveCooldownAsync(UUID uuid, String id, long expiry) {
        if (db == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(
            dev.frost.frostcore.Main.getInstance(),
            () -> db.saveCooldown(uuid, id, expiry)
        );
    }

    private static void deleteCooldownAsync(UUID uuid, String id) {
        if (db == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(
            dev.frost.frostcore.Main.getInstance(),
            () -> db.deleteCooldown(uuid, id)
        );
    }

    private static void deleteAllCooldownsAsync(UUID uuid) {
        if (db == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(
            dev.frost.frostcore.Main.getInstance(),
            () -> db.deleteAllCooldowns(uuid)
        );
    }
}

