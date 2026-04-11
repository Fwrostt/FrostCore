package dev.frost.frostcore.manager;

import dev.frost.frostcore.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages vanish state for players.
 * Stored in-memory only — vanish resets on restart.
 */
public class VanishManager {

    private static VanishManager instance;
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();

    public VanishManager() {
        instance = this;
    }

    public static VanishManager getInstance() {
        return instance;
    }

    /**
     * Toggle vanish for a player.
     * @return true if now vanished, false if now visible
     */
    public boolean toggle(Player player) {
        if (vanished.contains(player.getUniqueId())) {
            unvanish(player);
            return false;
        } else {
            vanish(player);
            return true;
        }
    }

    public void vanish(Player player) {
        vanished.add(player.getUniqueId());
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("frostcore.admin.vanish.see")) {
                online.hidePlayer(Main.getInstance(), player);
            }
        }
    }

    public void unvanish(Player player) {
        vanished.remove(player.getUniqueId());
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(Main.getInstance(), player);
        }
    }

    /**
     * Hide all currently vanished players from a newly joining player.
     */
    public void hideVanishedFrom(Player joiner) {
        if (joiner.hasPermission("frostcore.admin.vanish.see")) return;
        for (UUID uuid : vanished) {
            Player vanishedPlayer = Bukkit.getPlayer(uuid);
            if (vanishedPlayer != null && vanishedPlayer.isOnline()) {
                joiner.hidePlayer(Main.getInstance(), vanishedPlayer);
            }
        }
    }

    public boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    public Set<UUID> getVanished() {
        return Collections.unmodifiableSet(vanished);
    }

    /**
     * Clean up a player on quit (remove from vanish set).
     */
    public void cleanup(UUID uuid) {
        vanished.remove(uuid);
    }
}
