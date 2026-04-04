package dev.frost.frostcore.manager;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.exceptions.TeamException;
import dev.frost.frostcore.teams.Team;
import dev.frost.frostcore.teams.TeamError;
import lombok.Getter;
import org.bukkit.Location;

import java.util.*;

public class TeamManager {

    private final Map<String, Team> teams = new HashMap<>();
    private final Map<UUID, Team> playerTeams = new HashMap<>();
    @Getter private static TeamManager instance;

    private final ConfigManager config = Main.getConfigManager();
    private int maxOwners() { return config.getInt("teams.max-owners"); }
    private int maxAdmins() { return config.getInt("teams.max-admins"); }
    private int playerLimit() { return config.getInt("teams.player-limit"); }
    private int maxWarps() { return config.getInt("teams.warps.limit"); }
    private int maxAllies() { return config.getInt("teams.relations.max-allies"); }
    private int maxEnemies() { return config.getInt("teams.relations.max-enemies"); }
    private int minName() { return config.getInt("teams.team-name-min-length"); }
    private int maxName() { return config.getInt("teams.team-name-max-length"); }
    private List<String> bannedNames() { return config.getStringList("teams.team-name-banned"); }
    private boolean defaultPvp() { return config.getBoolean("teams.pvp-toggle"); }


    private TeamManager() {
        instance = this;
    }

    public Team getTeam(String name) throws TeamException {
        Team team = teams.get(name.toLowerCase());
        if (team == null) {
            throw new TeamException(TeamError.TEAM_NOT_FOUND, "Team not found");
        }
        return team;
    }

    public Team getTeam(UUID uuid) throws TeamException {
        Team team = playerTeams.get(uuid);
        if (team == null) {
            throw new TeamException(TeamError.PLAYER_NOT_IN_TEAM, "Player is not in a team");
        }
        return team;
    }

    public boolean hasTeam(UUID uuid) {
        return playerTeams.containsKey(uuid);
    }

    public Team createTeam(String name, String tag, UUID owner) throws TeamException {
        name = name.toLowerCase();

        if (teams.containsKey(name)) {
            throw new TeamException(TeamError.TEAM_ALREADY_EXISTS, "Team already exists");
        }

        if (hasTeam(owner)) {
            throw new TeamException(TeamError.PLAYER_ALREADY_IN_TEAM, "Player already in a team");
        }

        if (name.length() < minName()) {
            throw new TeamException(TeamError.TEAM_NAME_TOO_SHORT, "Team name too short");
        }

        if (name.length() > maxName()) {
            throw new TeamException(TeamError.TEAM_NAME_TOO_LONG, "Team name too long");
        }

        for (String banned : bannedNames()) {
            if (name.contains(banned.toLowerCase())) {
                throw new TeamException(TeamError.TEAM_NAME_BANNED, "Team name contains banned word");
            }
        }

        Team team = new Team(name, tag, owner, defaultPvp());

        teams.put(name, team);
        playerTeams.put(owner, team);

        return team;
    }

    public void disbandTeam(String name) throws TeamException {
        Team team = teams.remove(name.toLowerCase());
        if (team == null) {
            throw new TeamException(TeamError.TEAM_NOT_FOUND, "Team not found");
        }

        team.getOwners().forEach(playerTeams::remove);
        team.getAdmins().forEach(playerTeams::remove);
        team.getMembers().forEach(playerTeams::remove);
    }

    public void addMember(Team team, UUID uuid) throws TeamException {
        if (hasTeam(uuid)) {
            throw new TeamException(TeamError.PLAYER_ALREADY_IN_TEAM, "Player already in a team");
        }

        if (team.getTotalMembers() >= playerLimit()) {
            throw new TeamException(TeamError.TEAM_FULL, "Team is full");
        }

        team.addMember(uuid);
        playerTeams.put(uuid, team);
    }

    public void removeMember(UUID uuid) throws TeamException {
        Team team = getTeam(uuid);

        team.removeMember(uuid);
        playerTeams.remove(uuid);
    }

