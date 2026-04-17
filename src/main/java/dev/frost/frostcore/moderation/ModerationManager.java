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


public class ModerationManager {

    private static ModerationManager instance;
    private final ModerationDatabase modDb;
    private final WebhookManager webhookManager;
    private final ConfigManager config;
    private final MiniMessage mini = MiniMessage.miniMessage();

    
    
    private final Map<UUID, Punishment> activeBans = new ConcurrentHashMap<>();
    
    private final Map<String, Punishment> activeIpBans = new ConcurrentHashMap<>();
    
    private final Map<UUID, Punishment> activeMutes = new ConcurrentHashMap<>();
    
    private final Map<String, Punishment> activeIpMutes = new ConcurrentHashMap<>();
    
    private final Set<UUID> frozenPlayers = ConcurrentHashMap.newKeySet();
    
    private final Map<UUID, ModerationDatabase.JailedEntry> jailedPlayers = new ConcurrentHashMap<>();
    
    private final Map<String, JailLocation> jailLocations = new ConcurrentHashMap<>();
    
    private volatile boolean chatLocked = false;
    
    private volatile boolean lockdown = false;
    private volatile String lockdownReason = null;
    
    private final Map<UUID, Map<String, Long>> staffCooldowns = new ConcurrentHashMap<>();
    
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

    

    private void load() {
        
        for (Punishment p : modDb.loadActivePunishments(PunishmentType.BAN, PunishmentType.TEMPBAN)) {
            if (p.isExpired()) continue;
            if (p.targetUuid() != null) activeBans.put(p.targetUuid(), p);
        }

        
        for (Punishment p : modDb.loadActivePunishments(PunishmentType.IPBAN)) {
            if (p.isExpired()) continue;
            if (p.ip() != null) activeIpBans.put(p.ip(), p);
        }

        
        for (Punishment p : modDb.loadActivePunishments(PunishmentType.MUTE, PunishmentType.TEMPMUTE)) {
            if (p.isExpired()) continue;
            if (p.targetUuid() != null) activeMutes.put(p.targetUuid(), p);
        }

        
        for (Punishment p : modDb.loadActivePunishments(PunishmentType.IPMUTE)) {
            if (p.isExpired()) continue;
            if (p.ip() != null) activeIpMutes.put(p.ip(), p);
        }

        
        jailLocations.putAll(modDb.loadJailLocations());

        
        Map<UUID, ModerationDatabase.JailedEntry> jailed = modDb.loadJailedPlayers();
        long now = System.currentTimeMillis();
        jailed.forEach((uuid, entry) -> {
            if (entry.expiresAt() == -1 || entry.expiresAt() > now) {
                jailedPlayers.put(uuid, entry);
            } else {
                modDb.unjailPlayer(uuid); 
            }
        });

        FrostLogger.info("Moderation loaded: " + activeBans.size() + " bans, " + activeMutes.size()
                + " mutes, " + jailedPlayers.size() + " jailed, " + jailLocations.size() + " jails.");
    }

    

    
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

        
        p = new Punishment(
                id, type, targetUuid, targetName, ip,
                p.reason(), staffUuid, staffName, duration, now, expiresAt,
                true, null, null, null, null, server, silent, randomId
        );

        
        updateCacheAfterPunish(p);

        
        handleOnlineEffects(p);

        
        if (config.getBoolean("moderation.broadcast-to-staff", true)) {
            broadcastToStaff(p, silent);
        }

        
        if (webhookManager != null) {
            Player target = p.targetUuid() != null ? Bukkit.getPlayer(p.targetUuid()) : null;
            String playerStatus = (target != null && target.isOnline()) ? target.getGameMode().name() : "Offline";
            webhookManager.sendPunishmentWebhookAsync(p, playerStatus);
        }

