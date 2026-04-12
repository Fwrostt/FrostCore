package dev.frost.frostcore.moderation;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.manager.MessageManager;
import dev.frost.frostcore.utils.FrostLogger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for the entire moderation system.
 * Replaces the old PunishmentManager with a full punishment lifecycle.
 */
public class ModerationManager {

    private static ModerationManager instance;
    private final ModerationDatabase modDb;
    private final WebhookManager webhookManager;
    private final ConfigManager config;
    private final MiniMessage mini = MiniMessage.miniMessage();

    // ━━━ Caches ━━━
    // Active ban cache: UUID → Punishment
    private final Map<UUID, Punishment> activeBans = new ConcurrentHashMap<>();
    // Active IP bans: IP → Punishment
    private final Map<String, Punishment> activeIpBans = new ConcurrentHashMap<>();
    // Active mute cache: UUID → Punishment
    private final Map<UUID, Punishment> activeMutes = new ConcurrentHashMap<>();
    // Active IP mutes: IP → Punishment
    private final Map<String, Punishment> activeIpMutes = new ConcurrentHashMap<>();
    // Frozen players
    private final Set<UUID> frozenPlayers = ConcurrentHashMap.newKeySet();
    // Jailed players: UUID → JailedEntry
    private final Map<UUID, ModerationDatabase.JailedEntry> jailedPlayers = new ConcurrentHashMap<>();
    // Jail locations
    private final Map<String, JailLocation> jailLocations = new ConcurrentHashMap<>();
    // Chat locked
    private volatile boolean chatLocked = false;
    // Lockdown
    private volatile boolean lockdown = false;
    private volatile String lockdownReason = null;
    // Staff cooldowns: staffUUID → (action → last used timestamp)
    private final Map<UUID, Map<String, Long>> staffCooldowns = new ConcurrentHashMap<>();
    // Allowed players (IP ban bypass)
    private final Set<UUID> allowedPlayers = ConcurrentHashMap.newKeySet();

    public ModerationManager(ModerationDatabase modDb, WebhookManager webhookManager) {
        this.modDb = modDb;
        this.config = Main.getConfigManager();
        this.webhookManager = webhookManager;
        instance = this;
        load();
    }

