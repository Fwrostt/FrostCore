package dev.frost.frostcore.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.frost.frostcore.Main;
import dev.frost.frostcore.manager.ConfigManager;
import dev.frost.frostcore.teams.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages the HikariCP connection pool and all database CRUD operations.
 * Supports SQLite (default) and MySQL, switchable via config.
 * <p>
 * All public write methods have an async variant that runs on the
 * Bukkit async scheduler. Reads are always synchronous (called on startup).
 */
public class DatabaseManager {

    private final Main plugin;
    private HikariDataSource dataSource;
    private DatabaseType type;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
    }

    // ==================== LIFECYCLE ====================

    /**
     * Initialize the connection pool and create tables.
     * Call this during onEnable, before loading data.
     */
    public void init() {
        ConfigManager config = Main.getConfigManager();
        String typeStr = config.getString("database.type", "SQLITE").toUpperCase();

        try {
            type = DatabaseType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown database type '" + typeStr + "', falling back to SQLITE.");
            type = DatabaseType.SQLITE;
        }

        HikariConfig hikariConfig = new HikariConfig();

        if (type == DatabaseType.MYSQL) {
            String host = config.getString("database.mysql.host", "localhost");
            int port = config.getInt("database.mysql.port", 3306);
            String database = config.getString("database.mysql.database", "frostcore");
            String username = config.getString("database.mysql.username", "root");
            String password = config.getString("database.mysql.password", "");
            int poolSize = config.getInt("database.mysql.pool-size", 10);

            hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8");
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setMaximumPoolSize(poolSize);
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");

            plugin.getLogger().info("Using MySQL database at " + host + ":" + port + "/" + database);
        } else {
            File dbFile = new File(plugin.getDataFolder(), "database.db");
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hikariConfig.setMaximumPoolSize(1); // SQLite only supports one writer
            hikariConfig.setDriverClassName("org.sqlite.JDBC");

            // SQLite-specific optimizations
            hikariConfig.addDataSourceProperty("journal_mode", "WAL");
            hikariConfig.addDataSourceProperty("synchronous", "NORMAL");

            plugin.getLogger().info("Using SQLite database at " + dbFile.getAbsolutePath());
        }

        hikariConfig.setPoolName("FrostCore-DB");
        hikariConfig.setConnectionTimeout(5000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);

        dataSource = new HikariDataSource(hikariConfig);

        createTables();
    }

    /**
     * Shut down the connection pool. Call during onDisable, after saving.
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection pool closed.");
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // ==================== TABLE CREATION ====================

    private void createTables() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            // Enable WAL mode for SQLite
            if (type == DatabaseType.SQLITE) {
                stmt.execute("PRAGMA journal_mode=WAL;");
                stmt.execute("PRAGMA foreign_keys=ON;");
            }

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS teams (
                    name        VARCHAR(64) PRIMARY KEY,
                    tag         VARCHAR(32) NOT NULL,
                    color       VARCHAR(32) NOT NULL DEFAULT 'black',
                    pvp_toggle  BOOLEAN NOT NULL DEFAULT 1,
                    home_world  VARCHAR(128),
                    home_x      DOUBLE,
                    home_y      DOUBLE,
                    home_z      DOUBLE,
                    home_yaw    FLOAT,
                    home_pitch  FLOAT
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS team_members (
                    uuid        VARCHAR(36) PRIMARY KEY,
                    team_name   VARCHAR(64) NOT NULL,
                    role        VARCHAR(16) NOT NULL,
                    chat_enabled BOOLEAN NOT NULL DEFAULT 0
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS team_warps (
                    team_name   VARCHAR(64) NOT NULL,
                    warp_name   VARCHAR(64) NOT NULL,
                    world       VARCHAR(128) NOT NULL,
                    x           DOUBLE NOT NULL,
                    y           DOUBLE NOT NULL,
                    z           DOUBLE NOT NULL,
                    yaw         FLOAT NOT NULL,
                    pitch       FLOAT NOT NULL,
                    PRIMARY KEY (team_name, warp_name)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS team_relations (
                    team_name     VARCHAR(64) NOT NULL,
                    target_team   VARCHAR(64) NOT NULL,
                    relation_type VARCHAR(16) NOT NULL,
                    PRIMARY KEY (team_name, target_team)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS team_echests (
                    team_name   VARCHAR(64) PRIMARY KEY,
                    contents    MEDIUMTEXT
                )
            """);

            plugin.getLogger().info("Database tables verified.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create database tables!", e);
        }
    }

    // ==================== LOAD ====================

    /**
     * Load all teams from the database. Called synchronously on startup.
     *
     * @return list of fully-populated Team objects
     */
    public List<Team> loadAllTeams() {
        Map<String, Team> teamMap = new LinkedHashMap<>();

        // 1. Load base team data
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM teams");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("name");
                String tag = rs.getString("tag");
                String color = rs.getString("color");
                boolean pvp = rs.getBoolean("pvp_toggle");

                // Create team with a dummy owner — we'll populate members separately
                Team team = Team.createEmpty(name, tag, color, pvp);

                // Home location
                String homeWorld = rs.getString("home_world");
                if (homeWorld != null && !rs.wasNull()) {
                    World world = Bukkit.getWorld(homeWorld);
                    if (world != null) {
                        team.setHome(new Location(world,
                                rs.getDouble("home_x"),
                                rs.getDouble("home_y"),
                                rs.getDouble("home_z"),
                                rs.getFloat("home_yaw"),
                                rs.getFloat("home_pitch")));
                    }
                }

                teamMap.put(name, team);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load teams!", e);
        }

        // 2. Load members
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM team_members");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String teamName = rs.getString("team_name");
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String role = rs.getString("role");
                boolean chatEnabled = rs.getBoolean("chat_enabled");

                Team team = teamMap.get(teamName);
                if (team == null) continue;

                switch (role.toUpperCase()) {
                    case "OWNER" -> team.getOwners().add(uuid);
                    case "ADMIN" -> team.getAdmins().add(uuid);
                    default -> team.getMembers().add(uuid);
                }

                if (chatEnabled) {
                    team.getChatEnabled().put(uuid, true);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load team members!", e);
        }

        // 3. Load warps
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM team_warps");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String teamName = rs.getString("team_name");
                String warpName = rs.getString("warp_name");

                Team team = teamMap.get(teamName);
                if (team == null) continue;

                World world = Bukkit.getWorld(rs.getString("world"));
                if (world == null) continue;

                Location loc = new Location(world,
                        rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                        rs.getFloat("yaw"), rs.getFloat("pitch"));
                team.getWarps().put(warpName, loc);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load team warps!", e);
        }

        // 4. Load relations
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM team_relations");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String teamName = rs.getString("team_name");
                String targetTeam = rs.getString("target_team");
                String relType = rs.getString("relation_type");

                Team team = teamMap.get(teamName);
                if (team == null) continue;

                if ("ALLY".equalsIgnoreCase(relType)) {
                    team.getAllies().add(targetTeam);
                } else if ("ENEMY".equalsIgnoreCase(relType)) {
                    team.getEnemies().add(targetTeam);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load team relations!", e);
        }

        plugin.getLogger().info("Loaded " + teamMap.size() + " teams from database.");
        return new ArrayList<>(teamMap.values());
    }

    // ==================== SAVE (FULL TEAM) ====================

    /**
     * Save an entire team to the database (upsert all tables).
     * Use for initial creation or full sync.
     */
    public void saveTeam(Team team) {
        saveTeamBase(team);
        saveMembers(team);
        saveWarps(team);
        saveRelations(team);
    }

    /**
     * Async variant — runs on Bukkit async scheduler.
     */
    public void saveTeamAsync(Team team) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveTeam(team));
    }

    // ==================== SAVE (INDIVIDUAL) ====================

    /**
     * Save/update the base team row (name, tag, color, pvp, home).
     */
    public void saveTeamBase(Team team) {
        String sql = type == DatabaseType.MYSQL
                ? """
                INSERT INTO teams (name, tag, color, pvp_toggle, home_world, home_x, home_y, home_z, home_yaw, home_pitch)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE tag=VALUES(tag), color=VALUES(color), pvp_toggle=VALUES(pvp_toggle),
                    home_world=VALUES(home_world), home_x=VALUES(home_x), home_y=VALUES(home_y),
                    home_z=VALUES(home_z), home_yaw=VALUES(home_yaw), home_pitch=VALUES(home_pitch)
                """
                : """
                INSERT OR REPLACE INTO teams (name, tag, color, pvp_toggle, home_world, home_x, home_y, home_z, home_yaw, home_pitch)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, team.getName());
            ps.setString(2, team.getTag());
            ps.setString(3, team.getColor());
            ps.setBoolean(4, team.isPvpToggle());

            Location home = team.getHome();
            if (home != null && home.getWorld() != null) {
                ps.setString(5, home.getWorld().getName());
                ps.setDouble(6, home.getX());
                ps.setDouble(7, home.getY());
                ps.setDouble(8, home.getZ());
                ps.setFloat(9, home.getYaw());
                ps.setFloat(10, home.getPitch());
            } else {
                ps.setNull(5, Types.VARCHAR);
                ps.setNull(6, Types.DOUBLE);
                ps.setNull(7, Types.DOUBLE);
                ps.setNull(8, Types.DOUBLE);
                ps.setNull(9, Types.FLOAT);
                ps.setNull(10, Types.FLOAT);
            }

            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save team base: " + team.getName(), e);
        }
    }

    public void saveTeamBaseAsync(Team team) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveTeamBase(team));
    }

    /**
     * Replace all members for a team (delete + re-insert).
     */
    public void saveMembers(Team team) {
        try (Connection conn = getConnection()) {
            // Delete existing members for this team
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM team_members WHERE team_name = ?")) {
                del.setString(1, team.getName());
                del.executeUpdate();
            }

            String sql = "INSERT INTO team_members (uuid, team_name, role, chat_enabled) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (UUID uuid : team.getOwners()) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, team.getName());
                    ps.setString(3, "OWNER");
                    ps.setBoolean(4, team.getChatEnabled().getOrDefault(uuid, false));
                    ps.addBatch();
                }
                for (UUID uuid : team.getAdmins()) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, team.getName());
                    ps.setString(3, "ADMIN");
                    ps.setBoolean(4, team.getChatEnabled().getOrDefault(uuid, false));
                    ps.addBatch();
                }
                for (UUID uuid : team.getMembers()) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, team.getName());
                    ps.setString(3, "MEMBER");
                    ps.setBoolean(4, team.getChatEnabled().getOrDefault(uuid, false));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save members for team: " + team.getName(), e);
        }
    }

    public void saveMembersAsync(Team team) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveMembers(team));
    }

    /**
     * Replace all warps for a team (delete + re-insert).
     */
    public void saveWarps(Team team) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM team_warps WHERE team_name = ?")) {
                del.setString(1, team.getName());
                del.executeUpdate();
            }

            String sql = "INSERT INTO team_warps (team_name, warp_name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Map.Entry<String, Location> entry : team.getWarps().entrySet()) {
                    Location loc = entry.getValue();
                    if (loc.getWorld() == null) continue;

                    ps.setString(1, team.getName());
                    ps.setString(2, entry.getKey());
                    ps.setString(3, loc.getWorld().getName());
                    ps.setDouble(4, loc.getX());
                    ps.setDouble(5, loc.getY());
                    ps.setDouble(6, loc.getZ());
                    ps.setFloat(7, loc.getYaw());
                    ps.setFloat(8, loc.getPitch());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save warps for team: " + team.getName(), e);
        }
    }

    public void saveWarpsAsync(Team team) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveWarps(team));
    }

    /**
     * Replace all relations for a team (delete + re-insert).
     */
    public void saveRelations(Team team) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM team_relations WHERE team_name = ?")) {
                del.setString(1, team.getName());
                del.executeUpdate();
            }

            String sql = "INSERT INTO team_relations (team_name, target_team, relation_type) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (String ally : team.getAllies()) {
                    ps.setString(1, team.getName());
                    ps.setString(2, ally);
                    ps.setString(3, "ALLY");
                    ps.addBatch();
                }
                for (String enemy : team.getEnemies()) {
                    ps.setString(1, team.getName());
                    ps.setString(2, enemy);
                    ps.setString(3, "ENEMY");
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save relations for team: " + team.getName(), e);
        }
    }

    public void saveRelationsAsync(Team team) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveRelations(team));
    }

    // ==================== DELETE ====================

    /**
     * Delete a team and all associated data from the database.
     */
    public void deleteTeam(String teamName) {
        try (Connection conn = getConnection()) {
            // Delete in order: relations, warps, members, then team
            for (String table : new String[]{"team_relations", "team_warps", "team_members"}) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM " + table + " WHERE team_name = ?")) {
                    ps.setString(1, teamName);
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM teams WHERE name = ?")) {
                ps.setString(1, teamName);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete team: " + teamName, e);
        }
    }

    public void deleteTeamAsync(String teamName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> deleteTeam(teamName));
    }

    // ==================== SAVE ALL ====================

    /**
     * Synchronously save all provided teams. Called during onDisable.
     */
    public void saveAllTeams(Collection<Team> teams) {
        for (Team team : teams) {
            saveTeam(team);
        }
        plugin.getLogger().info("Saved " + teams.size() + " teams to database.");
    }

    // ==================== ECHEST ====================

    /**
     * Load a team's echest contents from the database.
     * @return Base64-encoded string, or null if no data exists
     */
    public String loadEchest(String teamName) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT contents FROM team_echests WHERE team_name = ?")) {
            ps.setString(1, teamName.toLowerCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("contents");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load echest for team: " + teamName, e);
        }
        return null;
    }

    /**
     * Save a team's echest contents to the database.
     * @param teamName the team name (lowercase)
     * @param base64 Base64-encoded inventory contents
     */
    public void saveEchest(String teamName, String base64) {
        String sql = type == DatabaseType.MYSQL
                ? "INSERT INTO team_echests (team_name, contents) VALUES (?, ?) ON DUPLICATE KEY UPDATE contents=VALUES(contents)"
                : "INSERT OR REPLACE INTO team_echests (team_name, contents) VALUES (?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teamName.toLowerCase());
            ps.setString(2, base64);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save echest for team: " + teamName, e);
        }
    }

    /**
     * Delete a team's echest data from the database.
     */
    public void deleteEchest(String teamName) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM team_echests WHERE team_name = ?")) {
            ps.setString(1, teamName.toLowerCase());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete echest for team: " + teamName, e);
        }
    }

    // ==================== RENAME ====================

    /**
     * Atomically rename a team across all tables.
     * Uses a transaction to ensure all-or-nothing consistency.
     *
     * @param oldName current team name (lowercase)
     * @param newName new team name (lowercase)
     * @return true if successful
     */
    public boolean renameTeam(String oldName, String newName) {
        oldName = oldName.toLowerCase();
        newName = newName.toLowerCase();

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try {
                // 1. Update teams table (primary key)
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE teams SET name = ? WHERE name = ?")) {
                    ps.setString(1, newName);
                    ps.setString(2, oldName);
                    ps.executeUpdate();
                }

                // 2. Update team_members
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE team_members SET team_name = ? WHERE team_name = ?")) {
                    ps.setString(1, newName);
                    ps.setString(2, oldName);
                    ps.executeUpdate();
                }

                // 3. Update team_warps
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE team_warps SET team_name = ? WHERE team_name = ?")) {
                    ps.setString(1, newName);
                    ps.setString(2, oldName);
                    ps.executeUpdate();
                }

                // 4. Update team_relations (both sides)
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE team_relations SET team_name = ? WHERE team_name = ?")) {
                    ps.setString(1, newName);
                    ps.setString(2, oldName);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE team_relations SET target_team = ? WHERE target_team = ?")) {
                    ps.setString(1, newName);
                    ps.setString(2, oldName);
                    ps.executeUpdate();
                }

                // 5. Update team_echests
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE team_echests SET team_name = ? WHERE team_name = ?")) {
                    ps.setString(1, newName);
                    ps.setString(2, oldName);
                    ps.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                plugin.getLogger().log(Level.SEVERE, "Failed to rename team (rolled back): " + oldName + " -> " + newName, e);
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get connection for rename: " + oldName, e);
            return false;
        }
    }
}
