package dev.frost.frostcore.mace;

import dev.frost.frostcore.Main;
import dev.frost.frostcore.database.DatabaseManager;
import dev.frost.frostcore.database.DatabaseType;
import dev.frost.frostcore.utils.FrostLogger;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.*;

public class MaceDatabase {

    private final DatabaseManager db;

    public MaceDatabase(DatabaseManager db) {
        this.db = db;
    }

    public void createTable() {
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            String autoInc = db.getType() == DatabaseType.MYSQL ? "AUTO_INCREMENT" : "";

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS mace_registry (
                    mace_id         VARCHAR(36)  PRIMARY KEY,
                    original_crafter VARCHAR(36),
                    crafter_name    VARCHAR(32),
                    crafted_at      BIGINT       NOT NULL,
                    current_holder  VARCHAR(36),
                    holder_name     VARCHAR(32),
                    last_seen_at    BIGINT       NOT NULL,
                    last_world      VARCHAR(128),
                    last_x          DOUBLE       NOT NULL DEFAULT 0,
                    last_y          DOUBLE       NOT NULL DEFAULT 0,
                    last_z          DOUBLE       NOT NULL DEFAULT 0,
                    enchantments    TEXT,
                    destroyed       BOOLEAN      NOT NULL DEFAULT 0
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS mace_pending_removals (
                    mace_id         VARCHAR(36)  PRIMARY KEY
                )
            """);

            try {
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_mace_holder ON mace_registry(current_holder)");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_mace_destroyed ON mace_registry(destroyed)");
            } catch (SQLException ignored) {}

            FrostLogger.info("Mace registry table verified.");
        } catch (SQLException e) {
            FrostLogger.error("Failed to create mace registry table!", e);
        }
    }

    public void insertMace(MaceEntry entry) {
        String sql = """
            INSERT INTO mace_registry (mace_id, original_crafter, crafter_name, crafted_at,
                current_holder, holder_name, last_seen_at, last_world, last_x, last_y, last_z,
                enchantments, destroyed)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setEntryParams(ps, entry);
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to insert mace: " + entry.maceId(), e);
        }
    }

    public void insertMaceAsync(MaceEntry entry) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> insertMace(entry));
    }

    public void updateMace(MaceEntry entry) {
        String sql = """
            UPDATE mace_registry SET current_holder = ?, holder_name = ?, last_seen_at = ?,
                last_world = ?, last_x = ?, last_y = ?, last_z = ?, enchantments = ?, destroyed = ?
            WHERE mace_id = ?
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entry.currentHolder() != null ? entry.currentHolder().toString() : null);
            ps.setString(2, entry.holderName());
            ps.setLong(3, entry.lastSeenAt());
            ps.setString(4, entry.lastWorld());
            ps.setDouble(5, entry.lastX());
            ps.setDouble(6, entry.lastY());
            ps.setDouble(7, entry.lastZ());
            ps.setString(8, entry.enchantments());
            ps.setBoolean(9, entry.destroyed());
            ps.setString(10, entry.maceId());
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to update mace: " + entry.maceId(), e);
        }
    }

    public void updateMaceAsync(MaceEntry entry) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> updateMace(entry));
    }

    public void softDeleteMace(String maceId) {
        String sql = "UPDATE mace_registry SET destroyed = 1 WHERE mace_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to soft-delete mace: " + maceId, e);
        }
    }

    public void softDeleteMaceAsync(String maceId) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> softDeleteMace(maceId));
    }

    public void hardDeleteMace(String maceId) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM mace_registry WHERE mace_id = ?")) {
            ps.setString(1, maceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to hard-delete mace: " + maceId, e);
        }
    }

    public void hardDeleteAllDestroyed() {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM mace_registry WHERE destroyed = 1")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to purge destroyed maces", e);
        }
    }

    public List<MaceEntry> loadAllActiveMaces() {
        List<MaceEntry> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM mace_registry WHERE destroyed = 0");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(fromResultSet(rs));
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to load active maces", e);
        }
        return result;
    }

    public List<MaceEntry> loadAllMaces() {
        List<MaceEntry> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM mace_registry ORDER BY crafted_at DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(fromResultSet(rs));
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to load all maces", e);
        }
        return result;
    }

    private MaceEntry fromResultSet(ResultSet rs) throws SQLException {
        String crafterStr = rs.getString("original_crafter");
        String holderStr = rs.getString("current_holder");
        return new MaceEntry(
                rs.getString("mace_id"),
                crafterStr != null ? UUID.fromString(crafterStr) : null,
                rs.getString("crafter_name"),
                rs.getLong("crafted_at"),
                holderStr != null ? UUID.fromString(holderStr) : null,
                rs.getString("holder_name"),
                rs.getLong("last_seen_at"),
                rs.getString("last_world"),
                rs.getDouble("last_x"),
                rs.getDouble("last_y"),
                rs.getDouble("last_z"),
                rs.getString("enchantments"),
                rs.getBoolean("destroyed")
        );
    }

    private void setEntryParams(PreparedStatement ps, MaceEntry e) throws SQLException {
        ps.setString(1, e.maceId());
        ps.setString(2, e.originalCrafter() != null ? e.originalCrafter().toString() : null);
        ps.setString(3, e.crafterName());
        ps.setLong(4, e.craftedAt());
        ps.setString(5, e.currentHolder() != null ? e.currentHolder().toString() : null);
        ps.setString(6, e.holderName());
        ps.setLong(7, e.lastSeenAt());
        ps.setString(8, e.lastWorld());
        ps.setDouble(9, e.lastX());
        ps.setDouble(10, e.lastY());
        ps.setDouble(11, e.lastZ());
        ps.setString(12, e.enchantments());
        ps.setBoolean(13, e.destroyed());
    }

    public void insertPendingRemoval(String maceId) {
        String sql = "INSERT OR IGNORE INTO mace_pending_removals (mace_id) VALUES (?)";
        if (db.getType() == DatabaseType.MYSQL) {
            sql = "INSERT IGNORE INTO mace_pending_removals (mace_id) VALUES (?)";
        }
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to insert pending removal: " + maceId, e);
        }
    }

    public void insertPendingRemovalAsync(String maceId) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> insertPendingRemoval(maceId));
    }

    public Set<String> loadPendingRemovals() {
        Set<String> result = new java.util.LinkedHashSet<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT mace_id FROM mace_pending_removals");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(rs.getString("mace_id"));
            }
        } catch (SQLException e) {
            FrostLogger.error("Failed to load pending removals", e);
        }
        return result;
    }

    public void removePendingRemoval(String maceId) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM mace_pending_removals WHERE mace_id = ?")) {
            ps.setString(1, maceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            FrostLogger.error("Failed to remove pending removal: " + maceId, e);
        }
    }

    public void removePendingRemovalAsync(String maceId) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> removePendingRemoval(maceId));
    }
}
