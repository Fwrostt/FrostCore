package dev.frost.frostcore.manager;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.database.DatabaseManager;
import dev.frost.frostcore.utils.FrostLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UtilityManager {

    private static UtilityManager instance;
    private final DatabaseManager db;
    private final Map<UUID, String> nicknames = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> godMode = new ConcurrentHashMap<>();
    private final MiniMessage mm = MiniMessage.miniMessage();

    public UtilityManager(DatabaseManager db) {
        this.db = db;
        loadAll();
        instance = this;
    }

    public static UtilityManager getInstance() {
        return instance;
    }

    private void loadAll() {
        Map<UUID, DatabaseManager.UtilityData> data = db.loadPlayerUtilities();
        data.forEach((uuid, utilData) -> {
            if (utilData.nickname() != null) nicknames.put(uuid, utilData.nickname());
            if (utilData.godMode()) godMode.put(uuid, true);
        });
        FrostLogger.info("Loaded utility data for " + data.size() + " players.");
    }

    public boolean isGodMode(UUID uuid) {
        return godMode.getOrDefault(uuid, false);
    }

    public void setGodMode(UUID uuid, boolean enabled) {
        if (enabled) {
            godMode.put(uuid, true);
        } else {
            godMode.remove(uuid);
        }
        db.savePlayerUtilityAsync(uuid, nicknames.get(uuid), enabled);
    }

    public void toggleGodMode(UUID uuid) {
        setGodMode(uuid, !isGodMode(uuid));
    }

    public String getNickname(UUID uuid) {
        return nicknames.get(uuid);
    }

    public void setNickname(UUID uuid, String nick) {
        nicknames.put(uuid, nick);
        db.savePlayerUtilityAsync(uuid, nick, isGodMode(uuid));
        
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            applyNick(player);
        }
    }

    public void removeNickname(UUID uuid) {
        nicknames.remove(uuid);
        db.savePlayerUtilityAsync(uuid, null, isGodMode(uuid));
        
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.displayName(Component.text(player.getName()));
            player.playerListName(Component.text(player.getName()));
        }
    }

    public void applyNick(Player player) {
        String nick = nicknames.get(player.getUniqueId());
        if (nick == null) return;

        Component nickComp = mm.deserialize(nick);
        player.displayName(nickComp);
        player.playerListName(nickComp);
    }
}
