package dev.frost.frostcore.manager;

import dev.frost.frostcore.utils.FrostLogger;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.database.DatabaseManager;
import dev.frost.frostcore.exceptions.TeamException;
import dev.frost.frostcore.teams.Team;
import dev.frost.frostcore.teams.TeamError;
import org.bukkit.Location;

import java.util.*;

public class TeamManager {

    private final Map<String, Team> teams = new HashMap<>();
    private final Map<UUID, Team> playerTeams = new HashMap<>();
    private static TeamManager instance;

    private final ConfigManager config = Main.getConfigManager();
    private DatabaseManager db;

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

    private TeamManager() {}

    /**
     * Thread-safe lazy singleton initialization.
     */
    public static TeamManager getInstance() {
        if (instance == null) {
            instance = new TeamManager();
        }
        return instance;
    }

    /**
     * Set the database manager. Called from Main after DatabaseManager is initialized.
     */
    public void setDatabaseManager(DatabaseManager db) {
        this.db = db;
    }

    // ==================== DATA LOADING ====================

    /**
     * Load all teams from the database into memory.
     * Called synchronously during onEnable.
     */
    public void loadAll() {
        if (db == null) {
            FrostLogger.warn("DatabaseManager is null — cannot load teams!");
            return;
        }

        clear();

        List<Team> loaded = db.loadAllTeams();
        for (Team team : loaded) {
            teams.put(team.getName().toLowerCase(), team);

            // Populate playerTeams index
            for (UUID uuid : team.getOwners()) playerTeams.put(uuid, team);
            for (UUID uuid : team.getAdmins()) playerTeams.put(uuid, team);
            for (UUID uuid : team.getMembers()) playerTeams.put(uuid, team);
        }

        FrostLogger.info("Loaded " + teams.size() + " teams into memory.");
    }

    /**
     * Synchronously save all teams to the database.
     * Called during onDisable.
     */
    public void saveAll() {
        if (db == null) return;
        db.saveAllTeams(teams.values());
    }

    // ==================== QUERIES ====================

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

    /**
     * Get a player's role string: "OWNER", "ADMIN", or "MEMBER".
     * Returns null if not in a team.
     */
    public String getRole(Team team, UUID uuid) {
        if (team.isOwner(uuid)) return "OWNER";
        if (team.isAdmin(uuid)) return "ADMIN";
        if (team.getMembers().contains(uuid)) return "MEMBER";
        return null;
    }

    public boolean hasTeam(UUID uuid) {
        return playerTeams.containsKey(uuid);
    }

    public Collection<Team> getAllTeams() {
        return teams.values();
    }

    // ==================== MUTATIONS ====================

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

        if (db != null) db.saveTeamAsync(team);