        return p;
    }

    
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

        // Clear caches (handles UUID map + IP map)
        removeCacheEntry(p);

        // Notify the target if online and this was a mute
        if (p.type().getCategory().equals("MUTE") && p.targetUuid() != null) {
            Player target = Bukkit.getPlayer(p.targetUuid());
            if (target != null && target.isOnline()) {
                Main.getMessageManager().sendRaw(target,
                        "<#D4727A>MOD <dark_gray>»</dark_gray> <#7ECFA0>You have been unmuted.");
            }
        }

        if (webhookManager != null) {
            webhookManager.sendUnpunishWebhookAsync(p, staffName);
        }

        return true;
    }

    
    public boolean removePunishmentByPlayer(UUID targetUuid, String category,
                                            CommandSender staff, String reason) {
        UUID staffUuid = (staff instanceof Player pl) ? pl.getUniqueId() : null;
        String staffName = staff != null ? staff.getName() : "CONSOLE";

        // Grab the active record before deactivating (for webhook + display name)
        Punishment active = modDb.getActivePunishment(targetUuid, category);

        // If no UUID-linked punishment, check whether there's an IP-mute on the player's current IP
        // (covers the case where player was /muteip'd but /unmute is used by name)
        if (active == null && category.equalsIgnoreCase("MUTE")) {
            Player onlineTarget = Bukkit.getPlayer(targetUuid);
            if (onlineTarget != null && onlineTarget.getAddress() != null) {
                String ip = onlineTarget.getAddress().getAddress().getHostAddress();
                active = modDb.getActiveIpPunishment(ip, "MUTE");
            }
        }

        int count = modDb.deactivateAllActive(targetUuid, category, staffUuid, staffName);

        // Also deactivate any IP-mute linked to the player's current IP
        boolean ipMuteCleared = false;
        if (category.equalsIgnoreCase("MUTE")) {
            Player onlineTarget = Bukkit.getPlayer(targetUuid);
            if (onlineTarget != null && onlineTarget.getAddress() != null) {
                String ip = onlineTarget.getAddress().getAddress().getHostAddress();
                Punishment ipMute = activeIpMutes.get(ip);
                if (ipMute != null) {
                    modDb.deactivatePunishment(ipMute.id(), staffUuid, staffName, reason);
                    activeIpMutes.remove(ip);
                    ipMuteCleared = true;
                    if (active == null) active = ipMute;
                }
            }
        }

        if (count == 0 && !ipMuteCleared) return false;

        // Clear UUID-level caches
        switch (category.toUpperCase()) {
            case "BAN" -> activeBans.remove(targetUuid);
            case "MUTE" -> activeMutes.remove(targetUuid);
        }

        // Notify the target if online and this was a mute removal
        if (category.equalsIgnoreCase("MUTE")) {
            Player onlineTarget = Bukkit.getPlayer(targetUuid);
            if (onlineTarget != null && onlineTarget.isOnline()) {
                Main.getMessageManager().sendRaw(onlineTarget,
                        "<#D4727A>MOD <dark_gray>»</dark_gray> <#7ECFA0>You have been unmuted.");
            }
        }

        if (active != null && webhookManager != null) {
            webhookManager.sendUnpunishWebhookAsync(active, staffName);
        }

        return true;
    }

    

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
            default -> {} 
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
                mm.sendRaw(target, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>You have been muted. Reason: <white>" + p.reason());
            }
            case WARN -> {
                MessageManager mm = Main.getMessageManager();
                mm.sendRaw(target, "");
                mm.sendRaw(target, "  <gradient:#C8A87C:#A68B5B><bold>⚠ WARNING</bold>");
                mm.sendRaw(target, "  <#8FA3BF>Reason: <white>" + p.reason());
                mm.sendRaw(target, "");
            }
            case JAIL -> {
                MessageManager mm = Main.getMessageManager();
                mm.sendRaw(target, "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>You have been jailed. Reason: <white>" + p.reason());
                
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
        sb.append("\n<#D4727A><bold>").append(title).append("</bold>\n\n");
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
        String msg = prefix + "<#D4727A>MOD <dark_gray>»</dark_gray> <#8FA3BF>"
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
        
        FrostLogger.info("[MOD] " + p.getStaffDisplayName() + " " + p.type().getPastTense() + " "
                + p.getTargetDisplayName() + " - " + p.reason() + " [#" + p.randomId() + "]");
    }

    

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

    

    public boolean isFrozen(UUID uuid)           { return frozenPlayers.contains(uuid); }
    public void setFrozen(UUID uuid, boolean frozen) {
        if (frozen) frozenPlayers.add(uuid);
        else        frozenPlayers.remove(uuid);
    }
    public void toggleFreeze(UUID uuid) { setFrozen(uuid, !isFrozen(uuid)); }

    

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

    

    public boolean isChatLocked()   { return chatLocked; }
    public void setChatLocked(boolean locked) { this.chatLocked = locked; }

    public boolean isLockdown()     { return lockdown; }
    public String getLockdownReason() { return lockdownReason; }
    public void setLockdown(boolean active, String reason) {
        this.lockdown = active;
        this.lockdownReason = reason;
    }

    

    public boolean isAllowed(UUID uuid) { return allowedPlayers.contains(uuid); }
    public void addAllowed(UUID uuid, UUID addedBy) {
        allowedPlayers.add(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> modDb.addAllowedPlayer(uuid, addedBy));
    }
    public void removeAllowed(UUID uuid) {
        allowedPlayers.remove(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> modDb.removeAllowedPlayer(uuid));
    }

    

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
        
        return 5000L;
    }

    

    
    public void autoBanAlts(String ip, Punishment original, CommandSender staff) {
        Set<UUID> alts = modDb.getAlts(original.targetUuid());
        for (UUID altUuid : alts) {
            if (isAllowed(altUuid)) continue; 
            if (isBanned(altUuid)) continue;  

            String altName = modDb.getLastKnownName(altUuid);
            punish(PunishmentType.BAN, altUuid, altName, null,
                    "Alt account of " + original.getTargetDisplayName() + " (IP Ban)",
                    staff, original.duration(), original.silent());
        }
    }

    

    public ModerationDatabase getDatabase() { return modDb; }
    public WebhookManager getWebhookManager() { return webhookManager; }
}
