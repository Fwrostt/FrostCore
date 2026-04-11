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
import dev.frost.frostcore.utils.FrostLogger;

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

    /**
     * Initialize the connection pool and create tables.
     * Call this during onEnable, before loading data.
     */
    public void init() {

        try {
            Class<?> configuratorClass = Class.forName("org.apache.logging.log4j.core.config.Configurator");
            Class<?> levelClass = Class.forName("org.apache.logging.log4j.Level");
            Object warnLevel = levelClass.getDeclaredField("WARN").get(null);
            configuratorClass.getMethod("setLevel", String.class, levelClass).invoke(null, "com.zaxxer.hikari", warnLevel);
        } catch (Exception ignored) {}

        ConfigManager config = Main.getConfigManager();
        String typeStr = config.getString("database.type", "SQLITE").toUpperCase();

        try {
            type = DatabaseType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            FrostLogger.warn("Unknown database type '" + typeStr + "', falling back to SQLITE.");
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

            FrostLogger.info("Using MySQL database at " + host + ":" + port + "/" + database);
        } else {
            File dbFile = new File(plugin.getDataFolder(), "database.db");
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hikariConfig.setMaximumPoolSize(1);
            hikariConfig.setDriverClassName("org.sqlite.JDBC");

            hikariConfig.addDataSourceProperty("journal_mode", "WAL");
            hikariConfig.addDataSourceProperty("synchronous", "NORMAL");

            FrostLogger.info("Using SQLite database at " + dbFile.getAbsolutePath());
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
            FrostLogger.info("Database connection pool closed.");
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void createTables() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

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

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS server_warps (
                    name        VARCHAR(64) PRIMARY KEY,
                    world       VARCHAR(128) NOT NULL,
                    x           DOUBLE NOT NULL,
                    y           DOUBLE NOT NULL,
                    z           DOUBLE NOT NULL,
                    yaw         FLOAT NOT NULL,
                    pitch       FLOAT NOT NULL
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS server_spawn (
                    id          INT PRIMARY KEY DEFAULT 1,
                    world       VARCHAR(128) NOT NULL,
                    x           DOUBLE NOT NULL,
                    y           DOUBLE NOT NULL,
                    z           DOUBLE NOT NULL,
                    yaw         FLOAT NOT NULL,
                    pitch       FLOAT NOT NULL
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_cooldowns (
                    uuid        VARCHAR(36) NOT NULL,
                    cooldown_id VARCHAR(64) NOT NULL,
                    expires_at  BIGINT NOT NULL,
                    PRIMARY KEY (uuid, cooldown_id)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_homes (
                    uuid        VARCHAR(36) NOT NULL,
                    name        VARCHAR(64) NOT NULL,
                    world       VARCHAR(128) NOT NULL,
                    x           DOUBLE NOT NULL,
                    y           DOUBLE NOT NULL,
                    z           DOUBLE NOT NULL,
                    yaw         FLOAT NOT NULL,
                    pitch       FLOAT NOT NULL,
                    PRIMARY KEY (uuid, name)
                )
            """);

            FrostLogger.info("Database tables verified.");
        } catch (SQLException e) {
            FrostLogger.error("Failed to create database tables!", e);
        }
    }

    /**
     * Load all teams from the database. Called synchronously on startup.
     *
     * @return list of fully-populated Team objects
     */
    public List<Team> loadAllTeams() {
        Map<String, Team> teamMap = new LinkedHashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM teams");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("name");
                String tag = rs.getString("tag");
                String color = rs.getString("color");
                boolean pvp = rs.getBoolean("pvp_toggle");

                Team team = Team.createEmpty(name, tag, color, pvp);

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
            FrostLogger.error("Failed to load teams!", e);
        }

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
            FrostLogger.error("Failed to load team members!", e);
        }

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
            FrostLogger.error("Failed to load team warps!", e);
        }

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
            FrostLogger.error("Failed to load team relations!", e);
        }

        FrostLogger.info("Loaded " + teamMap.size() + " teams from database.");
        return new ArrayList<>(teamMap.values());
    }

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
            FrostLogger.error("Failed to save team base: " + team.getName(), e);
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
            FrostLogger.error("Failed to save members for team: " + team.getName(), e);
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
            FrostLogger.error("Failed to save warps for team: " + team.getName(), e);
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
            FrostLogger.error("Failed to save relations for team: " + team.getName(), e);
        }
    }

    public void saveRelationsAsync(Team team) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveRelations(team));
    }

    /**
     * Delete a team and all associated data from the database.
     */
    public void deleteTeam(String teamName) {
        try (Connection conn = getConnection()) {

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
            FrostLogger.error("Failed to delete team: " + teamName, e);
        }
    }

    public void deleteTeamAsync(String teamName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> deleteTeam(teamName));
    }

    /**
     * Synchronously save all provided teams. Called during onDisable.
     */
    public void saveAllTeams(Collection<Team> teams) {
        for (Team team : teams) {
            saveTeam(team);
        }
        FrostLogger.info("Saved " + teams.size() + " teams to database.");
    }

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
            FrostLogger.error("Failed to load echest for team: " + teamName, e);
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
            FrostLogger.error("Failed to save echest for team: " + teamName, e);
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
            FrostLogger.error("Failed to delete echest for team: " + teamName, e);
        }
    }

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

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE teams SET name = ? WHERE name = ?")) {
                    ps.setString(1, newName);
                    ps.setString(2, oldName);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE team_members SET team_name = ? WHERE team_name = ?")) {
                    ps.setString(1, newName);
                    ps.setString(2, oldName);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE team_warps SET team_name = ? WHERE team_name = ?")) {
                    ps.setString(1, newName);
                    ps.setString(2, oldName);
                    ps.executeUpdate();
                }

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
                FrostLogger.error("Failed to rename team (rolled back): " + oldName + " -> " + newName, e);
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to get connection for rename: " + oldName, e);
            return false;
        }
    }

    public Map<String, Location> loadServerWarps() {
        Map<String, Location> warps = new LinkedHashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM server_warps");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("name");
                World world = Bukkit.getWorld(rs.getString("world"));
                if (world == null) continue;
                Location loc = new Location(world,
                        rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                        rs.getFloat("yaw"), rs.getFloat("pitch"));
                warps.put(name, loc);
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to load server warps!", e);
        }
        FrostLogger.info("Loaded " + warps.size() + " server warps.");
        return warps;
    }

    public void saveServerWarp(String name, Location loc) {
        String sql = type == DatabaseType.MYSQL
                ? "INSERT INTO server_warps (name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE world=VALUES(world), x=VALUES(x), y=VALUES(y), z=VALUES(z), yaw=VALUES(yaw), pitch=VALUES(pitch)"
                : "INSERT OR REPLACE INTO server_warps (name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.toLowerCase());
            ps.setString(2, loc.getWorld().getName());
            ps.setDouble(3, loc.getX());
            ps.setDouble(4, loc.getY());
            ps.setDouble(5, loc.getZ());
            ps.setFloat(6, loc.getYaw());
            ps.setFloat(7, loc.getPitch());
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to save server warp: " + name, e);
        }
    }

    public void saveServerWarpAsync(String name, Location loc) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveServerWarp(name, loc));
    }

    public void deleteServerWarp(String name) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM server_warps WHERE name = ?")) {
            ps.setString(1, name.toLowerCase());
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to delete server warp: " + name, e);
        }
    }

    public void deleteServerWarpAsync(String name) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> deleteServerWarp(name));
    }

    public Location loadSpawn() {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM server_spawn WHERE id = 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                World world = Bukkit.getWorld(rs.getString("world"));
                if (world != null) {
                    return new Location(world,
                            rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                            rs.getFloat("yaw"), rs.getFloat("pitch"));
                }
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to load spawn!", e);
        }
        return null;
    }

    public void saveSpawn(Location loc) {
        String sql = type == DatabaseType.MYSQL
                ? "INSERT INTO server_spawn (id, world, x, y, z, yaw, pitch) VALUES (1, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE world=VALUES(world), x=VALUES(x), y=VALUES(y), z=VALUES(z), yaw=VALUES(yaw), pitch=VALUES(pitch)"
                : "INSERT OR REPLACE INTO server_spawn (id, world, x, y, z, yaw, pitch) VALUES (1, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, loc.getWorld().getName());
            ps.setDouble(2, loc.getX());
            ps.setDouble(3, loc.getY());
            ps.setDouble(4, loc.getZ());
            ps.setFloat(5, loc.getYaw());
            ps.setFloat(6, loc.getPitch());
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to save spawn!", e);
        }
    }

    public void saveSpawnAsync(Location loc) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveSpawn(loc));
    }

    /**
     * Load all stored (non-expired) cooldowns, keyed by player UUID.
     * Called synchronously on startup.
     */
    public Map<UUID, Map<String, Long>> loadAllCooldowns() {
        Map<UUID, Map<String, Long>> result = new java.util.LinkedHashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_cooldowns");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String id  = rs.getString("cooldown_id");
                long   exp = rs.getLong("expires_at");
                result.computeIfAbsent(uuid, k -> new java.util.LinkedHashMap<>()).put(id, exp);
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to load player cooldowns!", e);
        }
        return result;
    }

    /**
     * Upsert a single cooldown entry.
     */
    public void saveCooldown(UUID uuid, String cooldownId, long expiresAt) {
        String sql = type == DatabaseType.MYSQL
                ? "INSERT INTO player_cooldowns (uuid, cooldown_id, expires_at) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE expires_at=VALUES(expires_at)"
                : "INSERT OR REPLACE INTO player_cooldowns (uuid, cooldown_id, expires_at) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, cooldownId);
            ps.setLong(3, expiresAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to save cooldown for " + uuid + "/" + cooldownId, e);
        }
    }

    /**
     * Delete a single cooldown entry (expired or cleared).
     */
    public void deleteCooldown(UUID uuid, String cooldownId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM player_cooldowns WHERE uuid = ? AND cooldown_id = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, cooldownId);
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to delete cooldown for " + uuid + "/" + cooldownId, e);
        }
    }

    /**
     * Delete ALL cooldowns for a player.
     */
    public void deleteCooldowns(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM player_cooldowns WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to delete cooldowns for " + uuid, e);
        }
    }

    public Map<String, Location> loadPlayerHomes(UUID uuid) {
        Map<String, Location> homes = new LinkedHashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_homes WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    World world = Bukkit.getWorld(rs.getString("world"));
                    if (world != null) {
                        homes.put(name.toLowerCase(), new Location(world,
                                rs.getDouble("x"),
                                rs.getDouble("y"),
                                rs.getDouble("z"),
                                rs.getFloat("yaw"),
                                rs.getFloat("pitch")));
                    }
                }
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to load homes for player " + uuid, e);
        }
        return homes;
    }

    public void savePlayerHome(UUID uuid, String name, Location loc) {
        String sql = type == DatabaseType.MYSQL
                ? "INSERT INTO player_homes (uuid, name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE world=VALUES(world), x=VALUES(x), y=VALUES(y), z=VALUES(z), yaw=VALUES(yaw), pitch=VALUES(pitch)"
                : "INSERT OR REPLACE INTO player_homes (uuid, name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, loc.getWorld().getName());
            ps.setDouble(4, loc.getX());
            ps.setDouble(5, loc.getY());
            ps.setDouble(6, loc.getZ());
            ps.setFloat(7, loc.getYaw());
            ps.setFloat(8, loc.getPitch());
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to save home " + name + " for player " + uuid, e);
        }
    }

    public void savePlayerHomeAsync(UUID uuid, String name, Location loc) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> savePlayerHome(uuid, name, loc));
    }

    public void deletePlayerHome(UUID uuid, String name) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM player_homes WHERE uuid = ? AND name = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to delete home " + name + " for player " + uuid, e);
        }
    }

    public void deletePlayerHomeAsync(UUID uuid, String name) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> deletePlayerHome(uuid, name));
    }

    public void clearPlayerHomes(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM player_homes WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to clear homes for player " + uuid, e);
        }
    }

    public void deleteAllCooldowns(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM player_cooldowns WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to delete all cooldowns for " + uuid, e);
        }
    }
}


