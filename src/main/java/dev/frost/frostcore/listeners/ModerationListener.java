package dev.frost.frostcore.listeners;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.manager.PunishmentManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;

public class ModerationListener implements Listener {

    private final PunishmentManager pm = PunishmentManager.getInstance();
    private final MessageManager mm = Main.getMessageManager();
    private final net.kyori.adventure.text.minimessage.MiniMessage mini = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (pm.isBanned(event.getUniqueId())) {
            PunishmentManager.BanData data = pm.getBanData(event.getUniqueId());
            String reason = data != null && data.reason() != null ? data.reason() : "No reason";
            String expires;
            if (data != null && data.expires() != -1) {
                long remaining = data.expires() - System.currentTimeMillis();
                expires = formatDuration(remaining);
            } else {
                expires = "Permanent";
            }
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    mini.deserialize("\n<gradient:#FF5555:#FF55FF><bold>BANNED</bold></gradient>\n\n<#B0C4FF>Reason: <white>" + reason + "\n<#B0C4FF>Duration: <white>" + expires + "\n")
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        
        if (pm.isMuted(player.getUniqueId())) {
            mm.send(player, "moderation.muted-message");
            event.setCancelled(true);
            return;
        }

        if (pm.isChatLocked() && !player.hasPermission("frostcore.moderation.bypass.lockchat")) {
            mm.send(player, "moderation.chat-locked-message");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        
        if (pm.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            mm.sendActionBar(event.getPlayer(), "moderation.frozen-alert");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (pm.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (pm.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (pm.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectile(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player player) {
            if (pm.isFrozen(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (pm.isFrozen(player.getUniqueId())) {
            String cmd = event.getMessage().split(" ")[0].toLowerCase();
            if (cmd.equals("/msg") || cmd.equals("/tell") || cmd.equals("/w") || cmd.equals("/reply") || cmd.equals("/r")) {
                return;
            }
            mm.send(player, "moderation.frozen-command-denied");
            event.setCancelled(true);
        }
    }

    private String formatDuration(long millis) {
        long totalSeconds = millis / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        if (days > 0) return days + "d " + hours + "h " + minutes + "m";
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }
}