    public static ModerationManager getInstance() {
        return instance;
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━ LOADING ━━━━━━━━━━━━━━━━━━━━━━━

    private void load() {
        // Load active bans
        for (Punishment p : modDb.loadActivePunishments(PunishmentType.BAN, PunishmentType.TEMPBAN)) {
            if (p.isExpired()) continue;
            if (p.targetUuid() != null) activeBans.put(p.targetUuid(), p);
        }

        // Load active IP bans
        for (Punishment p : modDb.loadActivePunishments(PunishmentType.IPBAN)) {
            if (p.isExpired()) continue;
            if (p.ip() != null) activeIpBans.put(p.ip(), p);
        }

        // Load active mutes
        for (Punishment p : modDb.loadActivePunishments(PunishmentType.MUTE, PunishmentType.TEMPMUTE)) {
            if (p.isExpired()) continue;
            if (p.targetUuid() != null) activeMutes.put(p.targetUuid(), p);
        }

        // Load active IP mutes
        for (Punishment p : modDb.loadActivePunishments(PunishmentType.IPMUTE)) {
            if (p.isExpired()) continue;
            if (p.ip() != null) activeIpMutes.put(p.ip(), p);
        }

        // Load jail locations
        jailLocations.putAll(modDb.loadJailLocations());

        // Load jailed players
        Map<UUID, ModerationDatabase.JailedEntry> jailed = modDb.loadJailedPlayers();
        long now = System.currentTimeMillis();
        jailed.forEach((uuid, entry) -> {
            if (entry.expiresAt() == -1 || entry.expiresAt() > now) {
                jailedPlayers.put(uuid, entry);
            } else {
                modDb.unjailPlayer(uuid); // Expired
            }
        });

        FrostLogger.info("Moderation loaded: " + activeBans.size() + " bans, " + activeMutes.size()
                + " mutes, " + jailedPlayers.size() + " jailed, " + jailLocations.size() + " jails.");
    }

    // ━━━━━━━━━━━━━━━━ PUNISHMENT EXECUTION ━━━━━━━━━━━━━━━━━

    /**
     * Execute a punishment (ban, mute, warn, kick, etc).
     * This is the main entry point for all punishment actions.
     *
     * @return the created Punishment record, or null on failure
     */
    public Punishment punish(PunishmentType type, UUID targetUuid, String targetName,
                             String ip, String reason, CommandSender staff,
                             long duration, boolean silent) {

        UUID staffUuid = (staff instanceof Player p) ? p.getUniqueId() : null;
        String staffName = staff != null ? staff.getName() : "CONSOLE";

        long now = System.currentTimeMillis();
        long expiresAt = duration == -1 ? -1 : now + duration;
        String randomId = Punishment.generateRandomId();
        String server = config.getString("moderation.server-name", "Survival");

        Punishment p = new Punishment(
                0, type, targetUuid, targetName, ip,
                reason != null ? reason : "No reason specified",
                staffUuid, staffName, duration, now, expiresAt,
                true, null, null, null, null, server, silent, randomId
        );

        int id = modDb.insertPunishment(p);
        if (id == -1) return null;

        // Recreate with actual ID
        p = new Punishment(
                id, type, targetUuid, targetName, ip,
                p.reason(), staffUuid, staffName, duration, now, expiresAt,
                true, null, null, null, null, server, silent, randomId
        );

        // Update caches
        updateCacheAfterPunish(p);

        // Handle online player effects
        handleOnlineEffects(p);

        // Broadcast to staff
        if (config.getBoolean("moderation.broadcast-to-staff", true)) {
            broadcastToStaff(p, silent);
        }

        // Discord webhook
        if (webhookManager != null) {
            webhookManager.sendPunishmentWebhookAsync(p);
        }

        return p;
    }

    /**
     * Remove a punishment (unban, unmute, unwarn).
     */
    public boolean removePunishment(int punishmentId, CommandSender staff, String reason, boolean delete) {
        Punishment p = modDb.getPunishmentById(punishmentId);
        if (p == null) return false;

        UUID staffUuid = (staff instanceof Player pl) ? pl.getUniqueId() : null;
        String staffName = staff != null ? staff.getName() : "CONSOLE";

        if (delete) {
            modDb.deletePunishment(punishmentId);
        } else {
            modDb.deactivatePunishment(punishmentId, staffUuid, staffName, reason);
        }

        // Remove from cache
        removeCacheEntry(p);

        // Webhook
        if (webhookManager != null) {
            webhookManager.sendUnpunishWebhookAsync(p, staffName);
        }

        return true;
    }

    /**
     * Remove an active punishment by player UUID and category.
     */
    public boolean removePunishmentByPlayer(UUID targetUuid, String category,
                                            CommandSender staff, String reason) {
        UUID staffUuid = (staff instanceof Player pl) ? pl.getUniqueId() : null;
        String staffName = staff != null ? staff.getName() : "CONSOLE";

        // Get active punishment first for webhook
        Punishment active = modDb.getActivePunishment(targetUuid, category);

        int count = modDb.deactivateAllActive(targetUuid, category, staffUuid, staffName);
        if (count == 0) return false;

        // Remove from cache
        switch (category.toUpperCase()) {
            case "BAN" -> activeBans.remove(targetUuid);
            case "MUTE" -> activeMutes.remove(targetUuid);
        }

        if (active != null && webhookManager != null) {
            webhookManager.sendUnpunishWebhookAsync(active, staffName);
        }

        return true;
    }

    // ━━━━━━━━━━━━━━━━━━━━●━ CACHE MANAGEMENT ━━━━━━━━━━━━━━━━━

    private void updateCacheAfterPunish(Punishment p) {
        switch (p.type()) {
            case BAN, TEMPBAN -> {
                if (p.targetUuid() != null) activeBans.put(p.targetUuid(), p);
            }
            case IPBAN -> {
                if (p.ip() != null) activeIpBans.put(p.ip(), p);
                if (p.targetUuid() != null) activeBans.put(p.targetUuid(), p);
            }
            case MUTE, TEMPMUTE -> {
                if (p.targetUuid() != null) activeMutes.put(p.targetUuid(), p);
            }
            case IPMUTE -> {
                if (p.ip() != null) activeIpMutes.put(p.ip(), p);
                if (p.targetUuid() != null) activeMutes.put(p.targetUuid(), p);
            }
            default -> {} // Kick and warn don't need caching
        }
    }

    private void removeCacheEntry(Punishment p) {
        switch (p.type().getCategory()) {
            case "BAN" -> {
                if (p.targetUuid() != null) activeBans.remove(p.targetUuid());
                if (p.ip() != null) activeIpBans.remove(p.ip());
            }
            case "MUTE" -> {
                if (p.targetUuid() != null) activeMutes.remove(p.targetUuid());
                if (p.ip() != null) activeIpMutes.remove(p.ip());
            }
        }
    }

    private void handleOnlineEffects(Punishment p) {
        if (p.targetUuid() == null) return;
        Player target = Bukkit.getPlayer(p.targetUuid());
        if (target == null) return;

        switch (p.type()) {
            case BAN, TEMPBAN, IPBAN -> {
                String kickMsg = buildKickScreen("BANNED", p);
                Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                        target.kick(mini.deserialize(kickMsg)));
            }
            case KICK -> {
                String kickMsg = buildKickScreen("KICKED", p);
                Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                        target.kick(mini.deserialize(kickMsg)));
            }
            case MUTE, TEMPMUTE, IPMUTE -> {
                MessageManager mm = Main.getMessageManager();
                mm.sendRaw(target, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#8FA3BF>You have been muted. Reason: <white>" + p.reason());
            }
            case WARN -> {
                MessageManager mm = Main.getMessageManager();
                mm.sendRaw(target, "");
                mm.sendRaw(target, "  <gradient:#C8A87C:#A68B5B><bold>⚠ WARNING</bold></gradient>");
                mm.sendRaw(target, "  <#8FA3BF>Reason: <white>" + p.reason());
                mm.sendRaw(target, "");
            }
            case JAIL -> {
                MessageManager mm = Main.getMessageManager();
                mm.sendRaw(target, "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#8FA3BF>You have been jailed. Reason: <white>" + p.reason());
                // Teleport to jail
                ModerationDatabase.JailedEntry entry = jailedPlayers.get(p.targetUuid());
                if (entry != null) {
                    JailLocation jail = jailLocations.get(entry.jailName());
                    if (jail != null) {
                        var loc = jail.toBukkitLocation();
                        if (loc != null) {
                            Bukkit.getScheduler().runTask(Main.getInstance(), () -> target.teleport(loc));
                        }
                    }
                }
            }
        }
    }

