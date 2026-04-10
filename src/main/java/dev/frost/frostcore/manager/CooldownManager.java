package dev.frost.frostcore.manager;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    // player UUID -> (cooldown ID -> expire time in milliseconds)
    private static Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public CooldownManager() {
//        loadCooldowns();
    }
    /**
     * Sets a cooldown for a player.
     * @param player The player.
     * @param id Cooldown ID.
     * @param durationSeconds Cooldown duration in seconds.
     */
    public static void setCooldown(Player player, String id, int durationSeconds) {
        cooldowns.computeIfAbsent(player.getUniqueId(), uuid -> new HashMap<>()).put(id, System.currentTimeMillis() + durationSeconds * 1000L);
        saveCooldowns();
    }

    /**
     * Checks if a player is currently on cooldown for a specific ID.
     * @param player The player.
     * @param id Cooldown ID.
     * @return True if on cooldown, false otherwise.
     */
    public static boolean isOnCooldown(Player player, String id) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return false;

        Long expireTime = playerCooldowns.get(id);
        if (expireTime == null) return false;

        if (System.currentTimeMillis() >= expireTime) {
            playerCooldowns.remove(id); // Cooldown expired, clean it up
            if (playerCooldowns.isEmpty()) cooldowns.remove(player.getUniqueId()); // Remove player entry if no cooldowns left
            return false;
        }
        return true;
    }

    /**
     * Gets the remaining cooldown time in seconds.
     * @param player The player.
     * @param id Cooldown ID.
     * @return Remaining time in seconds (0 if no cooldown).
     */
    public static int getRemainingTime(Player player, String id) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return 0;

        Long expireTime = playerCooldowns.get(id);
        if (expireTime == null) return 0;

        long remainingMillis = expireTime - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            playerCooldowns.remove(id);
            if (playerCooldowns.isEmpty()) cooldowns.remove(player.getUniqueId());
            return 0;
        }
        return (int) (remainingMillis / 1000);
    }

    /**
     * Clears a specific cooldown for a player.
     * @param player The player.
     * @param id Cooldown ID.
     */
    public static void clearCooldown(Player player, String id) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns != null) {
            playerCooldowns.remove(id);
            if (playerCooldowns.isEmpty()) cooldowns.remove(player.getUniqueId());
            saveCooldowns();
        }
    }

    /**
     * Clears all cooldowns for a player.
     * @param player The player.
     */
    public static void clearAllCooldowns(Player player) {
        cooldowns.remove(player.getUniqueId());
        saveCooldowns();
    }

    public static void saveCooldowns() {
//        binManager.setData("cooldowns", cooldowns);
    }

    public static void loadCooldowns() {
//        Map<UUID, Map<String, Long>> data = (Map<UUID, Map<String, Long>>) binManager.getData("cooldowns");
//        cooldowns = (data != null) ? data : new HashMap<>();
    }
}
