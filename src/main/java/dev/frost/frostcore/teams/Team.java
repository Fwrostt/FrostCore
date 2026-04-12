package dev.frost.frostcore.teams;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class Team {

    @Setter private String name;
    @Setter private String tag;
    @Setter private String color;
    private final Set<UUID> owners = ConcurrentHashMap.newKeySet();
    private final Set<UUID> admins = ConcurrentHashMap.newKeySet();
    private final Set<UUID> members = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Boolean> chatEnabled = new ConcurrentHashMap<>();
    @Setter private boolean pvpToggle;
    @Setter private Location home;
    private final Map<String, Location> warps = new ConcurrentHashMap<>();
    private final Set<String> allies = ConcurrentHashMap.newKeySet();
    private final Set<String> enemies = ConcurrentHashMap.newKeySet();

    public Team(String name, String tag, UUID owner, boolean defaultPvp) {
        this.name = name;
        this.tag = tag;
        this.owners.add(owner);
        this.pvpToggle = defaultPvp;
        this.color = "black";
    }

    
    public static Team createEmpty(String name, String tag, String color, boolean pvpToggle) {
        return new Team(name, tag, color, pvpToggle);
    }

    
    private Team(String name, String tag, String color, boolean pvpToggle) {
        this.name = name;
        this.tag = tag;
        this.color = color;
        this.pvpToggle = pvpToggle;
    }

    public int getTotalMembers() {
        return owners.size() + admins.size() + members.size();
    }

    
    public String getDisplayName() {
        if (name == null || name.isEmpty()) return name;
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public boolean isMember(UUID uuid) {
        return owners.contains(uuid) || admins.contains(uuid) || members.contains(uuid);
    }

    public boolean isOwner(UUID uuid) {
        return owners.contains(uuid);
    }

    public boolean isAdmin(UUID uuid) {
        return admins.contains(uuid);
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        owners.remove(uuid);
        admins.remove(uuid);
        members.remove(uuid);
        chatEnabled.remove(uuid);
    }

    public void promoteToAdmin(UUID uuid) {
        if (members.remove(uuid)) {
            admins.add(uuid);
        }
    }

    public void promoteToOwner(UUID uuid) {
        if (admins.remove(uuid) || members.remove(uuid)) {
            owners.add(uuid);
        }
    }

    
    public void demoteToAdmin(UUID uuid) {
        if (owners.remove(uuid)) {
            admins.add(uuid);
        }
    }

    public void demoteToMember(UUID uuid) {
        if (owners.remove(uuid) || admins.remove(uuid)) {
            members.add(uuid);
        }
    }

    public boolean isTeamChatEnabled(UUID uuid) {
        return chatEnabled.getOrDefault(uuid, false);
    }

    public void setTeamChat(UUID uuid, boolean enabled) {
        if (!isMember(uuid)) return;
        chatEnabled.put(uuid, enabled);
    }

    public void toggleTeamChat(UUID uuid) {
        if (!isMember(uuid)) return;
        chatEnabled.put(uuid, !isTeamChatEnabled(uuid));
    }

    public void removeChat(UUID uuid) {
        chatEnabled.remove(uuid);
    }

    public void setWarp(String name, Location location) {
        warps.put(name.toLowerCase(), location);
    }

    public Location getWarp(String name) {
        return warps.get(name.toLowerCase());
    }

    public void removeWarp(String name) {
        warps.remove(name.toLowerCase());
    }

    public boolean hasWarp(String name) {
        return warps.containsKey(name.toLowerCase());
    }

    public void addAlly(String teamName) {
        allies.add(teamName.toLowerCase());
        enemies.remove(teamName.toLowerCase());
    }

    public void addEnemy(String teamName) {
        enemies.add(teamName.toLowerCase());
        allies.remove(teamName.toLowerCase());
    }

    public void removeAlly(String teamName) {
        allies.remove(teamName.toLowerCase());
    }

    public void removeEnemy(String teamName) {
        enemies.remove(teamName.toLowerCase());
    }

    public boolean isAlly(String teamName) {
        return allies.contains(teamName.toLowerCase());
    }

    public boolean isEnemy(String teamName) {
        return enemies.contains(teamName.toLowerCase());
    }

    @Override
    public String toString() {
        return "Team{" +
                "name='" + name + '\'' +
                ", tag='" + tag + '\'' +
                ", color='" + color + '\'' +
                ", pvp=" + pvpToggle +
                ", owners=" + owners +
                ", admins=" + admins +
                ", members=" + members +
                ", warps=" + warps.keySet() +
                ", allies=" + allies +
                ", enemies=" + enemies +
                '}';
    }
}
