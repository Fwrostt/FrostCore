package dev.frost.frostcore.moderation;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.database.DatabaseManager;
import dev.frost.frostcore.database.DatabaseType;
import dev.frost.frostcore.utils.FrostLogger;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class ModerationDatabase {

    private final DatabaseManager db;
    private final Main plugin;

    public ModerationDatabase(DatabaseManager db, Main plugin) {
        this.db = db;
        this.plugin = plugin;
    }

    

    public void createTables() {
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {

            
            String autoInc = db.getType() == DatabaseType.MYSQL ? "AUTO_INCREMENT" : "AUTOINCREMENT";

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS punishments (
                    id            INTEGER PRIMARY KEY %s,
                    type          VARCHAR(16)  NOT NULL,
                    uuid          VARCHAR(36),
                    target_name   VARCHAR(32),
                    ip            VARCHAR(45),
                    reason        TEXT,
                    staff_uuid    VARCHAR(36),
                    staff_name    VARCHAR(32),
                    duration      BIGINT       NOT NULL DEFAULT -1,
                    created_at    BIGINT       NOT NULL,
                    expires_at    BIGINT       NOT NULL DEFAULT -1,
                    active        BOOLEAN      NOT NULL DEFAULT 1,
                    removed_by    VARCHAR(36),
                    removed_by_name VARCHAR(32),
                    removed_at    BIGINT,
                    removed_reason TEXT,
                    server        VARCHAR(64),
                    silent        BOOLEAN      NOT NULL DEFAULT 0,
                    random_id     VARCHAR(8)
                )
            """.formatted(autoInc));

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_ips (
                    uuid          VARCHAR(36)  NOT NULL,
                    ip            VARCHAR(45)  NOT NULL,
                    last_seen     BIGINT       NOT NULL,
                    PRIMARY KEY (uuid, ip)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_names (
                    uuid          VARCHAR(36)  NOT NULL,
                    name          VARCHAR(32)  NOT NULL,
                    first_seen    BIGINT       NOT NULL,
                    last_seen     BIGINT       NOT NULL,
                    PRIMARY KEY (uuid, name)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS jail_locations (
                    name          VARCHAR(64)  PRIMARY KEY,
                    world         VARCHAR(128) NOT NULL,
                    x             DOUBLE       NOT NULL,
                    y             DOUBLE       NOT NULL,
                    z             DOUBLE       NOT NULL,
                    yaw           FLOAT        NOT NULL,
                    pitch         FLOAT        NOT NULL
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS jailed_players (
                    uuid          VARCHAR(36)  PRIMARY KEY,
                    jail_name     VARCHAR(64)  NOT NULL,
                    jailed_at     BIGINT       NOT NULL,
                    expires_at    BIGINT       NOT NULL DEFAULT -1,
                    reason        TEXT,
                    staff_uuid    VARCHAR(36)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS reports (
                    id            INTEGER PRIMARY KEY %s,
                    reporter_uuid VARCHAR(36)  NOT NULL,
                    reporter_name VARCHAR(32),
                    target_uuid   VARCHAR(36)  NOT NULL,
                    target_name   VARCHAR(32),
                    reason        TEXT         NOT NULL,
                    created_at    BIGINT       NOT NULL,
                    handled       BOOLEAN      NOT NULL DEFAULT 0,
                    handled_by    VARCHAR(36),
                    handled_by_name VARCHAR(32),
                    handled_at    BIGINT
                )
            """.formatted(autoInc));

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS allowed_players (
                    uuid          VARCHAR(36)  PRIMARY KEY,
                    added_by      VARCHAR(36),
                    added_at      BIGINT       NOT NULL
                )
            """);

            
            try {
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_punishments_uuid ON punishments(uuid)");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_punishments_ip ON punishments(ip)");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_punishments_type_active ON punishments(type, active)");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_punishments_staff ON punishments(staff_uuid)");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_punishments_random ON punishments(random_id)");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_player_ips_ip ON player_ips(ip)");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_reports_handled ON reports(handled)");
            } catch (SQLException ignored) {
                
            }

            FrostLogger.info("Moderation database tables verified.");
        } catch (SQLException e) {
            FrostLogger.error("Failed to create moderation tables!", e);
        }
    }

    

    
    public int insertPunishment(Punishment p) {
        String sql = """
            INSERT INTO punishments (type, uuid, target_name, ip, reason, staff_uuid, staff_name,
                duration, created_at, expires_at, active, server, silent, random_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, p.type().name());
            ps.setString(2, p.targetUuid() != null ? p.targetUuid().toString() : null);
            ps.setString(3, p.targetName());
            ps.setString(4, p.ip());
            ps.setString(5, p.reason());
            ps.setString(6, p.staffUuid() != null ? p.staffUuid().toString() : null);
            ps.setString(7, p.staffName());
            ps.setLong(8, p.duration());
            ps.setLong(9, p.createdAt());
            ps.setLong(10, p.expiresAt());
            ps.setBoolean(11, p.active());
            ps.setString(12, p.server());
            ps.setBoolean(13, p.silent());
            ps.setString(14, p.randomId());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to insert punishment", e);
        }
        return -1;
    }

    public void insertPunishmentAsync(Punishment p, java.util.function.Consumer<Integer> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int id = insertPunishment(p);
            if (callback != null) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(id));
            }
        });
    }

    
    public void deactivatePunishment(int id, UUID removedBy, String removedByName, String removedReason) {
        String sql = "UPDATE punishments SET active = 0, removed_by = ?, removed_by_name = ?, removed_at = ?, removed_reason = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, removedBy != null ? removedBy.toString() : null);
            ps.setString(2, removedByName);
            ps.setLong(3, System.currentTimeMillis());
            ps.setString(4, removedReason);
            ps.setInt(5, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to deactivate punishment #" + id, e);
        }
    }

    public void deactivatePunishmentAsync(int id, UUID removedBy, String removedByName, String removedReason) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> deactivatePunishment(id, removedBy, removedByName, removedReason));
    }

    
    public void deletePunishment(int id) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM punishments WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to delete punishment #" + id, e);
        }
    }

    
    public List<Punishment> loadActivePunishments(PunishmentType... types) {
        List<Punishment> result = new ArrayList<>();
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
        }

        String sql = "SELECT * FROM punishments WHERE active = 1 AND type IN (" + placeholders + ")";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < types.length; i++) {
                ps.setString(i + 1, types[i].name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(fromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to load active punishments", e);
        }
        return result;
    }

    
    public Punishment getPunishmentById(int id) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM punishments WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return fromResultSet(rs);
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to get punishment #" + id, e);
        }
        return null;
    }

    
    public Punishment getPunishmentByRandomId(String randomId) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM punishments WHERE random_id = ?")) {
            ps.setString(1, randomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return fromResultSet(rs);
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to get punishment by random ID: " + randomId, e);
        }
        return null;
    }

    
    public Punishment getActivePunishment(UUID uuid, String category) {
        List<String> typeNames = getTypeNamesForCategory(category);
        if (typeNames.isEmpty()) return null;

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < typeNames.size(); i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
        }

        String sql = "SELECT * FROM punishments WHERE uuid = ? AND active = 1 AND type IN (" + placeholders + ") ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            for (int i = 0; i < typeNames.size(); i++) {
                ps.setString(i + 2, typeNames.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return fromResultSet(rs);
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to get active punishment", e);
        }
        return null;
    }

    
    public Punishment getActiveIpPunishment(String ip, String category) {
        List<String> typeNames = getTypeNamesForCategory(category);
        if (typeNames.isEmpty()) return null;

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < typeNames.size(); i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
        }

        String sql = "SELECT * FROM punishments WHERE ip = ? AND active = 1 AND type IN (" + placeholders + ") ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ip);
            for (int i = 0; i < typeNames.size(); i++) {
                ps.setString(i + 2, typeNames.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return fromResultSet(rs);
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to get active IP punishment", e);
        }
        return null;
    }

    
    public List<Punishment> getPlayerHistory(UUID uuid, String typeFilter) {
        List<Punishment> result = new ArrayList<>();
        String sql;
        if (typeFilter != null && !typeFilter.isEmpty()) {
            List<String> typeNames = getTypeNamesForCategory(typeFilter);
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < typeNames.size(); i++) {
                if (i > 0) placeholders.append(",");
                placeholders.append("?");
            }
            sql = "SELECT * FROM punishments WHERE uuid = ? AND type IN (" + placeholders + ") ORDER BY created_at DESC";
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                for (int i = 0; i < typeNames.size(); i++) {
                    ps.setString(i + 2, typeNames.get(i));
                }
                fillHistoryList(result, ps);
            } catch (SQLException e) {
                FrostLogger.error("Failed to get player history", e);
            }
        } else {
            sql = "SELECT * FROM punishments WHERE uuid = ? ORDER BY created_at DESC";
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                fillHistoryList(result, ps);
            } catch (SQLException e) {
                FrostLogger.error("Failed to get player history", e);
            }
        }
        return result;
    }

    
    public List<Punishment> getStaffHistory(UUID staffUuid, String typeFilter) {
        List<Punishment> result = new ArrayList<>();
        String baseSql = "SELECT * FROM punishments WHERE staff_uuid = ?";
        if (typeFilter != null && !typeFilter.isEmpty()) {
            List<String> typeNames = getTypeNamesForCategory(typeFilter);
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < typeNames.size(); i++) {
                if (i > 0) placeholders.append(",");
                placeholders.append("?");
            }
            baseSql += " AND type IN (" + placeholders + ")";
            baseSql += " ORDER BY created_at DESC";
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(baseSql)) {
                ps.setString(1, staffUuid.toString());
                for (int i = 0; i < typeNames.size(); i++) {
                    ps.setString(i + 2, typeNames.get(i));
                }
                fillHistoryList(result, ps);
            } catch (SQLException e) {
                FrostLogger.error("Failed to get staff history", e);
            }
        } else {
            baseSql += " ORDER BY created_at DESC";
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(baseSql)) {
                ps.setString(1, staffUuid.toString());
                fillHistoryList(result, ps);
            } catch (SQLException e) {
                FrostLogger.error("Failed to get staff history", e);
            }
        }
        return result;
    }

    
    public List<Punishment> getActivePunishmentsPaginated(String category, int page, int pageSize) {
        List<Punishment> result = new ArrayList<>();
        List<String> typeNames = getTypeNamesForCategory(category);
        if (typeNames.isEmpty()) return result;

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < typeNames.size(); i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
        }

        String sql = "SELECT * FROM punishments WHERE active = 1 AND type IN (" + placeholders
                + ") ORDER BY created_at DESC LIMIT ? OFFSET ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < typeNames.size(); i++) {
                ps.setString(i + 1, typeNames.get(i));
            }
            ps.setInt(typeNames.size() + 1, pageSize);
            ps.setInt(typeNames.size() + 2, page * pageSize);
            fillHistoryList(result, ps);
        } catch (SQLException e) {
            FrostLogger.error("Failed to get paginated punishments", e);
        }
        return result;
    }

    
    public int countActivePunishments(String category) {
        List<String> typeNames = getTypeNamesForCategory(category);
        if (typeNames.isEmpty()) return 0;

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < typeNames.size(); i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
        }

        String sql = "SELECT COUNT(*) FROM punishments WHERE active = 1 AND type IN (" + placeholders + ")";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < typeNames.size(); i++) {
                ps.setString(i + 1, typeNames.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to count punishments", e);
        }
        return 0;
    }

    
    public int countPlayerWarnings(UUID uuid) {
        String sql = "SELECT COUNT(*) FROM punishments WHERE uuid = ? AND type = 'WARN'";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to count warnings", e);
        }
        return 0;
    }

    
    public List<Punishment> getActiveWarnings(UUID uuid) {
        List<Punishment> result = new ArrayList<>();
        String sql = "SELECT * FROM punishments WHERE uuid = ? AND type = 'WARN' AND active = 1 ORDER BY created_at DESC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            fillHistoryList(result, ps);
        } catch (SQLException e) {
            FrostLogger.error("Failed to get active warnings", e);
        }
        return result;
    }

    
    public int deactivateAllActive(UUID uuid, String category, UUID removedBy, String removedByName) {
        List<String> typeNames = getTypeNamesForCategory(category);
        if (typeNames.isEmpty()) return 0;

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < typeNames.size(); i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
        }

        String sql = "UPDATE punishments SET active = 0, removed_by = ?, removed_by_name = ?, removed_at = ? WHERE uuid = ? AND active = 1 AND type IN (" + placeholders + ")";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, removedBy != null ? removedBy.toString() : null);
            ps.setString(2, removedByName);
            ps.setLong(3, System.currentTimeMillis());
            ps.setString(4, uuid.toString());
            for (int i = 0; i < typeNames.size(); i++) {
                ps.setString(i + 5, typeNames.get(i));
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to deactivate all active punishments", e);
        }
        return 0;
    }

    
    public int staffRollback(UUID staffUuid, Long sinceMs) {
        String sql;
        if (sinceMs != null) {
            sql = "UPDATE punishments SET active = 0, removed_by = 'ROLLBACK', removed_at = ? WHERE staff_uuid = ? AND active = 1 AND created_at >= ?";
        } else {
            sql = "UPDATE punishments SET active = 0, removed_by = 'ROLLBACK', removed_at = ? WHERE staff_uuid = ? AND active = 1";
        }
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setString(2, staffUuid.toString());
            if (sinceMs != null) {
                ps.setLong(3, sinceMs);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to rollback staff punishments", e);
        }
        return 0;
    }

    
    public int pruneHistory(UUID uuid, Long sinceMs) {
        String sql;
        if (sinceMs != null) {
            sql = "DELETE FROM punishments WHERE uuid = ? AND active = 0 AND created_at < ?";
        } else {
            sql = "DELETE FROM punishments WHERE uuid = ? AND active = 0";
        }
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            if (sinceMs != null) {
                ps.setLong(2, sinceMs);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to prune history", e);
        }
        return 0;
    }

    

    public void recordPlayerIp(UUID uuid, String ip) {
        String sql = db.getType() == DatabaseType.MYSQL
                ? "INSERT INTO player_ips (uuid, ip, last_seen) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE last_seen=VALUES(last_seen)"
                : "INSERT OR REPLACE INTO player_ips (uuid, ip, last_seen) VALUES (?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, ip);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to record player IP", e);
        }
    }

    public void unlinkIps(UUID uuid) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM player_ips WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to unlink IPs for " + uuid, e);
        }
    }

    public void recordPlayerIpAsync(UUID uuid, String ip) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> recordPlayerIp(uuid, ip));
    }

    
    public Set<UUID> getUuidsByIp(String ip) {
        Set<UUID> result = new LinkedHashSet<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM player_ips WHERE ip = ?")) {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(UUID.fromString(rs.getString("uuid")));
                }
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to get UUIDs by IP", e);
        }
        return result;
    }

    
    public Set<String> getIpsByUuid(UUID uuid) {
        Set<String> result = new LinkedHashSet<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT ip FROM player_ips WHERE uuid = ? ORDER BY last_seen DESC")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString("ip"));
                }
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to get IPs by UUID", e);
        }
        return result;
    }

    
    public Set<UUID> getAlts(UUID uuid) {
        Set<UUID> alts = new LinkedHashSet<>();
        Set<String> ips = getIpsByUuid(uuid);
        for (String ip : ips) {
            alts.addAll(getUuidsByIp(ip));
        }
        alts.remove(uuid); 
        return alts;
    }

    

    public void recordPlayerName(UUID uuid, String name) {
        String sql = db.getType() == DatabaseType.MYSQL
                ? "INSERT INTO player_names (uuid, name, first_seen, last_seen) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE last_seen=VALUES(last_seen)"
                : "INSERT INTO player_names (uuid, name, first_seen, last_seen) VALUES (?, ?, ?, ?) ON CONFLICT(uuid, name) DO UPDATE SET last_seen = excluded.last_seen";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            long now = System.currentTimeMillis();
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setLong(3, now);
            ps.setLong(4, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to record player name", e);
        }
    }

    public void recordPlayerNameAsync(UUID uuid, String name) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> recordPlayerName(uuid, name));
    }

    public Map<String, long[]> getNameHistory(UUID uuid) {
        Map<String, long[]> result = new LinkedHashMap<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT name, first_seen, last_seen FROM player_names WHERE uuid = ? ORDER BY last_seen DESC")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("name"), new long[]{rs.getLong("first_seen"), rs.getLong("last_seen")});
                }
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to get name history", e);
        }
        return result;
    }

    public String getLastKnownName(UUID uuid) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT name FROM player_names WHERE uuid = ? ORDER BY last_seen DESC LIMIT 1")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("name");
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to get last known name", e);
        }
        return null;
    }

    

    public void saveJailLocation(JailLocation jail) {
        String sql = db.getType() == DatabaseType.MYSQL
                ? "INSERT INTO jail_locations (name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE world=VALUES(world), x=VALUES(x), y=VALUES(y), z=VALUES(z), yaw=VALUES(yaw), pitch=VALUES(pitch)"
                : "INSERT OR REPLACE INTO jail_locations (name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jail.name());
            ps.setString(2, jail.worldName());
            ps.setDouble(3, jail.x());
            ps.setDouble(4, jail.y());
            ps.setDouble(5, jail.z());
            ps.setFloat(6, jail.yaw());
            ps.setFloat(7, jail.pitch());
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to save jail location: " + jail.name(), e);
        }
    }

    public void deleteJailLocation(String name) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM jail_locations WHERE name = ?")) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to delete jail location: " + name, e);
        }
    }

    public Map<String, JailLocation> loadJailLocations() {
        Map<String, JailLocation> result = new LinkedHashMap<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM jail_locations");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("name");
                result.put(name, new JailLocation(name,
                        rs.getString("world"), rs.getDouble("x"), rs.getDouble("y"),
                        rs.getDouble("z"), rs.getFloat("yaw"), rs.getFloat("pitch")));
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to load jail locations", e);
        }
        return result;
    }

    

    public void jailPlayer(UUID uuid, String jailName, long expiresAt, String reason, UUID staffUuid) {
        String sql = db.getType() == DatabaseType.MYSQL
                ? "INSERT INTO jailed_players (uuid, jail_name, jailed_at, expires_at, reason, staff_uuid) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE jail_name=VALUES(jail_name), jailed_at=VALUES(jailed_at), expires_at=VALUES(expires_at), reason=VALUES(reason), staff_uuid=VALUES(staff_uuid)"
                : "INSERT OR REPLACE INTO jailed_players (uuid, jail_name, jailed_at, expires_at, reason, staff_uuid) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, jailName);
            ps.setLong(3, System.currentTimeMillis());
            ps.setLong(4, expiresAt);
            ps.setString(5, reason);
            ps.setString(6, staffUuid != null ? staffUuid.toString() : null);
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to jail player: " + uuid, e);
        }
    }

    public void unjailPlayer(UUID uuid) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM jailed_players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to unjail player: " + uuid, e);
        }
    }

    public record JailedEntry(String jailName, long jailedAt, long expiresAt, String reason, UUID staffUuid) {}

    public Map<UUID, JailedEntry> loadJailedPlayers() {
        Map<UUID, JailedEntry> result = new LinkedHashMap<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM jailed_players");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                result.put(uuid, new JailedEntry(
                        rs.getString("jail_name"), rs.getLong("jailed_at"),
                        rs.getLong("expires_at"), rs.getString("reason"),
                        rs.getString("staff_uuid") != null ? UUID.fromString(rs.getString("staff_uuid")) : null
                ));
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to load jailed players", e);
        }
        return result;
    }

    

    public int insertReport(Report r) {
        String sql = """
            INSERT INTO reports (reporter_uuid, reporter_name, target_uuid, target_name, reason, created_at, handled)
            VALUES (?, ?, ?, ?, ?, ?, 0)
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.reporterUuid().toString());
            ps.setString(2, r.reporterName());
            ps.setString(3, r.targetUuid().toString());
            ps.setString(4, r.targetName());
            ps.setString(5, r.reason());
            ps.setLong(6, r.createdAt());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to insert report", e);
        }
        return -1;
    }

    public void handleReport(int id, UUID handledBy, String handledByName) {
        String sql = "UPDATE reports SET handled = 1, handled_by = ?, handled_by_name = ?, handled_at = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, handledBy != null ? handledBy.toString() : null);
            ps.setString(2, handledByName);
            ps.setLong(3, System.currentTimeMillis());
            ps.setInt(4, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to handle report #" + id, e);
        }
    }

    public List<Report> getOpenReports(int page, int pageSize) {
        List<Report> result = new ArrayList<>();
        String sql = "SELECT * FROM reports WHERE handled = 0 ORDER BY created_at DESC LIMIT ? OFFSET ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, page * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(reportFromResultSet(rs));
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to get open reports", e);
        }
        return result;
    }

    public int countOpenReports() {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM reports WHERE handled = 0");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            FrostLogger.error("Failed to count open reports", e);
        }
        return 0;
    }

    

    public void addAllowedPlayer(UUID uuid, UUID addedBy) {
        String sql = db.getType() == DatabaseType.MYSQL
                ? "INSERT INTO allowed_players (uuid, added_by, added_at) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE added_by=VALUES(added_by), added_at=VALUES(added_at)"
                : "INSERT OR REPLACE INTO allowed_players (uuid, added_by, added_at) VALUES (?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, addedBy != null ? addedBy.toString() : null);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to add allowed player: " + uuid, e);
        }
    }

    public void removeAllowedPlayer(UUID uuid) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM allowed_players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to remove allowed player: " + uuid, e);
        }
    }

    public boolean isAllowedPlayer(UUID uuid) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM allowed_players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to check allowed player: " + uuid, e);
        }
        return false;
    }

    

    private Punishment fromResultSet(ResultSet rs) throws SQLException {
        String uuidStr = rs.getString("uuid");
        String staffStr = rs.getString("staff_uuid");
        String removedStr = rs.getString("removed_by");

        return new Punishment(
                rs.getInt("id"),
                PunishmentType.valueOf(rs.getString("type")),
                uuidStr != null ? UUID.fromString(uuidStr) : null,
                rs.getString("target_name"),
                rs.getString("ip"),
                rs.getString("reason"),
                staffStr != null ? UUID.fromString(staffStr) : null,
                rs.getString("staff_name"),
                rs.getLong("duration"),
                rs.getLong("created_at"),
                rs.getLong("expires_at"),
                rs.getBoolean("active"),
                removedStr != null && removedStr.length() == 36 ? UUID.fromString(removedStr) : null,
                rs.getString("removed_by_name"),
                rs.getObject("removed_at") != null ? rs.getLong("removed_at") : null,
                rs.getString("removed_reason"),
                rs.getString("server"),
                rs.getBoolean("silent"),
                rs.getString("random_id")
        );
    }

    private Report reportFromResultSet(ResultSet rs) throws SQLException {
        String handledByStr = rs.getString("handled_by");
        return new Report(
                rs.getInt("id"),
                UUID.fromString(rs.getString("reporter_uuid")),
                rs.getString("reporter_name"),
                UUID.fromString(rs.getString("target_uuid")),
                rs.getString("target_name"),
                rs.getString("reason"),
                rs.getLong("created_at"),
                rs.getBoolean("handled"),
                handledByStr != null ? UUID.fromString(handledByStr) : null,
                rs.getString("handled_by_name"),
                rs.getObject("handled_at") != null ? rs.getLong("handled_at") : null
        );
    }

    private void fillHistoryList(List<Punishment> list, PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(fromResultSet(rs));
            }
        }
    }

    private List<String> getTypeNamesForCategory(String category) {
        if (category == null) return List.of();
        return switch (category.toUpperCase()) {
            case "BAN" -> List.of("BAN", "TEMPBAN", "IPBAN");
            case "MUTE" -> List.of("MUTE", "TEMPMUTE", "IPMUTE");
            case "WARN" -> List.of("WARN");
            case "KICK" -> List.of("KICK");
            case "JAIL" -> List.of("JAIL");
            default -> List.of(category);
        };
    }
}
