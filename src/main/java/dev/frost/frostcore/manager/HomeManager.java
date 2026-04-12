package dev.frost.frostcore.manager;

import dev.frost.frostcore.Main;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player personal homes.
 * Loads a player's homes into cache on join, and removes them on quit.
 */
public class HomeManager implements Listener {

    private final Main plugin;
    private final ConfigManager config;

    private final Map<UUID, Map<String, Location>> cache = new ConcurrentHashMap<>();

    public HomeManager(Main plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Set or update a home for a player.
     * Overwrites if it exists. Checks limits if creating a new one.
     *
     * @return true if successful, false if they hit max homes
     */
    public boolean setHome(Player player, String name, Location location) {
        UUID uuid = player.getUniqueId();
        String lowerName = name.toLowerCase();

        Map<String, Location> playerHomes = cache.computeIfAbsent(uuid, k -> new LinkedHashMap<>());

        if (!playerHomes.containsKey(lowerName)) {
            if (playerHomes.size() >= getMaxHomes(player)) {
                return false;
            }
        }

        playerHomes.put(lowerName, location);
        Main.getDatabaseManager().savePlayerHomeAsync(uuid, lowerName, location);
        return true;
    }

    public void deleteHome(Player player, String name) {
        UUID uuid = player.getUniqueId();
        String lowerName = name.toLowerCase();
        Map<String, Location> playerHomes = cache.get(uuid);
        if (playerHomes != null) {
            playerHomes.remove(lowerName);
        }
        Main.getDatabaseManager().deletePlayerHomeAsync(uuid, lowerName);
    }

    public boolean renameHome(Player player, String oldName, String newName) {
        UUID uuid = player.getUniqueId();
        String oldLower = oldName.toLowerCase();
        String newLower = newName.toLowerCase();

        Map<String, Location> playerHomes = cache.get(uuid);
        if (playerHomes == null || !playerHomes.containsKey(oldLower)) {
            return false;
        }
        if (playerHomes.containsKey(newLower)) {
            return false;
        }

        Location loc = playerHomes.remove(oldLower);
        playerHomes.put(newLower, loc);

        Main.getDatabaseManager().deletePlayerHomeAsync(uuid, oldLower);
        Main.getDatabaseManager().savePlayerHomeAsync(uuid, newLower, loc);
        return true;
    }

    public Location getHome(Player player, String name) {
        Map<String, Location> playerHomes = cache.get(player.getUniqueId());
        if (playerHomes == null) return null;
        return playerHomes.get(name.toLowerCase());
    }

    public Map<String, Location> getHomes(Player player) {
        return cache.getOrDefault(player.getUniqueId(), new LinkedHashMap<>());
    }

    public int getMaxHomes(Player player) {
        int max = config.getInt("homes.max-homes", 5);

        for (org.bukkit.permissions.PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
            String p = perm.getPermission();
            if (p.startsWith("frostcore.homes.limit.")) {
                try {
                    int limit = Integer.parseInt(p.substring("frostcore.homes.limit.".length()));
                    if (limit > max) {
                        max = limit;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return max;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, Location> homes = Main.getDatabaseManager().loadPlayerHomes(uuid);

            // Use ConcurrentHashMap to prevent race conditions between
            // async load completion and main-thread commands like /sethome
            cache.put(uuid, new java.util.concurrent.ConcurrentHashMap<>(homes));
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cache.remove(event.getPlayer().getUniqueId());
    }
}

