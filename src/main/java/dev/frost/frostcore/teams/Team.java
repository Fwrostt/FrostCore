package dev.frost.frostcore.teams;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

import java.util.*;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

@Getter
public class Team implements ConfigurationSerializable {

    @Setter private String name;
    @Setter private String tag;
    @Setter private String color;
    private final Set<UUID> owners = new HashSet<>();
    private final Set<UUID> admins = new HashSet<>();
    private final Set<UUID> members = new HashSet<>();
    private final Map<UUID, Boolean> chatEnabled = new HashMap<>();
    @Setter private boolean pvpToggle;
    @Setter private Location home;
    private final Map<String, Location> warps = new HashMap<>();
    private final Set<String> allies = new HashSet<>();
    private final Set<String> enemies = new HashSet<>();

    public Team(String name,String tag, UUID owner, boolean defaultPvp) {
        this.name = name;
        this.tag = tag;
        this.owners.add(owner);
        this.pvpToggle = defaultPvp;
        this.color = "black";
    }

    public int getTotalMembers() {
        return owners.size() + admins.size() + members.size();
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
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("tag", tag);
        data.put("color", color);
        data.put("pvp", pvpToggle);
        data.put("owners", owners.stream().map(UUID::toString).toList());
        data.put("admins", admins.stream().map(UUID::toString).toList());
        data.put("members", members.stream().map(UUID::toString).toList());
        if (home != null) {
            data.put("home", serializeLocation(home));
        }
        Map<String, Object> warpMap = new HashMap<>();
        for (Map.Entry<String, Location> entry : warps.entrySet()) {
            warpMap.put(entry.getKey(), serializeLocation(entry.getValue()));
        }
        data.put("chat-enabled", chatEnabled.entrySet().stream()
                .collect(HashMap::new, (map, e) -> map.put(e.getKey().toString(), e.getValue()), HashMap::putAll));
        data.put("warps", warpMap);
        data.put("allies", new ArrayList<>(allies));
        data.put("enemies", new ArrayList<>(enemies));
        return data;
    }

    private Map<String, Object> serializeLocation(Location loc) {
        Map<String, Object> map = new HashMap<>();
        map.put("world", loc.getWorld().getName());
        map.put("x", loc.getX());
        map.put("y", loc.getY());
        map.put("z", loc.getZ());
        map.put("yaw", loc.getYaw());
        map.put("pitch", loc.getPitch());
        return map;
    }

    private static Location deserializeLocation(Map<String, Object> map) {
        World world = Bukkit.getWorld((String) map.get("world"));
        if (world == null) {
            return null;
        }
        double x = ((Number) map.get("x")).doubleValue();
        double y = ((Number) map.get("y")).doubleValue();
        double z = ((Number) map.get("z")).doubleValue();
        float yaw = ((Number) map.get("yaw")).floatValue();
        float pitch = ((Number) map.get("pitch")).floatValue();
        return new Location(world, x, y, z, yaw, pitch);
    }

    @SuppressWarnings("unchecked")
    public static Team deserialize(Map<String, Object> map) {
        String name = (String) map.get("name");
        String tag = (String) map.get("tag");
        boolean pvp = (boolean) map.get("pvp");
        List<String> ownerList = (List<String>) map.get("owners");
        UUID owner = UUID.fromString(ownerList.getFirst());
        Team team = new Team(name, tag, owner, pvp);
        team.setColor((String) map.get("color"));
        for (String uuid : ownerList) {
            team.getOwners().add(UUID.fromString(uuid));
        }
        for (String uuid : (List<String>) map.get("admins")) {
            team.getAdmins().add(UUID.fromString(uuid));
        }
        for (String uuid : (List<String>) map.get("members")) {
            team.getMembers().add(UUID.fromString(uuid));
        }
        if (map.containsKey("home")) {
            team.setHome(deserializeLocation((Map<String, Object>) map.get("home")));
        }
        Map<String, Object> warpMap = (Map<String, Object>) map.get("warps");
        for (Map.Entry<String, Object> entry : warpMap.entrySet()) {
            Location loc = deserializeLocation((Map<String, Object>) entry.getValue());
            team.getWarps().put(entry.getKey(), loc);
        }
        team.getAllies().addAll((List<String>) map.get("allies"));
        team.getEnemies().addAll((List<String>) map.get("enemies"));
        if (map.containsKey("chat-enabled")) {
            Map<String, Object> chatMap = (Map<String, Object>) map.get("chat-enabled");
            for (Map.Entry<String, Object> entry : chatMap.entrySet()) {
                team.getChatEnabled().put(UUID.fromString(entry.getKey()), (Boolean) entry.getValue());
            }
        }
        return team;
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