        return team;
    }

    public void disbandTeam(String name) throws TeamException {
        Team team = teams.remove(name.toLowerCase());
        if (team == null) {
            throw new TeamException(TeamError.TEAM_NOT_FOUND, "Team not found");
        }

        // Remove player-to-team mappings
        team.getOwners().forEach(playerTeams::remove);
        team.getAdmins().forEach(playerTeams::remove);
        team.getMembers().forEach(playerTeams::remove);

        // Clean up ally/enemy references in OTHER teams
        for (String allyName : team.getAllies()) {
            Team ally = teams.get(allyName);
            if (ally != null) {
                ally.removeAlly(name.toLowerCase());
                if (db != null) db.saveRelationsAsync(ally);
            }
        }
        for (String enemyName : team.getEnemies()) {
            Team enemy = teams.get(enemyName);
            if (enemy != null) {
                enemy.removeEnemy(name.toLowerCase());
                if (db != null) db.saveRelationsAsync(enemy);
            }
        }

        if (db != null) {
            db.deleteTeamAsync(name.toLowerCase());
            db.deleteEchest(name.toLowerCase());
        }

        // Invalidate echest cache
        if (Main.getEchestManager() != null) {
            Main.getEchestManager().invalidate(name);
        }
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

        if (db != null) db.saveMembersAsync(team);
    }

    /**
     * Remove a member from their team (leave/kick).
     * Blocks owners from leaving if they're the last owner.
     */
    public void removeMember(UUID uuid) throws TeamException {
        Team team = getTeam(uuid);

        // Prevent the last owner from leaving
        if (team.isOwner(uuid) && team.getOwners().size() <= 1) {
            throw new TeamException(TeamError.CANNOT_LEAVE_AS_OWNER,
                    "You are the last owner. Use /team disband or /team promote someone first.");
        }

        team.removeMember(uuid);
        playerTeams.remove(uuid);

        if (db != null) db.saveMembersAsync(team);
    }

    public void promoteToAdmin(Team team, UUID uuid) throws TeamException {
        if (!team.getMembers().contains(uuid)) {
            throw new TeamException(TeamError.ALREADY_HIGHEST_RANK, "Player is not a member to promote");
        }

        if (team.getAdmins().size() >= maxAdmins()) {
            throw new TeamException(TeamError.MAX_ADMINS_REACHED, "Max admins reached");
        }

        team.promoteToAdmin(uuid);

        if (db != null) db.saveMembersAsync(team);
    }

    public void promoteToOwner(Team team, UUID uuid) throws TeamException {
        if (!team.isAdmin(uuid)) {
            throw new TeamException(TeamError.ALREADY_HIGHEST_RANK, "Player must be an admin to promote to owner");
        }

        if (team.getOwners().size() >= maxOwners()) {
            throw new TeamException(TeamError.MAX_OWNERS_REACHED, "Max owners reached");
        }

        team.promoteToOwner(uuid);

        if (db != null) db.saveMembersAsync(team);
    }

    public void demoteOwnerToAdmin(Team team, UUID uuid) throws TeamException {
        if (!team.isOwner(uuid)) {
            throw new TeamException(TeamError.ALREADY_LOWEST_RANK, "Player is not an owner");
        }
        if (team.getOwners().size() <= 1) {
            throw new TeamException(TeamError.CANNOT_DEMOTE_LAST_OWNER, "Cannot demote the last owner");
        }

        team.getOwners().remove(uuid);
        team.getAdmins().add(uuid);

        if (db != null) db.saveMembersAsync(team);
    }

    public void demoteAdminToMember(Team team, UUID uuid) throws TeamException {
        if (!team.isAdmin(uuid)) {
            throw new TeamException(TeamError.ALREADY_LOWEST_RANK, "Player is not an admin");
        }

        team.getAdmins().remove(uuid);
        team.getMembers().add(uuid);

        if (db != null) db.saveMembersAsync(team);
    }

    public void demoteToMember(Team team, UUID uuid) throws TeamException {
        if (!team.isMember(uuid)) {
            throw new TeamException(TeamError.PLAYER_NOT_IN_TEAM, "Player not in team");
        }

        team.demoteToMember(uuid);

        if (db != null) db.saveMembersAsync(team);
    }

    // ==================== HOME / WARP ====================

    public void setHome(Team team, Location loc) throws TeamException {
        if (!config.getBoolean("teams.home.enabled")) {
            throw new TeamException(TeamError.HOME_DISABLED, "Team homes are disabled");
        }

        team.setHome(loc);

        if (db != null) db.saveTeamBaseAsync(team);
    }

    /**
     * Delete the team home and save to database.
     */
    public void deleteHome(Team team) {
        team.setHome(null);
        if (db != null) db.saveTeamBaseAsync(team);
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

        if (db != null) db.saveWarpsAsync(team);
    }

    public void deleteWarp(Team team, String name) throws TeamException {
        name = name.toLowerCase();

        if (!team.hasWarp(name)) {
            throw new TeamException(TeamError.WARP_DOES_NOT_EXIST, "Warp does not exist");
        }

        team.removeWarp(name);

        if (db != null) db.saveWarpsAsync(team);
    }

    // ==================== RELATIONS ====================

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

        if (db != null) db.saveRelationsAsync(team);
    }

    /**
     * Remove an ally relationship (mutual removal).
     */
    public void removeAlly(Team team, String target) throws TeamException {
        target = target.toLowerCase();

        if (!team.isAlly(target)) {
            throw new TeamException(TeamError.NOT_ALLY, "Not allied with that team");
        }

        team.removeAlly(target);
        if (db != null) db.saveRelationsAsync(team);

        // Remove from the other team too (mutual)
        Team otherTeam = teams.get(target);
        if (otherTeam != null) {
            otherTeam.removeAlly(team.getName());
            if (db != null) db.saveRelationsAsync(otherTeam);
        }
    }

    public void addEnemy(Team team, String target) throws TeamException {
        target = target.toLowerCase();

        // Validate the target team exists
        if (!teams.containsKey(target)) {
            throw new TeamException(TeamError.TEAM_NOT_FOUND, "Target team not found");
        }

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

        if (db != null) db.saveRelationsAsync(team);
    }

    /**
     * Remove an enemy relationship (one-sided — only removes from your team).
     */
    public void removeEnemy(Team team, String target) throws TeamException {
        target = target.toLowerCase();

        if (!team.isEnemy(target)) {
            throw new TeamException(TeamError.NOT_ENEMY, "Not enemies with that team");
        }

        team.removeEnemy(target);

        if (db != null) db.saveRelationsAsync(team);
    }

    // ==================== SAVE HELPERS ====================

    public void savePvpToggle(Team team) {
        if (db != null) db.saveTeamBaseAsync(team);
    }

    public void saveTag(Team team) {
        if (db != null) db.saveTeamBaseAsync(team);
    }

    public void saveColor(Team team) {
        if (db != null) db.saveTeamBaseAsync(team);
    }

    public void saveChatToggle(Team team) {
        if (db != null) db.saveMembersAsync(team);
    }

    // ==================== RENAME ====================

    /**
     * Rename a team. Updates in-memory state and all DB tables atomically.
     *
     * @param team the team to rename
     * @param newName the new name
     * @throws TeamException if the new name is invalid or already taken
     */
    public void renameTeam(Team team, String newName) throws TeamException {
        String newNameLower = newName.toLowerCase();
        String oldNameLower = team.getName().toLowerCase();

        // Validate new name
        if (teams.containsKey(newNameLower)) {
            throw new TeamException(TeamError.TEAM_ALREADY_EXISTS, "A team with that name already exists");
        }
        if (newNameLower.length() < minName()) {
            throw new TeamException(TeamError.TEAM_NAME_TOO_SHORT, "Team name too short");
        }
        if (newNameLower.length() > maxName()) {
            throw new TeamException(TeamError.TEAM_NAME_TOO_LONG, "Team name too long");
        }
        for (String banned : bannedNames()) {
            if (newNameLower.contains(banned.toLowerCase())) {
                throw new TeamException(TeamError.TEAM_NAME_BANNED, "Team name contains banned word");
            }
        }

        // Update DB atomically asynchronously — optimistic memory update
        if (db != null) {
            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
                boolean success = db.renameTeam(oldNameLower, newNameLower);
                if (!success) {
                    dev.frost.frostcore.utils.FrostLogger.error("Database rename failed for team " + oldNameLower + " -> " + newNameLower);
                }
            });
        }

        // Update in-memory map
        teams.remove(oldNameLower);
        team.setName(newNameLower);
        teams.put(newNameLower, team);

        // Update ally/enemy references in other teams
        for (Team other : teams.values()) {
            if (other == team) continue;
            if (other.isAlly(oldNameLower)) {
                other.removeAlly(oldNameLower);
                other.addAlly(newNameLower);
            }
            if (other.isEnemy(oldNameLower)) {
                other.removeEnemy(oldNameLower);
                other.addEnemy(newNameLower);
            }
        }

        // Invalidate echest cache (old name key)
        if (Main.getEchestManager() != null) {
            Main.getEchestManager().invalidate(oldNameLower);
        }
    }

    public void clear() {
        teams.clear();
        playerTeams.clear();
    }
}
