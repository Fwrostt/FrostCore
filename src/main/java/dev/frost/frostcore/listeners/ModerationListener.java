package dev.frost.frostcore.listeners;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.moderation.ModerationManager;
import dev.frost.frostcore.moderation.Punishment;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
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

import java.util.List;
import java.util.UUID;

/**
 * Enforces all moderation restrictions: bans, IP bans, mutes, freezes,
 * jails, chat lock, lockdown, and command blocking.
 */
public class ModerationListener implements Listener {

    private final MiniMessage mini = MiniMessage.miniMessage();

    private ModerationManager mod() {
        return ModerationManager.getInstance();
    }

    private MessageManager mm() {
        return Main.getMessageManager();
    }

    // ━━━━━━━━━━━━━━━━━━ PRE-LOGIN: Ban + IP Ban + Lockdown ━━━━━━━━━━━━━━━━━━

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        ModerationManager mod = mod();
        if (mod == null) return;

        UUID uuid = event.getUniqueId();
        String ip = event.getAddress().getHostAddress();

        if (mod.isLockdown()) {
            if (!mod.isAllowed(uuid)) {
                String reason = mod.getLockdownReason() != null ? mod.getLockdownReason() : "Maintenance";
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        mini.deserialize("\n<#D4727A><bold>SERVER LOCKED</bold>\n\n<#8FA3BF>Reason: <white>" + reason + "\n"));
                return;
            }
        }

        // UUID ban check
        if (mod.isBanned(uuid)) {
            Punishment ban = mod.getActiveBan(uuid);
            if (ban != null && ban.isInEffect()) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                        mini.deserialize(buildBanScreen(ban)));
                return;
            }
        }

        // IP ban check (skip if player is on allowed list)
        if (!mod.isAllowed(uuid) && mod.isIpBanned(ip)) {
            Punishment ipBan = mod.getActiveIpBan(ip);
            if (ipBan != null && ipBan.isInEffect()) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                        mini.deserialize(buildBanScreen(ipBan)));
            }
        }
    }

    private String buildBanScreen(Punishment p) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n<#D4727A><bold>BANNED</bold>\n\n");
        sb.append("<#8FA3BF>Reason: <white>").append(p.reason()).append("\n");
        if (!p.isPermanent()) {
            sb.append("<#8FA3BF>Duration: <white>").append(p.getFormattedDuration()).append("\n");
            sb.append("<#8FA3BF>Expires in: <white>").append(p.getFormattedRemaining()).append("\n");
        } else {
            sb.append("<#8FA3BF>Duration: <white>Permanent\n");
        }
        sb.append("\n<dark_gray>ID: ").append(p.randomId());
        return sb.toString();
    }

    // ━━━━━━━━━━━━━━━━━━ CHAT: Mute + IP Mute + Chat Lock ━━━━━━━━━━━━━━━━━━

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        ModerationManager mod = mod();
        if (mod == null) return;

        // Mute check
        if (mod.isMuted(player.getUniqueId())) {
            Punishment mute = mod.getActiveMute(player.getUniqueId());
            if (mute != null) {
                String remaining = mute.isPermanent() ? "Permanent" : mute.getFormattedRemaining();
                mm().sendRaw(player, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>You are muted. <#8FA3BF>Remaining: <white>" + remaining);
            } else {
                mm().send(player, "moderation.muted-message");
            }
            event.setCancelled(true);
            return;
        }

        // IP mute check
        String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : null;
        if (ip != null && mod.isIpMuted(ip)) {
            mm().sendRaw(player, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>You are IP muted.");
            event.setCancelled(true);
            return;
        }

        // Chat lock
        if (mod.isChatLocked() && !player.hasPermission("frostcore.moderation.bypass.lockchat")) {
            mm().send(player, "moderation.chat-locked-message");
            event.setCancelled(true);
        }
    }

    // ━━━━━━━━━━━━━━━━━━ MOVEMENT: Freeze + Jail ━━━━━━━━━━━━━━━━━━━

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        Player player = event.getPlayer();
        ModerationManager mod = mod();
        if (mod == null) return;

        if (mod.isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
            mm().sendActionBar(player, "moderation.frozen-alert");
            return;
        }

        if (mod.isJailed(player.getUniqueId())) {
            // Allow movement within jail radius (3 blocks)
            var entry = mod.getJailedEntry(player.getUniqueId());
            if (entry != null) {
                var jail = mod.getJailLocation(entry.jailName());
                if (jail != null) {
                    var jailLoc = jail.toBukkitLocation();
                    if (jailLoc != null && event.getTo().distance(jailLoc) > 5) {
                        event.setCancelled(true);
                        mm().sendRaw(player, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>You are jailed. You cannot leave this area.");
                    }
                }
            }
        }
    }

    // ━━━━━━━━━━━━━━━━━━ BLOCKS: Freeze + Jail ━━━━━━━━━━━━━━━━━━━

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        ModerationManager mod = mod();
        if (mod == null) return;
        UUID uuid = event.getPlayer().getUniqueId();
        if (mod.isFrozen(uuid) || mod.isJailed(uuid)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ModerationManager mod = mod();
        if (mod == null) return;
        UUID uuid = event.getPlayer().getUniqueId();
        if (mod.isFrozen(uuid) || mod.isJailed(uuid)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        ModerationManager mod = mod();
        if (mod == null) return;
        if (mod.isFrozen(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectile(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player player) {
            ModerationManager mod = mod();
            if (mod == null) return;
            if (mod.isFrozen(player.getUniqueId())) event.setCancelled(true);
        }
    }

    // ━━━━━━━━━━━━━━━━━━ COMMANDS: Freeze + Mute + Jail ━━━━━━━━━━━━━━━━━━

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        ModerationManager mod = mod();
        if (mod == null) return;
        String cmd = event.getMessage().split(" ")[0].toLowerCase();

        // Frozen players: deny all commands except messaging
        if (mod.isFrozen(player.getUniqueId())) {
            if (cmd.equals("/msg") || cmd.equals("/tell") || cmd.equals("/w") || cmd.equals("/reply") || cmd.equals("/r")) {
                return;
            }
            mm().send(player, "moderation.frozen-command-denied");
            event.setCancelled(true);
            return;
        }

        // Jailed players: allow chat commands only
        if (mod.isJailed(player.getUniqueId())) {
            if (cmd.equals("/msg") || cmd.equals("/tell") || cmd.equals("/w") || cmd.equals("/reply") || cmd.equals("/r")
                    || cmd.equals("/team") || cmd.equals("/t")) {
                return;
            }
            mm().sendRaw(player, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>You cannot use commands while jailed.");
            event.setCancelled(true);
            return;
        }

        // Muted players: block configured commands
        if (mod.isMuted(player.getUniqueId())) {
            List<String> blockedCmds = Main.getConfigManager().getStringList("moderation.muted-blocked-commands");
            for (String blocked : blockedCmds) {
                if (cmd.equalsIgnoreCase(blocked)) {
                    mm().sendRaw(player, "<#D4727A>MOD <dark_gray>»</dark_gray> <#D4727A>You cannot use this command while muted.");
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}
