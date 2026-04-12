package dev.frost.frostcore.listeners;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.manager.WarpManager;
import dev.frost.frostcore.utils.TeleportUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Handles automatic spawn teleportation:
 * <ul>
 *   <li><b>teleport-on-join</b>  — teleports players to spawn on their first join.</li>
 *   <li><b>teleport-on-respawn</b> — teleports players to spawn after death/respawn.</li>
 * </ul>
 * Both behaviours are independently togglable in {@code config.yml} under {@code spawn.*}.
 */
public class SpawnListener implements Listener {

    private final ConfigManager config;
    private final WarpManager warpManager;
    private final TeleportUtil teleportUtil;

    public SpawnListener(ConfigManager config, WarpManager warpManager, TeleportUtil teleportUtil) {
        this.config = config;
        this.warpManager = warpManager;
        this.teleportUtil = teleportUtil;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!config.getBoolean("spawn.teleport-on-join", false)) return;
        if (!config.getBoolean("spawn.enabled", true)) return;

        Player player = event.getPlayer();

        // Only teleport on first join — not every login
        if (player.hasPlayedBefore()) return;

        Location spawn = warpManager.getSpawn();
        if (spawn == null) return;

        org.bukkit.Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (player.isOnline()) {
                teleportUtil.teleportInstant(player, spawn);
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!config.getBoolean("spawn.teleport-on-respawn", true)) return;
        if (!config.getBoolean("spawn.enabled", true)) return;

        Location spawn = warpManager.getSpawn();
        if (spawn == null) return;

        event.setRespawnLocation(spawn);
    }
}