    private String buildKickScreen(String title, Punishment p) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n<gradient:#D4727A:#A35560><bold>").append(title).append("</bold></gradient>\n\n");
        sb.append("<#8FA3BF>Reason: <white>").append(p.reason()).append("\n");
        if (!p.isPermanent()) {
            sb.append("<#8FA3BF>Duration: <white>").append(p.getFormattedDuration()).append("\n");
            sb.append("<#8FA3BF>Expires: <white>").append(p.getFormattedRemaining()).append("\n");
        } else if (p.type() != PunishmentType.KICK) {
            sb.append("<#8FA3BF>Duration: <white>Permanent\n");
        }
        sb.append("\n<dark_gray>ID: ").append(p.randomId());
        return sb.toString();
    }

    private void broadcastToStaff(Punishment p, boolean silent) {
        String prefix = silent ? "<dark_gray>[Silent] " : "";
        String msg = prefix + "<gradient:#D4727A:#A35560>MODERATION</gradient> <dark_gray>» <#8FA3BF>"
                + p.getStaffDisplayName() + " " + p.type().getPastTense() + " <white>"
                + p.getTargetDisplayName()
                + " <dark_gray>| <#8FA3BF>" + p.reason()
                + (p.isPermanent() ? "" : " <dark_gray>(" + p.getFormattedDuration() + ")")
                + " <dark_gray>[#" + p.randomId() + "]";

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("frostcore.moderation.notify")) {
                Main.getMessageManager().sendRaw(online, msg);
            }
        }
        // Also log to console
        FrostLogger.info("[MOD] " + p.getStaffDisplayName() + " " + p.type().getPastTense() + " "
                + p.getTargetDisplayName() + " - " + p.reason() + " [#" + p.randomId() + "]");
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━ QUERIES ━━━━━━━━━━━━━━━━━━━━━━━

    public boolean isBanned(UUID uuid) {
        Punishment p = activeBans.get(uuid);
        if (p == null) return false;
        if (p.isExpired()) {
            activeBans.remove(uuid);
            modDb.deactivatePunishmentAsync(p.id(), null, "SYSTEM", "Expired");
            return false;
        }
        return true;
    }

    public boolean isIpBanned(String ip) {
        Punishment p = activeIpBans.get(ip);
        if (p == null) return false;
        if (p.isExpired()) {
            activeIpBans.remove(ip);
            modDb.deactivatePunishmentAsync(p.id(), null, "SYSTEM", "Expired");
            return false;
        }
        return true;
    }

    public boolean isMuted(UUID uuid) {
        Punishment p = activeMutes.get(uuid);
        if (p == null) return false;
        if (p.isExpired()) {
            activeMutes.remove(uuid);
            modDb.deactivatePunishmentAsync(p.id(), null, "SYSTEM", "Expired");
            return false;
        }
        return true;
    }

    public boolean isIpMuted(String ip) {
        Punishment p = activeIpMutes.get(ip);
        if (p == null) return false;
        if (p.isExpired()) {
            activeIpMutes.remove(ip);
            modDb.deactivatePunishmentAsync(p.id(), null, "SYSTEM", "Expired");
            return false;
        }
        return true;
    }

    public Punishment getActiveBan(UUID uuid)   { return activeBans.get(uuid); }
    public Punishment getActiveMute(UUID uuid)   { return activeMutes.get(uuid); }
    public Punishment getActiveIpBan(String ip)  { return activeIpBans.get(ip); }
    public Punishment getActiveIpMute(String ip) { return activeIpMutes.get(ip); }

    // ━━━━━━━━━━━━━━━━━━━━━ FREEZE ━━━━━━━━━━━━━━━━━━━━━━━

    public boolean isFrozen(UUID uuid)           { return frozenPlayers.contains(uuid); }
    public void setFrozen(UUID uuid, boolean frozen) {
        if (frozen) frozenPlayers.add(uuid);
        else        frozenPlayers.remove(uuid);
    }
    public void toggleFreeze(UUID uuid) { setFrozen(uuid, !isFrozen(uuid)); }

    // ━━━━━━━━━━━━━━━━━━━━━ JAIL ━━━━━━━━━━━━━━━━━━━━━━━

    public boolean isJailed(UUID uuid) {
        ModerationDatabase.JailedEntry entry = jailedPlayers.get(uuid);
        if (entry == null) return false;
        if (entry.expiresAt() != -1 && entry.expiresAt() < System.currentTimeMillis()) {
            jailedPlayers.remove(uuid);
            Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> modDb.unjailPlayer(uuid));
            return false;
        }
        return true;
    }

    public void jailPlayer(UUID uuid, String jailName, long expiresAt, String reason, UUID staffUuid) {
        modDb.jailPlayer(uuid, jailName, expiresAt, reason, staffUuid);
        jailedPlayers.put(uuid, new ModerationDatabase.JailedEntry(jailName, System.currentTimeMillis(), expiresAt, reason, staffUuid));
    }

    public void unjailPlayer(UUID uuid) {
        jailedPlayers.remove(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> modDb.unjailPlayer(uuid));
    }

    public ModerationDatabase.JailedEntry getJailedEntry(UUID uuid) { return jailedPlayers.get(uuid); }

    // ━━━━━━━━━━━━━━━━ JAIL LOCATIONS ━━━━━━━━━━━━━━━━━━

    public JailLocation getJailLocation(String name) { return jailLocations.get(name.toLowerCase()); }
    public Map<String, JailLocation> getJailLocations() { return Collections.unmodifiableMap(jailLocations); }
    public void setJailLocation(JailLocation jail) {
        jailLocations.put(jail.name().toLowerCase(), jail);
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> modDb.saveJailLocation(jail));
    }
    public void deleteJailLocation(String name) {
        jailLocations.remove(name.toLowerCase());
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> modDb.deleteJailLocation(name.toLowerCase()));
    }

    // ━━━━━━━━━━━━━━━━ CHAT LOCK / LOCKDOWN ━━━━━━━━━━━━━━━━━━

    public boolean isChatLocked()   { return chatLocked; }
    public void setChatLocked(boolean locked) { this.chatLocked = locked; }

    public boolean isLockdown()     { return lockdown; }
    public String getLockdownReason() { return lockdownReason; }
    public void setLockdown(boolean active, String reason) {
        this.lockdown = active;
        this.lockdownReason = reason;
    }

    // ━━━━━━━━━━━━━━━━ ALLOWED PLAYERS ━━━━━━━━━━━━━━━━━━

    public boolean isAllowed(UUID uuid) { return allowedPlayers.contains(uuid); }
    public void addAllowed(UUID uuid, UUID addedBy) {
        allowedPlayers.add(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> modDb.addAllowedPlayer(uuid, addedBy));
    }
    public void removeAllowed(UUID uuid) {
        allowedPlayers.remove(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> modDb.removeAllowedPlayer(uuid));
    }

    // ━━━━━━━━━━━━━━━━ STAFF COOLDOWNS ━━━━━━━━━━━━━━━━━━

    public boolean isOnCooldown(UUID staffUuid, String action) {
        Map<String, Long> cooldowns = staffCooldowns.get(staffUuid);
        if (cooldowns == null) return false;
        Long lastUsed = cooldowns.get(action);
        return lastUsed != null && System.currentTimeMillis() - lastUsed < getCooldownMs(action);
    }

    public long getRemainingCooldown(UUID staffUuid, String action) {
        Map<String, Long> cooldowns = staffCooldowns.get(staffUuid);
        if (cooldowns == null) return 0;
        Long lastUsed = cooldowns.get(action);
        if (lastUsed == null) return 0;
        return Math.max(0, getCooldownMs(action) - (System.currentTimeMillis() - lastUsed));
    }

    public void setCooldown(UUID staffUuid, String action) {
        staffCooldowns.computeIfAbsent(staffUuid, k -> new ConcurrentHashMap<>())
                .put(action, System.currentTimeMillis());
    }

    private long getCooldownMs(String action) {
        // Default 5 seconds, can be overridden by group limits
        return 5000L;
    }

    // ━━━━━━━━━━━━━━━━ IP BAN → ALT AUTO-BAN ━━━━━━━━━━━━━━━━━━

    /**
     * When an IP is banned, auto-ban all alt accounts associated with that IP.
     */
    public void autoBanAlts(String ip, Punishment original, CommandSender staff) {
        Set<UUID> alts = modDb.getAlts(original.targetUuid());
        for (UUID altUuid : alts) {
            if (isAllowed(altUuid)) continue; // Skip allowed players
            if (isBanned(altUuid)) continue;  // Already banned

            String altName = modDb.getLastKnownName(altUuid);
            punish(PunishmentType.BAN, altUuid, altName, null,
                    "Alt account of " + original.getTargetDisplayName() + " (IP Ban)",
                    staff, original.duration(), original.silent());
        }
    }

    // ━━━━━━━━━━━━━━━━ DELEGATION ━━━━━━━━━━━━━━━━━━

    public ModerationDatabase getDatabase() { return modDb; }
    public WebhookManager getWebhookManager() { return webhookManager; }
}
