package dev.frost.frostcore.listeners;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.TeamManager;
import dev.frost.frostcore.teams.Team;
import dev.frost.frostcore.utils.FrostLogger;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Prevents teammates and allies from damaging each other when PvP is disabled.
 * <p>
 * Handles:
 * - Direct melee hits
 * - Projectiles (arrows, tridents, etc.)
 */
public class TeamPvPListener implements Listener {

    private final TeamManager teamManager = TeamManager.getInstance();

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Only care about player-on-player damage
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) return;

        // Don't block self-damage
        if (attacker.getUniqueId().equals(victim.getUniqueId())) return;

        // Both players must be in a team
        if (!teamManager.hasTeam(attacker.getUniqueId())) return;
        if (!teamManager.hasTeam(victim.getUniqueId())) return;

        try {
            Team attackerTeam = teamManager.getTeam(attacker.getUniqueId());
            Team victimTeam = teamManager.getTeam(victim.getUniqueId());

            // Same team — check PvP toggle
            if (attackerTeam.getName().equalsIgnoreCase(victimTeam.getName())) {
                if (!attackerTeam.isPvpToggle()) {
                    event.setCancelled(true);
                    return;
                }
            }

            // Allied teams — check friendly-fire config
            if (!Main.getConfigManager().getBoolean("teams.friendly-fire", false)) {
                if (attackerTeam.isAlly(victimTeam.getName())) {
                    event.setCancelled(true);
                }
            }

        } catch (Exception e) {
            // Fail open — if team lookup fails, allow the hit. Log for debugging.
            FrostLogger.warn("TeamPvPListener error: " + e.getMessage());
        }
    }

    /**
     * Resolve the actual attacking player, including projectile sources.
     */
    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter) {
                return shooter;
            }
        }
        return null;
    }
}
