package dev.frost.frostcore.manager;

import dev.frost.frostcore.database.DatabaseManager;
import dev.frost.frostcore.utils.FrostLogger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PunishmentManager {

    private static PunishmentManager instance;
    private final DatabaseManager db;
    
    private final Map<UUID, Long> mutedPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> frozenPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, BanData> bannedPlayers = new ConcurrentHashMap<>();
    private boolean chatLocked = false;

    public PunishmentManager(DatabaseManager db) {
        this.db = db;
        loadAll();
        instance = this;
    }

    public static PunishmentManager getInstance() {
        return instance;
    }

    private void loadAll() {
        Map<UUID, DatabaseManager.PunishmentData> data = db.loadPlayerPunishments();
        data.forEach((uuid, punData) -> {
            if (punData.muteExpires() > -2) { 
                long now = System.currentTimeMillis();
                if (punData.muteExpires() == -1 || punData.muteExpires() > now) {
                    mutedPlayers.put(uuid, punData.muteExpires());
                }
            }
            if (punData.isFrozen()) frozenPlayers.put(uuid, true);
        });

        
        Map<UUID, DatabaseManager.BanEntry> bans = db.loadPlayerBans();
        bans.forEach((uuid, banEntry) -> {
            long now = System.currentTimeMillis();
            if (banEntry.expires() == -1 || banEntry.expires() > now) {
                bannedPlayers.put(uuid, new BanData(banEntry.expires(), banEntry.reason()));
            }
        });

        FrostLogger.info("Loaded punishment data for " + data.size() + " players, " + bannedPlayers.size() + " bans.");
    }

    public boolean isMuted(UUID uuid) {
        Long expires = mutedPlayers.get(uuid);
        if (expires == null) return false;
        
        if (expires != -1 && expires < System.currentTimeMillis()) {
            mutedPlayers.remove(uuid);
            savePunishmentAsync(uuid);
            return false;
        }
        return true;
    }

    public void mute(UUID uuid, long durationMs) {
        long expireAt = durationMs == -1 ? -1 : System.currentTimeMillis() + durationMs;
        mutedPlayers.put(uuid, expireAt);
        savePunishmentAsync(uuid);
    }

    public void unmute(UUID uuid) {
        mutedPlayers.remove(uuid);
        savePunishmentAsync(uuid);
    }

    public long getMuteExpiration(UUID uuid) {
        return mutedPlayers.getOrDefault(uuid, -2L);
    }

    public boolean isFrozen(UUID uuid) {
        return frozenPlayers.getOrDefault(uuid, false);
    }

    public void setFrozen(UUID uuid, boolean frozen) {
        if (frozen) {
            frozenPlayers.put(uuid, true);
        } else {
            frozenPlayers.remove(uuid);
        }
        savePunishmentAsync(uuid);
    }

    public void toggleFreeze(UUID uuid) {
        setFrozen(uuid, !isFrozen(uuid));
    }

    public boolean isChatLocked() {
        return chatLocked;
    }

    public void setChatLocked(boolean locked) {
        this.chatLocked = locked;
    }

    

    public record BanData(long expires, String reason) {}

    public void ban(UUID uuid, long expireAt, String reason) {
        bannedPlayers.put(uuid, new BanData(expireAt, reason));
        saveBanAsync(uuid);
    }

    public void unban(UUID uuid) {
        bannedPlayers.remove(uuid);
        db.deletePlayerBanAsync(uuid);
    }

    public boolean isBanned(UUID uuid) {
        BanData data = bannedPlayers.get(uuid);
        if (data == null) return false;
        if (data.expires() != -1 && data.expires() < System.currentTimeMillis()) {
            bannedPlayers.remove(uuid);
            db.deletePlayerBanAsync(uuid);
            return false;
        }
        return true;
    }

    public BanData getBanData(UUID uuid) {
        return bannedPlayers.get(uuid);
    }

    private void saveBanAsync(UUID uuid) {
        BanData data = bannedPlayers.get(uuid);
        if (data != null) {
            db.savePlayerBanAsync(uuid, data.expires(), data.reason());
        }
    }

    private void savePunishmentAsync(UUID uuid) {
        long mute = mutedPlayers.getOrDefault(uuid, -2L);
        boolean frozen = isFrozen(uuid);
        db.savePlayerPunishmentAsync(uuid, mute, frozen);
    }

    
    public long parseTime(String input) {
        if (input == null || input.isEmpty()) return -1;
        
        long totalMs = 0;
        Pattern pattern = Pattern.compile("(\\d+)([smhd])");
        Matcher matcher = pattern.matcher(input.toLowerCase());
        
        boolean found = false;
        while (matcher.find()) {
            found = true;
            long val = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            
            switch (unit) {
                case "s" -> totalMs += val * 1000L;
                case "m" -> totalMs += val * 60000L;
                case "h" -> totalMs += val * 3600000L;
                case "d" -> totalMs += val * 86400000L;
            }
        }
        
        
        return found ? totalMs : -2;
    }
}