    public void promoteToAdmin(Team team, UUID uuid) throws TeamException {
        if (!team.isMember(uuid)) {
            throw new TeamException(TeamError.PLAYER_NOT_IN_TEAM, "Player not in team");
        }

        if (team.getAdmins().size() >= maxAdmins()) {
            throw new TeamException(TeamError.MAX_ADMINS_REACHED, "Max admins reached");
        }

        team.promoteToAdmin(uuid);
    }

    public void promoteToOwner(Team team, UUID uuid) throws TeamException {
        if (!team.isMember(uuid)) {
            throw new TeamException(TeamError.PLAYER_NOT_IN_TEAM, "Player not in team");
        }

        if (team.getOwners().size() >= maxOwners()) {
            throw new TeamException(TeamError.MAX_OWNERS_REACHED, "Max owners reached");
        }

        team.promoteToOwner(uuid);
    }

    public void demoteToMember(Team team, UUID uuid) throws TeamException {
        if (!team.isMember(uuid)) {
            throw new TeamException(TeamError.PLAYER_NOT_IN_TEAM, "Player not in team");
        }

        team.demoteToMember(uuid);
    }

    public void setHome(Team team, Location loc) throws TeamException {
        if (!config.getBoolean("teams.home.enabled")) {
            throw new TeamException(TeamError.HOME_DISABLED, "Team homes are disabled");
        }

        team.setHome(loc);
    }

    public void setWarp(Team team, String name, Location loc) throws TeamException {
        name = name.toLowerCase();

        if (!config.getBoolean("teams.warps.enabled")) {
            throw new TeamException(TeamError.WARPS_DISABLED, "Warps are disabled");
        }

        if (team.hasWarp(name)) {
            throw new TeamException(TeamError.WARP_ALREADY_EXISTS, "Warp already exists");
        }

        if (team.getWarps().size() >= maxWarps()) {
            throw new TeamException(TeamError.MAX_WARPS_REACHED, "Max warps reached");
        }

        team.setWarp(name, loc);
    }

    public void deleteWarp(Team team, String name) throws TeamException {
        name = name.toLowerCase();

        if (!team.hasWarp(name)) {
            throw new TeamException(TeamError.WARP_DOES_NOT_EXIST, "Warp does not exist");
        }

        team.removeWarp(name);
    }

    public void addAlly(Team team, String target) throws TeamException {
        target = target.toLowerCase();

        if (!config.getBoolean("teams.relations.allies-enabled")) {
            throw new TeamException(TeamError.ALLIES_DISABLED, "Allies are disabled");
        }

        if (team.getName().equalsIgnoreCase(target)) {
            throw new TeamException(TeamError.CANNOT_TARGET_SELF, "Cannot ally yourself");
        }

        if (team.isAlly(target)) {
            throw new TeamException(TeamError.ALREADY_ALLY, "Already allied");
        }

        if (team.getAllies().size() >= maxAllies()) {
            throw new TeamException(TeamError.MAX_ALLIES_REACHED, "Max allies reached");
        }

        team.addAlly(target);
    }

    public void addEnemy(Team team, String target) throws TeamException {
        target = target.toLowerCase();

        if (!config.getBoolean("teams.relations.enemies-enabled")) {
            throw new TeamException(TeamError.ENEMIES_DISABLED, "Enemies are disabled");
        }

        if (team.getName().equalsIgnoreCase(target)) {
            throw new TeamException(TeamError.CANNOT_TARGET_SELF, "Cannot enemy yourself");
        }

        if (team.isEnemy(target)) {
            throw new TeamException(TeamError.ALREADY_ENEMY, "Already enemy");
        }

        if (team.getEnemies().size() >= maxEnemies()) {
            throw new TeamException(TeamError.MAX_ENEMIES_REACHED, "Max enemies reached");
        }

        team.addEnemy(target);
    }

    public Collection<Team> getAllTeams() {
        return teams.values();
    }

    public void clear() {
        teams.clear();
        playerTeams.clear();
    }
}