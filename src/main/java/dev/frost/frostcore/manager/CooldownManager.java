package dev.frost.frostcore.manager;

import dev.frost.frostcore.database.DatabaseManager;
import dev.frost.frostcore.utils.FrostLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class CooldownManager {

    private static final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    private static DatabaseManager db;

    private CooldownManager() {}

    
    public static void init(DatabaseManager database) {
        db = database;
        loadCooldowns();
    }

    
    public static void setCooldown(Player player, String id, int durationSeconds) {
        long expiry = System.currentTimeMillis() + durationSeconds * 1000L;
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                 .put(id, expiry);
        saveCooldownAsync(player.getUniqueId(), id, expiry);
    }

    
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

    
    public static void clearCooldown(Player player, String id) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns != null) {
            playerCooldowns.remove(id);
            if (playerCooldowns.isEmpty()) cooldowns.remove(player.getUniqueId());
            deleteCooldownAsync(player.getUniqueId(), id);
        }
    }

    
    public static void clearAllCooldowns(Player player) {
        cooldowns.remove(player.getUniqueId());
        deleteAllCooldownsAsync(player.getUniqueId());
    }

    
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

